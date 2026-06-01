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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
                             @RequestParam(required = false) Integer switched,
                             Model model) {
        String normalizedRole = "doctor".equalsIgnoreCase(role) ? "doctor" : "patient";
        model.addAttribute("role", normalizedRole);
        if (error != null) model.addAttribute("error", "이메일 또는 비밀번호가 일치하지 않습니다");
        if (switched != null) model.addAttribute("notice", "이전 계정에서 로그아웃했습니다. 사용할 계정으로 다시 로그인하세요.");
        return "auth/login";
    }

    @GetMapping("/auth/access-denied")
    public String accessDenied(@RequestParam(required = false) String targetRole,
                               HttpSession session,
                               Model model) {
        User me = current.isLoggedIn(session) ? current.require(session) : null;
        String currentRole = me == null ? "guest" : roleKey(me.getRole());
        String target = normalizeTargetRole(targetRole);

        model.addAttribute("currentName", me == null ? "비로그인 사용자" : me.getName());
        model.addAttribute("currentRole", currentRole);
        model.addAttribute("currentRoleLabel", roleLabel(currentRole));
        model.addAttribute("currentHome", me == null ? "/" : roleHome(me.getRole()));
        model.addAttribute("targetRole", target);
        model.addAttribute("targetRoleLabel", roleLabel(target));
        model.addAttribute("targetLoginRole", loginRole(target));
        return "auth/access-denied";
    }

    @PostMapping("/auth/switch-role")
    public String switchRole(@RequestParam(required = false) String targetRole,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/auth/login?role=" + loginRole(normalizeTargetRole(targetRole)) + "&switched=1";
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

    private String normalizeTargetRole(String role) {
        if (role == null) return "doctor";
        return switch (role.toLowerCase()) {
            case "patient", "doctor", "reception", "staff", "admin" -> role.toLowerCase();
            default -> "doctor";
        };
    }

    private String loginRole(String role) {
        // 로그인 화면은 환자/의사 톤만 구분합니다. 직원/관리자는 의사용 톤으로 로그인합니다.
        return "patient".equals(role) ? "patient" : "doctor";
    }

    private String roleKey(Role role) {
        return switch (role) {
            case PATIENT -> "patient";
            case DOCTOR -> "doctor";
            case RECEPTIONIST -> "reception";
            case NURSE, TECHNICIAN -> "staff";
            case ADMIN -> "admin";
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "patient" -> "환자";
            case "doctor" -> "의사";
            case "reception" -> "접수";
            case "staff" -> "의료진";
            case "admin" -> "관리자";
            default -> "방문자";
        };
    }

    private String roleHome(Role role) {
        return switch (role) {
            case PATIENT -> "/patient/";
            case DOCTOR -> "/doctor/";
            case RECEPTIONIST -> "/reception/";
            case NURSE, TECHNICIAN -> "/staff/";
            case ADMIN -> "/admin/";
        };
    }
}
