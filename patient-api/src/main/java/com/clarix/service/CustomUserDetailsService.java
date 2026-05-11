package com.clarix.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.clarix.domain.User;
import com.clarix.repo.UserRepository;

/**
 * Spring Security가 폼 로그인 시 호출. 우리 User 엔티티를 UserDetails로 변환.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다"));
        return org.springframework.security.core.userdetails.User
            .withUsername(u.getEmail())
            .password(u.getPasswordHash())
            .authorities(new SimpleGrantedAuthority(u.getRole().name()))
            .build();
    }
}
