package com.agenticcp.core.common.util;

/**
 * ENCRYPTED 값에 대한 감사/로그용 마스킹 유틸리티.
 * 
 * 원칙:
 * - ENCRYPTED 타입은 평문을 절대로 노출하지 않는다.
 * - 감사/로그에서는 기본적으로 "***"로 마스킹한다.
 */
public final class EncryptedValueMasker {

    private static final String MASK = "***";

    private EncryptedValueMasker() {}

    /**
     * 감사 로깅용 마스킹. ENCRYPTED면 항상 마스킹 반환.
     * @param value 원본 값(평문/암호문)
     * @param valueType 설정 타입 문자열(예: "ENCRYPTED")
     * @return 마스킹된 문자열 또는 원본(비민감)
     */
    public static String maskForAudit(String value, String valueType) {
        if (isEncryptedType(valueType)) {
            return MASK;
        }
        return safeString(value);
    }

    /**
     * 감사 로깅용 마스킹. ENCRYPTED 여부로 판단.
     * @param value 원본 값(평문/암호문)
     * @param encryptedType true이면 ENCRYPTED 취급
     * @return 마스킹된 문자열 또는 원본(비민감)
     */
    public static String maskForAudit(String value, boolean encryptedType) {
        if (encryptedType) {
            return MASK;
        }
        return safeString(value);
    }

    public static boolean isEncryptedType(String valueType) {
        return valueType != null && "ENCRYPTED".equalsIgnoreCase(valueType.trim());
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}



