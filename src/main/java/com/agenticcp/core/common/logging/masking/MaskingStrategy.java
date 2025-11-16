package com.agenticcp.core.common.logging.masking;

/**
 * 마스킹 처리 전략 인터페이스입니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
public interface MaskingStrategy {
    String mask(String value);
    MaskingType getType();
}
