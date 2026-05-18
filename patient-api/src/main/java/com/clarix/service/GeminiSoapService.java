package com.clarix.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.clarix.domain.User;
import com.clarix.repo.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 의사 자유 노트 + 환자 컨텍스트를 Gemini에 보내 SOAP 4 필드를 자동 작성.
 * 키는 환경변수 GEMINI_API_KEY 또는 GOOGLE_API_KEY (application.properties → gemini.api.key).
 */
@Service
public class GeminiSoapService {

    private final String apiKey;
    private final String model;
    private final UserRepository users;
    private final DoctorService doctorSvc;
    private final ObjectMapper json = new ObjectMapper();
    private final RestClient http = RestClient.builder().build();

    public GeminiSoapService(@Value("${gemini.api.key:}") String apiKey,
                             @Value("${gemini.api.model:gemini-2.0-flash}") String model,
                             UserRepository users,
                             DoctorService doctorSvc) {
        this.apiKey = apiKey;
        this.model = model;
        this.users = users;
        this.doctorSvc = doctorSvc;
    }

    public record Soap(String subjective, String objective, String assessment, String plan) {}

    public Soap autofill(UUID patientId, String freeText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "GEMINI_API_KEY 또는 GOOGLE_API_KEY가 설정되지 않았습니다");
        }
        User patient = users.findById(patientId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "환자를 찾을 수 없음"));

        String prompt = buildPrompt(patient, freeText);

        // Gemini REST: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
            )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
            + model + ":generateContent?key=" + apiKey;

        try {
            String responseBody = http.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

            JsonNode root = json.readTree(responseBody);
            String text = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText();
            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 응답 비어있음");
            }
            // 응답이 JSON 문자열일 수 있고 코드블록 안에 있을 수도 있어 정리
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                int firstNl = cleaned.indexOf('\n');
                if (firstNl > 0) cleaned = cleaned.substring(firstNl + 1);
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
                cleaned = cleaned.trim();
            }
            JsonNode soap = json.readTree(cleaned);
            return new Soap(
                soap.path("subjective").asText(""),
                soap.path("objective").asText(""),
                soap.path("assessment").asText(""),
                soap.path("plan").asText("")
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Gemini 호출 실패: " + ex.getMessage());
        }
    }

    private String buildPrompt(User patient, String freeText) {
        var header = doctorSvc.patientHeader(patient.getId());
        StringBuilder ctx = new StringBuilder();
        ctx.append("환자: ").append(patient.getName()).append("\n");
        ctx.append("병원: ").append(header.hospitalName() == null ? "—" : header.hospitalName()).append("\n");
        ctx.append("활성 처방: ").append(header.activePrescriptions()).append("건\n");
        if (header.adherence7d() != null) ctx.append("7일 복약: ").append(header.adherence7d()).append("%\n");
        if (header.avgMood7d() != null)   ctx.append("7일 평균 기분: ").append(header.avgMood7d()).append("/5\n");
        if (header.phq9Score() != null) {
            ctx.append("PHQ-9: ").append(header.phq9Score())
               .append(" (").append(header.phq9Severity().label()).append(")\n");
        }
        if (header.lastMoodEmotion() != null) {
            ctx.append("최근 감정: ").append(header.lastMoodEmotion().label());
            if (header.lastMoodJournal() != null && !header.lastMoodJournal().isBlank()) {
                ctx.append(" — ").append(header.lastMoodJournal());
            }
            ctx.append("\n");
        }
        if (!header.alerts().isEmpty()) {
            ctx.append("특이사항:\n");
            for (var a : header.alerts()) ctx.append("  - ").append(a.text()).append("\n");
        }

        return """
            당신은 정신건강의학과 임상 보조 AI입니다.
            아래 환자 컨텍스트와 의사가 작성한 자유 노트를 바탕으로 SOAP 형식의 임상 노트를 작성해주세요.
            반드시 한국어로, 의학적·임상적으로 정확하고 간결하게 작성합니다.
            S(주관적), O(객관적), A(평가), P(계획) 네 항목 모두 채우되, 자유 노트에 정보가 부족하면 환자 컨텍스트로 보완합니다.

            [환자 컨텍스트]
            %s

            [의사 자유 노트]
            \"\"\"
            %s
            \"\"\"

            응답은 반드시 다음 JSON 형식만 반환:
            {
              "subjective": "...",
              "objective": "...",
              "assessment": "...",
              "plan": "..."
            }
            """.formatted(ctx.toString(), freeText == null ? "" : freeText.trim());
    }
}
