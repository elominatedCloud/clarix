package com.clarix.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clarix.dto.SignupForm;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.AuthService;
import com.clarix.service.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

    private final AuthService auth;
    private final CurrentUser current;

    public HomeController(AuthService auth, CurrentUser current) {
        this.auth = auth;
        this.current = current;
    }

    @GetMapping("/")
    public String landing(HttpSession session) {
        // 이미 로그인한 사용자가 랜딩/로그인으로 돌아오면 역할별 홈으로 보내 UX를 단순화합니다.
        if (current.isLoggedIn(session)) {
            User u = current.require(session);
            return switch (u.getRole()) {
                case DOCTOR -> "redirect:/doctor/";
                case PATIENT -> "redirect:/patient/";
                case RECEPTIONIST -> "redirect:/reception/";
                case NURSE, TECHNICIAN -> "redirect:/staff/";
                case ADMIN -> "redirect:/admin/";
            };
        }
        return "landing";
    }

    /**
     * 로그인 폼만 렌더. 실제 인증은 Spring Security의 UsernamePasswordAuthenticationFilter가
     * POST /auth/login을 가로채서 처리하므로 여기에 별도 핸들러 없음.
     */
    @GetMapping("/auth/login")
    public String loginForm(@RequestParam(required = false) String role,
                             @RequestParam(required = false) Integer error,
                             Model model) {
        String normalizedRole = "doctor".equalsIgnoreCase(role) ? "doctor" : "patient";
        model.addAttribute("role", normalizedRole);
        if (error != null) model.addAttribute("error", "이메일 또는 비밀번호가 일치하지 않습니다");
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupForm(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("role", "doctor".equalsIgnoreCase(role) ? "doctor" : "patient");
        return "auth/signup";
    }

    @PostMapping("/auth/signup")
    public String signup(@Valid @ModelAttribute SignupForm form,
                         BindingResult errors,
                         HttpServletRequest request, Model model) {
        // @Valid 결과는 BindingResult에 담깁니다. errors 매개변수는 검증 대상 바로 뒤에 와야 합니다.
        if (errors.hasErrors()) {
            return signupError(form, model, firstError(errors));
        }
        try {
            Role r = "doctor".equalsIgnoreCase(form.getRole()) ? Role.DOCTOR : Role.PATIENT;
            User u = auth.signup(form.getEmail(), form.getPassword(), form.getName(), r);
            // 가입 직후 자동 로그인 (Spring Security context에 직접 주입)
            Role savedRole = u.getRole();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, java.util.List.of(new SimpleGrantedAuthority(savedRole.name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.getSession(true).setAttribute(
                "SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return switch (savedRole) {
                case DOCTOR -> "redirect:/doctor/";
                case PATIENT -> "redirect:/patient/welcome";
                case RECEPTIONIST -> "redirect:/reception/";
                case NURSE, TECHNICIAN -> "redirect:/staff/";
                case ADMIN -> "redirect:/admin/";
            };
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return signupError(form, model, e.getReason());
        }
    }

    private String signupError(SignupForm form, Model model, String message) {
        model.addAttribute("error", message);
        model.addAttribute("role", "doctor".equalsIgnoreCase(form.getRole()) ? "doctor" : "patient");
        model.addAttribute("name", form.getName());
        model.addAttribute("email", form.getEmail());
        return "auth/signup";
    }

    private String firstError(BindingResult errors) {
        var field = errors.getFieldError();
        return field != null ? field.getDefaultMessage() : "입력값을 확인하세요";
    }
}
