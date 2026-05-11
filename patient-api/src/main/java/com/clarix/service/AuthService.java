package com.clarix.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.clarix.domain.Role;
import com.clarix.domain.StaffInvite;
import com.clarix.domain.User;
import com.clarix.repo.StaffInviteRepository;
import com.clarix.repo.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final StaffInviteRepository invites;

    public AuthService(UserRepository users, PasswordHasher hasher, StaffInviteRepository invites) {
        this.users = users;
        this.hasher = hasher;
        this.invites = invites;
    }

    @Transactional
    public User signup(String email, String rawPassword, String name, Role role) {
        if (email == null || email.isBlank()) throw badRequest("이메일을 입력하세요");
        if (rawPassword == null || rawPassword.length() < 6) throw badRequest("비밀번호는 6자 이상");
        if (name == null || name.isBlank()) throw badRequest("이름을 입력하세요");
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) throw badRequest("이미 가입된 이메일입니다");

        // staff invite가 있으면 invite 우선 (가입 시 선택한 역할 무시)
        StaffInvite invite = invites
            .findFirstByEmailAndActiveTrueAndConsumedAtIsNullOrderByCreatedAtDesc(normalized)
            .orElse(null);

        User u = new User();
        u.setEmail(normalized);
        u.setName(name.trim());
        u.setPasswordHash(hasher.hash(rawPassword));
        if (invite != null) {
            u.setRole(invite.getRole());
            u.setHospital(invite.getHospital());
        } else {
            // staff role은 invite 없이 직접 가입 불가
            if (role == Role.NURSE || role == Role.TECHNICIAN
                || role == Role.RECEPTIONIST || role == Role.ADMIN) {
                throw badRequest("초대된 이메일이 아닙니다. 의사에게 초대 등록을 요청하세요.");
            }
            u.setRole(role);
        }
        User saved = users.save(u);

        if (invite != null) {
            invite.setConsumedAt(java.time.OffsetDateTime.now());
            invites.save(invite);
        }
        return saved;
    }

    public User login(String email, String rawPassword) {
        User u = users.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> badRequest("이메일 또는 비밀번호가 일치하지 않습니다"));
        if (!hasher.matches(rawPassword, u.getPasswordHash())) {
            throw badRequest("이메일 또는 비밀번호가 일치하지 않습니다");
        }
        return u;
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
