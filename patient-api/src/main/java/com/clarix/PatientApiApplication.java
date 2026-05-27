package com.clarix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatientApiApplication {
    /**
     * Spring Boot 시작점입니다.
     * 이 한 줄이 내장 Tomcat, Spring MVC, JPA, Security 자동 설정을 모두 부팅합니다.
     */
    public static void main(String[] args) {
        SpringApplication.run(PatientApiApplication.class, args);
    }
}
