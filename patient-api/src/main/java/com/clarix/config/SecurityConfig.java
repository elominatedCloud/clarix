package com.clarix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.UserRepository;

/**
 * 12주차: Spring Security 폼 로그인 + BCrypt 패스워드.
 *
 * 흐름:
 *   - 비로그인 사용자가 보호 페이지에 접근 → /auth/login으로 자동 redirect
 *   - 로그인 성공 → 역할별 홈 (/patient/ 또는 /doctor/)으로 redirect
 *   - 로그아웃 → / 로 redirect
 *   - CSRF 토큰: Thymeleaf의 th:action이 자동 포함 (form post 보호)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserRepository users) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .requestMatchers("/patient/**").hasAuthority(Role.PATIENT.name())
                .requestMatchers("/doctor/**").hasAuthority(Role.DOCTOR.name())
                .requestMatchers("/reception/**").hasAuthority(Role.RECEPTIONIST.name())
                .requestMatchers("/staff/**").hasAnyAuthority(
                    Role.NURSE.name(), Role.TECHNICIAN.name())
                .requestMatchers("/admin/**").hasAuthority(Role.ADMIN.name())
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler(users))
                .failureUrl("/auth/login?error=1")
                .permitAll()
            )
            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "POST"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // H2 콘솔이 iframe에 들어가야 작동 — 같은 출처만 허용
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            // H2 콘솔은 자체 토큰 흐름이 없어서 CSRF 면제.
            // LLM autofill은 fetch 기반이라 CSRF 토큰을 form으로 못 받아서 면제 (인증은 그대로).
            .csrf(c -> c.ignoringRequestMatchers(
                new AntPathRequestMatcher("/h2-console/**"),
                new AntPathRequestMatcher("/doctor/patient/*/llm-soap", "POST"),
                new AntPathRequestMatcher("/doctor/drug-check", "POST")
            ));

        return http.build();
    }

    private SimpleUrlAuthenticationSuccessHandler roleBasedSuccessHandler(UserRepository users) {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication) {
                String email = authentication.getName();
                User u = users.findByEmail(email).orElse(null);
                if (u == null) return "/";
                return switch (u.getRole()) {
                    case DOCTOR       -> "/doctor/";
                    case PATIENT      -> "/patient/";
                    case RECEPTIONIST -> "/reception/";
                    case NURSE, TECHNICIAN -> "/staff/";
                    case ADMIN        -> "/admin/";
                };
            }
        };
    }
}
