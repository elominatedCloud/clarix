package com.clarix.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 기반. SecurityConfig가 제공하는 PasswordEncoder를 위임 호출.
 * 12주차 Spring Security 강의 분량을 그대로 따라간다.
 */
@Component
public class PasswordHasher {
    private final PasswordEncoder encoder;

    public PasswordHasher(PasswordEncoder encoder) { this.encoder = encoder; }

    public String hash(String raw) { return encoder.encode(raw); }

    public boolean matches(String raw, String stored) {
        return stored != null && encoder.matches(raw, stored);
    }
}
