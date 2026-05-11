package com.clarix.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.CurrentUser;
import com.clarix.service.DoctorService;
import com.clarix.service.NoteService;
import com.clarix.service.StaffService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final CurrentUser current;
    private final StaffService staffSvc;
    private final DoctorService doctorSvc;
    private final NoteService noteSvc;

    public StaffController(CurrentUser current, StaffService staffSvc,
                           DoctorService doctorSvc, NoteService noteSvc) {
        this.current = current;
        this.staffSvc = staffSvc;
        this.doctorSvc = doctorSvc;
        this.noteSvc = noteSvc;
    }

    private User requireStaff(HttpSession session) {
        User me = current.require(session);
        if (me.getRole() != Role.NURSE && me.getRole() != Role.TECHNICIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role mismatch");
        }
        return me;
    }

    @GetMapping({"", "/"})
    public String index(HttpSession session, Model model) {
        User me = requireStaff(session);
        if (me.getHospital() == null) {
            model.addAttribute("me", me);
            return "staff/no-hospital";
        }
        model.addAttribute("me", me);
        model.addAttribute("patients", staffSvc.patientsForHospital(me.getHospital().getId()));
        return "staff/index";
    }

    @GetMapping("/patient/{id}")
    public String patientCharting(@PathVariable UUID id, HttpSession session, Model model) {
        User me = requireStaff(session);
        User patient = staffSvc.requirePatientInSameHospital(me, id);
        model.addAttribute("me", me);
        model.addAttribute("patient", patient);
        // doctorSvc.patientHeader gives self-report-friendly bits (last emotion/journal etc.)
        model.addAttribute("header", doctorSvc.patientHeader(id));
        // self-report timeline (mood/med/phq9; soap excluded in template)
        model.addAttribute("timeline", doctorSvc.timelineEvents(id, 20));
        // notes for read; staff can append new ones
        model.addAttribute("notes", staffSvc.notesForPatient(id));
        return "staff/patient";
    }

    @GetMapping("/note/{id}/edit")
    public String editNote(@PathVariable UUID id, HttpSession session,
                           org.springframework.ui.Model model) {
        User me = requireStaff(session);
        var note = noteSvc.requireOwn(me, id);
        model.addAttribute("me", me);
        model.addAttribute("note", note);
        model.addAttribute("backUrl", "/staff/patient/" + note.getPatient().getId());
        return "note-edit";
    }

    @PostMapping("/note/{id}/edit")
    public String saveNoteEdit(@PathVariable UUID id,
                               @RequestParam(required = false) String subjective,
                               @RequestParam(required = false) String objective,
                               @RequestParam(required = false) String assessment,
                               @RequestParam(required = false) String plan,
                               HttpSession session) {
        User me = requireStaff(session);
        var n = noteSvc.update(me, id, subjective, objective, assessment, plan);
        return "redirect:/staff/patient/" + n.getPatient().getId();
    }

    @PostMapping("/patient/{id}/soap")
    public String addSoap(@PathVariable UUID id,
                          @RequestParam(required = false) String subjective,
                          @RequestParam(required = false) String objective,
                          @RequestParam(required = false) String assessment,
                          @RequestParam(required = false) String plan,
                          HttpSession session) {
        User me = requireStaff(session);
        staffSvc.createNote(me, id, subjective, objective, assessment, plan);
        return "redirect:/staff/patient/" + id;
    }
}
