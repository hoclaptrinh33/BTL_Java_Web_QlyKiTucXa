package com.ktx.web.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Building;
import com.ktx.domain.RoomImage;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomImageRepository;
import com.ktx.service.MediaGalleryService;

@Controller
@RequestMapping("/admin/room-types/images")
public class AdminRoomImageController {

    private final RoomImageRepository roomImageRepository;
    private final BuildingRepository buildingRepository;
    private final MediaGalleryService mediaGalleryService;

    public AdminRoomImageController(RoomImageRepository roomImageRepository,
                                    BuildingRepository buildingRepository,
                                    MediaGalleryService mediaGalleryService) {
        this.roomImageRepository = roomImageRepository;
        this.buildingRepository = buildingRepository;
        this.mediaGalleryService = mediaGalleryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewRoomTypeImages(@RequestParam(value = "type", required = false) RoomType selectedType,
                                     Model model) {
        if (selectedType == null) {
            selectedType = RoomType.STANDARD_4;
        }

        List<RoomImage> images = roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(selectedType);
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("selectedType", selectedType);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("images", images);
        model.addAttribute("buildings", buildings);
        model.addAttribute("pageTitle", "Quản lý album ảnh loại phòng");
        return "admin/rooms/type_images";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("roomType") RoomType roomType,
                              @RequestParam(value = "buildingId", required = false) Long buildingId,
                              @RequestParam("files") List<MultipartFile> files,
                              @RequestParam(value = "caption", required = false) String caption,
                              @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
                              RedirectAttributes redirectAttributes) {
        try {
            int count = 0;
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    mediaGalleryService.addRoomTypeImage(roomType, buildingId, file, caption, isPrimary && count == 0);
                    count++;
                }
            }
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tải lên thành công " + count + " ảnh cho loại phòng.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một file ảnh.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải ảnh: " + e.getMessage());
        }
        return "redirect:/admin/room-types/images?type=" + roomType.name();
    }

    @PostMapping("/{imageId}/primary")
    public String setPrimary(@PathVariable("imageId") Long imageId,
                             @RequestParam("roomType") RoomType roomType,
                             RedirectAttributes redirectAttributes) {
        try {
            mediaGalleryService.setRoomTypePrimaryImage(roomType, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm ảnh đại diện cho loại phòng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/room-types/images?type=" + roomType.name();
    }

    @PostMapping("/{imageId}/delete")
    public String deleteImage(@PathVariable("imageId") Long imageId,
                              @RequestParam("roomType") RoomType roomType,
                              RedirectAttributes redirectAttributes) {
        try {
            mediaGalleryService.deleteRoomImage(imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/room-types/images?type=" + roomType.name();
    }
}
