package com.clarix.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.clarix.dto.AppointmentForm;
import com.clarix.dto.MemoForm;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.CareRequestService;
import com.clarix.service.CurrentUser;
import com.clarix.service.ReceptionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/reception")
public class ReceptionController {

    private final CurrentUser current;
    private final ReceptionService receptionSvc;
    private final CareRequestService careRequestSvc;

    public ReceptionController(CurrentUser current, ReceptionService receptionSvc,
                               CareRequestService careRequestSvc) {
        this.current = current;
        this.receptionSvc = receptionSvc;
        this.careRequestSvc = careRequestSvc;
    }

    @GetMapping({"", "/"})
    public String index(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) {
            model.addAttribute("me", me);
            return "reception/no-hospital";
        }
        model.addAttribute("me", me);
        model.addAttribute("rows", receptionSvc.patientsForHospital(me.getHospital().getId()));
        model.addAttribute("pendingCareRequests", careRequestSvc.pendingForHospital(me.getHospital().getId()));
        return "reception/index";
    }

    @PostMapping("/patient/{id}/memo")
    public String updateMemo(@PathVariable UUID id,
                             @Valid @ModelAttribute MemoForm form,
                             BindingResult errors,
                             HttpSession session,
                             RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        if (errors.hasErrors()) return ValidationFeedback.redirect("/reception/", errors, redirect);
        receptionSvc.updateMemo(me.getHospital().getId(), id, form.getMemo());
        return "redirect:/reception/";
    }

    @PostMapping("/patient/{id}/book")
    public String toggleBook(@PathVariable UUID id,
                             @Valid @ModelAttribute AppointmentForm form,
                             BindingResult errors,
                             HttpSession session,
                             RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        if (errors.hasErrors()) return ValidationFeedback.redirect("/reception/", errors, redirect);
        receptionSvc.toggleBooked(me.getHospital().getId(), id,
            form.isBooked(), form.getAppointmentAt());
        return "redirect:/reception/";
    }

    @PostMapping("/request/{id}/scheduled")
    public String markRequestScheduled(@PathVariable UUID id,
                                       @Valid @ModelAttribute AppointmentForm form,
                                       BindingResult errors,
                                       HttpSession session,
                                       RedirectAttributes redirect) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        if (errors.hasErrors() || form.getAppointmentAt() == null) {
            redirect.addFlashAttribute("formError", "예약 시간을 입력하세요");
            return "redirect:/reception/";
        }
        careRequestSvc.markScheduled(me.getHospital().getId(), id, form.getAppointmentAt());
        return "redirect:/reception/";
    }

    /** 접수처용 활성 처방 목록. 환자별로 묶어 표시하고 처방전 출력 링크를 노출합니다. */
    @GetMapping("/prescriptions")
    public String prescriptions(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        model.addAttribute("me", me);
        model.addAttribute("prescriptions",
            receptionSvc.activePrescriptionsForHospital(me.getHospital().getId()));
        return "reception/prescriptions";
    }

    /** 접수처에서 환자별 처방전을 인쇄/PDF 저장하는 화면. 같은 병원 환자만 접근 가능. */
    @GetMapping("/patient/{id}/prescription-print")
    public String prescriptionPrint(@PathVariable UUID id, HttpSession session, Model model) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        User patient = receptionSvc.requireSameHospitalPatient(me.getHospital().getId(), id);
        model.addAttribute("me", me);
        model.addAttribute("patient", patient);
        model.addAttribute("rx", receptionSvc.activePrescriptionsForPatient(id));
        model.addAttribute("issuedAt", java.time.LocalDate.now());
        return "doctor/prescription-print";
    }
}
