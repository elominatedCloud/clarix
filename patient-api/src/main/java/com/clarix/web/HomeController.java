package com.clarix.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.service.AuthService;
import com.clarix.service.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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
        if (current.isLoggedIn(session)) {
            User u = current.require(session);
            return u.getRole() == Role.DOCTOR ? "redirect:/doctor/" : "redirect:/patient/";
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
        model.addAttribute("role", role);
        if (error != null) model.addAttribute("error", "이메일 또는 비밀번호가 일치하지 않습니다");
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupForm(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("role", "doctor".equalsIgnoreCase(role) ? "doctor" : "patient");
        return "auth/signup";
    }

    @PostMapping("/auth/signup")
    public String signup(@RequestParam String email, @RequestParam String password,
                         @RequestParam String name, @RequestParam String role,
                         HttpServletRequest request, Model model) {
        try {
            Role r = "doctor".equalsIgnoreCase(role) ? Role.DOCTOR : Role.PATIENT;
            User u = auth.signup(email, password, name, r);
            // 가입 직후 자동 로그인 (Spring Security context에 직접 주입)
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, java.util.List.of(new SimpleGrantedAuthority(r.name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.getSession(true).setAttribute(
                "SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return r == Role.DOCTOR ? "redirect:/doctor/" : "redirect:/patient/welcome";
        } catch (org.springframework.web.server.ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
            model.addAttribute("role", role);
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "auth/signup";
        }
    }
}
