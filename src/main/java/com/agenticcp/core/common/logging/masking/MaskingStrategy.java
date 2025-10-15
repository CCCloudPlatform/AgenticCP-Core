package com.agenticcp.core.common.logging.masking;

/**
 * 마스킹 처리 전략 인터페이스
 * 
 * 각 마스킹 타입별로 구체적인 마스킹 로직을 구현합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface MaskingStrategy {
    String mask(String value);
    MaskingType getType();
}
