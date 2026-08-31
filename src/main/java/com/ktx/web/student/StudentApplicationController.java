package com.ktx.web.student;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.RegistrationPeriodForm; // just in case
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.RoomApplicationService;

@Controller
@RequestMapping("/student/applications")
public class StudentApplicationController {

    private final RoomApplicationService roomApplicationService;
    private final StudentRepository studentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;

    @Autowired
    public StudentApplicationController(RoomApplicationService roomApplicationService,
                                        StudentRepository studentRepository,
                                        RegistrationPeriodRepository periodRepository,
                                        BuildingRepository buildingRepository,
                                        ContractRepository contractRepository) {
        this.roomApplicationService = roomApplicationService;
        this.studentRepository = studentRepository;
        this.periodRepository = periodRepository;
        this.buildingRepository = buildingRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<RoomApplication> applications = roomApplicationService.getStudentApplications(student.getId());
        List<RegistrationPeriod> openPeriods = periodRepository.findAllWithCreator().stream()
                .filter(p -> p.getStatus() == PeriodStatus.OPEN)
                .toList();

        model.addAttribute("applications", applications);
        model.addAttribute("openPeriods", openPeriods);
        model.addAttribute("student", student);
        
        // Ràng buộc nhanh hiển thị ở UI
        boolean hasOccupying = contractRepository.existsByStudentIdAndStatusIn(student.getId(), OccupyingStatuses.OCCUPYING);
        model.addAttribute("hasOccupyingContract", hasOccupying);
        model.addAttribute("isBlocked", Boolean.TRUE.equals(student.getBlockedFromHousing()));
        model.addAttribute("isConductZero", student.getConductScore() == null || student.getConductScore() <= 0);

        page(model, "Đơn đăng ký chỗ ở", "Nộp và theo dõi nguyện vọng phòng ở ký túc xá");
        return "student/applications/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam("periodId") Long periodId, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);

        // Pre-checks
        if (Boolean.TRUE.equals(student.getBlockedFromHousing())) {
            redirectAttributes.addFlashAttribute("errorMessage", RoomApplicationService.STUDENT_BLOCKED);
            return "redirect:/student/applications";
        }
        if (student.getConductScore() == null || student.getConductScore() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", RoomApplicationService.CONDUCT_SCORE_ZERO);
            return "redirect:/student/applications";
        }
        if (contractRepository.existsByStudentIdAndStatusIn(student.getId(), OccupyingStatuses.OCCUPYING)) {
            redirectAttributes.addFlashAttribute("errorMessage", RoomApplicationService.ACTIVE_CONTRACT_EXISTS);
            return "redirect:/student/applications";
        }
        if (roomApplicationRepositoryExists(periodId, student.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", RoomApplicationService.ALREADY_SUBMITTED);
            return "redirect:/student/applications";
        }

        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException(RoomApplicationService.PERIOD_NOT_FOUND));

        if (period.getStatus() != PeriodStatus.OPEN) {
            redirectAttributes.addFlashAttribute("errorMessage", RoomApplicationService.PERIOD_NOT_OPEN);
            return "redirect:/student/applications";
        }

        // Lọc tòa nhà theo giới tính của sinh viên
        BuildingGenderPolicy genderPolicy = student.getGender() == Gender.MALE ? BuildingGenderPolicy.MALE : BuildingGenderPolicy.FEMALE;
        List<Building> buildings = buildingRepository.findAll().stream()
                .filter(b -> Boolean.TRUE.equals(b.getActive()) && b.getGenderPolicy() == genderPolicy)
                .toList();

        model.addAttribute("period", period);
        model.addAttribute("buildings", buildings);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("student", student);
        
        RoomApplication app = new RoomApplication();
        app.setPeriod(period);
        app.setStudent(student);
        model.addAttribute("form", app);

        page(model, "Nộp đơn nguyện vọng", "Chọn tòa nhà và loại phòng mong muốn");
        return "student/applications/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam("periodId") Long periodId,
                         @RequestParam(value = "preferredBuildingId", required = false) Long preferredBuildingId,
                         @RequestParam(value = "preferredRoomType", required = false) RoomType preferredRoomType,
                         @RequestParam(value = "note", required = false) String note,
                         Principal principal, RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        try {
            RoomApplication created = roomApplicationService.submitApplication(
                    student.getId(), periodId, preferredBuildingId, preferredRoomType, note);
            redirectAttributes.addFlashAttribute("successMessage", "Nộp đơn nguyện vọng thành công cho đợt: " + created.getPeriod().getName());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/applications";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        try {
            roomApplicationService.withdrawApplication(id, student.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã rút đơn nguyện vọng thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/applications";
    }

    private Student getStudent(Principal principal) {
        return studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));
    }

    private boolean roomApplicationRepositoryExists(Long periodId, Long studentId) {
        return roomApplicationService.getStudentApplications(studentId).stream()
                .anyMatch(a -> a.getPeriod().getId().equals(periodId));
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "applications");
    }
}
