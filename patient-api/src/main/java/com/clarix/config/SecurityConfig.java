package com.clarix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

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
        // BCrypt는 salt를 자동 포함하므로 같은 비밀번호도 매번 다른 해시가 됩니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserRepository users) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                // 정적 리소스와 인증 화면은 로그인 전에도 접근 가능해야 합니다.
                .requestMatchers("/", "/health", "/auth/**", "/css/**", "/js/**", "/img/**").permitAll()
                // URL prefix마다 역할을 분리해 화면 진입 자체를 막습니다.
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
                // 이 URL은 HomeController가 아니라 Spring Security filter가 처리합니다.
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler(users))
                .failureHandler((request, response, exception) -> {
                    // 로그인 실패 후에도 환자/의사 로그인 화면 문맥을 유지하기 위한 role 파라미터입니다.
                    String role = "doctor".equalsIgnoreCase(request.getParameter("role"))
                        ? "doctor"
                        : "patient";
                    response.sendRedirect(request.getContextPath()
                        + "/auth/login?role=" + role + "&error=1");
                })
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(e -> e
                // 역할이 맞지 않는 화면에 접근하면 Spring 기본 Whitelabel 대신 안내 화면으로 보냅니다.
                .accessDeniedHandler((request, response, exception) ->
                    response.sendRedirect(accessDeniedUrl(request))
                )
            );

        return http.build();
    }

    private String accessDeniedUrl(jakarta.servlet.http.HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI();
        if (!contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        String targetRole = targetRole(path);
        String query = targetRole == null ? "" : "?targetRole=" + targetRole;
        return contextPath + "/auth/access-denied" + query;
    }

    private String targetRole(String path) {
        if (path.equals("/patient") || path.startsWith("/patient/")) return "patient";
        if (path.equals("/doctor") || path.startsWith("/doctor/")) return "doctor";
        if (path.equals("/reception") || path.startsWith("/reception/")) return "reception";
        if (path.equals("/staff") || path.startsWith("/staff/")) return "staff";
        if (path.equals("/admin") || path.startsWith("/admin/")) return "admin";
        return null;
    }

    private SimpleUrlAuthenticationSuccessHandler roleBasedSuccessHandler(UserRepository users) {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication) {
                // Authentication에는 email/authority만 있으므로 DB에서 최신 User role을 다시 확인합니다.
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
