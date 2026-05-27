package com.clarix.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.UserRepository;

import jakarta.servlet.http.HttpSession;

/**
 * SecurityContext에서 현재 인증 사용자를 꺼내 우리 User 엔티티로 반환.
 * 컨트롤러는 메서드 시그니처에 HttpSession을 받지 않아도 되지만,
 * 기존 호출부 호환을 위해 HttpSession 매개변수 시그니처도 유지.
 */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) { this.users = users; }

    public boolean isLoggedIn(HttpSession session) {
        // 세션 객체보다 SecurityContext가 진짜 인증 상태의 기준입니다.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }

    public User require(HttpSession session) {
        // 인증 안 된 요청은 Controller 초입에서 바로 끊어 이후 로직이 User null을 신경 쓰지 않게 합니다.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no session");
        }
        // Spring Security principal은 email 문자열이고, 화면/Service에는 우리 User 엔티티가 필요합니다.
        return users.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
    }

    public User requireRole(HttpSession session, Role role) {
        User u = require(session);
        // URL 보안은 SecurityConfig가 담당하지만, Controller 내부에서도 역할을 다시 확인해 방어합니다.
        if (u.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role mismatch");
        }
        return u;
    }
}
