package com.clarix.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clarix.dto.AdminUserForm;
import com.clarix.dto.HospitalForm;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.AdminService;
import com.clarix.service.CurrentUser;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CurrentUser current;
    private final AdminService adminSvc;

    public AdminController(CurrentUser current, AdminService adminSvc) {
        this.current = current;
        this.adminSvc = adminSvc;
    }

    @GetMapping({"", "/"})
    public String index(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.ADMIN);
        var rows = adminSvc.hospitalRows();
        int totalH      = rows.size();
        long partnered  = rows.stream().filter(AdminService.HospitalRow::partnered).count();
        int totalDoc    = rows.stream().mapToInt(AdminService.HospitalRow::doctorCount).sum();
        int totalStaff  = rows.stream().mapToInt(AdminService.HospitalRow::staffCount).sum();
        int totalPatient = rows.stream().mapToInt(AdminService.HospitalRow::patientCount).sum();
        model.addAttribute("me", me);
        model.addAttribute("rows", rows);
        model.addAttribute("totalH", totalH);
        model.addAttribute("partnered", partnered);
        model.addAttribute("totalDoc", totalDoc);
        model.addAttribute("totalStaff", totalStaff);
        model.addAttribute("totalPatient", totalPatient);
        return "admin/index";
    }

    @PostMapping("/hospital")
    public String createHospital(@ModelAttribute HospitalForm form,
                                 HttpSession session) {
        current.requireRole(session, Role.ADMIN);
        adminSvc.createHospital(form.getName(), form.getAddress(), form.getSpecialty(), form.isPartnered());
        return "redirect:/admin/";
    }

    @PostMapping("/hospital/{id}/toggle")
    public String togglePartnered(@PathVariable UUID id,
                                  @RequestParam boolean partnered,
                                  HttpSession session) {
        current.requireRole(session, Role.ADMIN);
        adminSvc.togglePartnered(id, partnered);
        return "redirect:/admin/";
    }

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        User me = current.requireRole(session, Role.ADMIN);
        model.addAttribute("me", me);
        model.addAttribute("users", adminSvc.allUsers());
        model.addAttribute("hospitals", adminSvc.allHospitals());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable UUID id,
                             @ModelAttribute AdminUserForm form,
                             HttpSession session) {
        current.requireRole(session, Role.ADMIN);
        UUID hid = (form.getHospitalId() == null || form.getHospitalId().isBlank())
            ? null : UUID.fromString(form.getHospitalId());
        adminSvc.updateUser(id, Role.valueOf(form.getRole()), hid);
        return "redirect:/admin/users";
    }
}
