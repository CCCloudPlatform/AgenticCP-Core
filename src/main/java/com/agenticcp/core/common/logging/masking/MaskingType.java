package com.agenticcp.core.common.logging.masking;

/**
 * 마스킹 처리 유형 열거형
 * 
 * 다양한 민감 정보에 대한 마스킹 규칙을 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public enum MaskingType {

    PASSWORD,
    CREDIT_CARD,
    IP_ADDRESS,
    EMAIL,
    PHONE_NUMBER,
    SSN,
    SECRET_KEY,
    TOKEN,
    DEFAULT
}
