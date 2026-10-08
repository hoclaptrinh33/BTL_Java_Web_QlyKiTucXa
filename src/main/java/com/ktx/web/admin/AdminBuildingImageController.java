package com.ktx.web.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Building;
import com.ktx.domain.BuildingImage;
import com.ktx.repository.BuildingImageRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.service.MediaGalleryService;

@Controller
@RequestMapping("/admin/buildings/{id}/images")
public class AdminBuildingImageController {

    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final MediaGalleryService mediaGalleryService;

    public AdminBuildingImageController(BuildingRepository buildingRepository,
                                        BuildingImageRepository buildingImageRepository,
                                        MediaGalleryService mediaGalleryService) {
        this.buildingRepository = buildingRepository;
        this.buildingImageRepository = buildingImageRepository;
        this.mediaGalleryService = mediaGalleryService;
    }

    @GetMapping
    public String viewGallery(@PathVariable("id") Long buildingId, Model model, RedirectAttributes redirectAttributes) {
        Building building = buildingRepository.findById(buildingId).orElse(null);
        if (building == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tòa nhà.");
            return "redirect:/admin/buildings";
        }

        List<BuildingImage> images = buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(buildingId);
        model.addAttribute("building", building);
        model.addAttribute("images", images);
        model.addAttribute("pageTitle", "Quản lý album ảnh — Tòa " + building.getCode());
        return "admin/buildings/images";
    }

    @PostMapping("/upload")
    public String uploadImages(@PathVariable("id") Long buildingId,
                               @RequestParam("files") List<MultipartFile> files,
                               @RequestParam(value = "caption", required = false) String caption,
                               @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
                               RedirectAttributes redirectAttributes) {
        try {
            int count = 0;
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    mediaGalleryService.addBuildingImage(buildingId, file, caption, isPrimary && count == 0);
                    count++;
                }
            }
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tải lên thành công " + count + " hình ảnh.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một file ảnh hợp lệ.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải ảnh: " + e.getMessage());
        }
        return "redirect:/admin/buildings/" + buildingId + "/images";
    }

    @PostMapping("/{imageId}/primary")
    public String setPrimary(@PathVariable("id") Long buildingId,
                             @PathVariable("imageId") Long imageId,
                             RedirectAttributes redirectAttributes) {
        try {
            mediaGalleryService.setBuildingPrimaryImage(buildingId, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm ảnh đại diện cho tòa nhà.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/buildings/" + buildingId + "/images";
    }

    @PostMapping("/{imageId}/delete")
    public String deleteImage(@PathVariable("id") Long buildingId,
                              @PathVariable("imageId") Long imageId,
                              RedirectAttributes redirectAttributes) {
        try {
            mediaGalleryService.deleteBuildingImage(imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/buildings/" + buildingId + "/images";
    }
}
