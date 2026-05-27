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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.clarix.dto.MemoForm;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.CurrentUser;
import com.clarix.service.ReceptionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/reception")
public class ReceptionController {

    private final CurrentUser current;
    private final ReceptionService receptionSvc;

    public ReceptionController(CurrentUser current, ReceptionService receptionSvc) {
        this.current = current;
        this.receptionSvc = receptionSvc;
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
                             @RequestParam boolean booked,
                             HttpSession session) {
        User me = current.requireRole(session, Role.RECEPTIONIST);
        if (me.getHospital() == null) return "redirect:/reception/";
        receptionSvc.toggleBooked(me.getHospital().getId(), id, booked);
        return "redirect:/reception/";
    }
}
