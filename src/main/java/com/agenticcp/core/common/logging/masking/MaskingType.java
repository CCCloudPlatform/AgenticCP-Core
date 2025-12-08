package com.agenticcp.core.common.logging.masking;

/**
 * 민감 정보 마스킹 처리 유형을 정의한 enum입니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
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
    ACCESS_KEY,
    ACCOUNT_SCOPE,
    TOKEN,
    TENANT_KEY,
    USERNAME,
    DEFAULT
}
