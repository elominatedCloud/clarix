package com.clarix.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Map ↔ JSON 문자열 변환 헬퍼. JSONB 컬럼 대신 TEXT에 직렬화 저장 (H2/Postgres 공통).
 */
@Component
public class JsonStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public String stringify(Map<String, ?> map) {
        try {
            return map == null ? "{}" : mapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid map", e);
        }
    }

    public Map<String, Object> parse(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return mapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
