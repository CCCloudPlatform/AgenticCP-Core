package com.agenticcp.core.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Map<String, String> ↔ JSON String 변환을 위한 JPA AttributeConverter
 * 
 * 엔티티의 tags 필드를 Map 타입으로 선언하면서 DB에는 JSON 문자열로 저장합니다.
 * 서비스 레이어에서 JSON 직렬화 로직을 제거하고 인프라 레이어로 이동시킵니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Converter
public class TagMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TYPE_REFERENCE = new TypeReference<>() {};

    /**
     * 엔티티 → DB: Map을 JSON 문자열로 변환
     *
     * @param attribute 엔티티의 Map<String, String> 필드
     * @return JSON 문자열 (null이거나 비어있으면 null 반환)
     */
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.warn("[TagMapConverter] Map → JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * DB → 엔티티: JSON 문자열을 Map으로 변환
     *
     * @param dbData DB에 저장된 JSON 문자열
     * @return Map<String, String> (null이거나 비어있으면 null 반환)
     */
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.warn("[TagMapConverter] JSON → Map 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}

