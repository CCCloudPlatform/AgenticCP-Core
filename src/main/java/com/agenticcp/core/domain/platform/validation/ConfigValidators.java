package com.agenticcp.core.domain.platform.validation;

import com.agenticcp.core.common.exception.ValidationException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * PlatformConfig 입력값에 대한 공통 검증 유틸리티.
 * - 키 정책 검증(길이/시작문자/허용문자)
 * - 타입별 값 검증(NUMBER/BOOLEAN/JSON/ENCRYPTED)
 * - ENCRYPTED는 빈 문자열 불가
 */
public class ConfigValidators {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 설정 키 정책 검증.
     * 길이: 3~128, 시작: 영문/숫자, 허용문자: [a-zA-Z0-9._-]
     */
    public static void validateKeyPolicy(String key) {
        if (key == null || key.isBlank()) {
            throw new ValidationException("key", "must not be blank");
        }
        if (key.length() < 3 || key.length() > 128) {
            throw new ValidationException("key", "length must be between 3 and 128");
        }
        if (!Character.isLetterOrDigit(key.charAt(0))) {
            throw new ValidationException("key", "must start with alphanumeric");
        }
        if (!key.matches("[a-zA-Z0-9._-]+")) {
            throw new ValidationException("key", "allowed chars are [a-zA-Z0-9._-]");
        }
    }

    /**
     * 타입별 값 검증. 유효하지 않으면 ValidationException 발생.
     */
    public static void validateValueByType(PlatformConfig.ConfigType type, String value) {
        if (type == null) {
            throw new ValidationException("type", "must not be null");
        }
        if (value == null) {
            throw new ValidationException("value", "must not be null");
        }
        switch (type) {
            case STRING -> {
                // always valid
            }
            case NUMBER -> validateNumber(value);
            case BOOLEAN -> validateBoolean(value);
            case JSON -> validateJson(value);
            case ENCRYPTED -> validateEncrypted(value);
            default -> throw new ValidationException("type", "unsupported type");
        }
    }

    /** NUMBER 타입 값 검증(BigDecimal 파싱 가능 여부) */
    private static void validateNumber(String value) {
        try {
            new BigDecimal(value);
        } catch (Exception e) {
            throw new ValidationException("value", "must be a valid number");
        }
    }

    /** BOOLEAN 타입 값 검증("true" 또는 "false"만 허용) */
    private static void validateBoolean(String value) {
        if (!("true".equals(value) || "false".equals(value))) {
            throw new ValidationException("value", "must be 'true' or 'false' (lowercase)");
        }
    }

    /** JSON 타입 값 검증(ObjectMapper 파싱 성공 필수) */
    private static void validateJson(String value) {
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw new ValidationException("value", "must be valid JSON");
        }
    }

    /** ENCRYPTED 타입 값 검증(빈 문자열 불가) */
    private static void validateEncrypted(String value) {
        if (value.isBlank()) {
            throw new ValidationException("value", "encrypted value must not be blank");
        }
    }
}


