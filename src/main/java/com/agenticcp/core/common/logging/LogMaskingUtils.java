package com.agenticcp.core.common.logging;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 로그 메시지 내 민감 정보를 마스킹하는 유틸리티 클래스입니다.
 * 이메일/전화/카드/토큰 등 패턴 기반 마스キング을 제공합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Slf4j
public class LogMaskingUtils {
    
    // 패스워드 마스킹 패턴
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|pwd|pass)\\s*[:=]\\s*[^\\s,}]+", Pattern.CASE_INSENSITIVE);
    
    // 토큰 마스킹 패턴
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(token|auth|authorization)\\s*[:=]\\s*[^\\s,}]+", Pattern.CASE_INSENSITIVE);
    
    // API 키 마스킹 패턴
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[^\\s,}]+", Pattern.CASE_INSENSITIVE);
    
    // 이메일 마스킹 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    
    // 전화번호 마스킹 패턴
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(\\d{2,3})[-.]?(\\d{3,4})[-.]?(\\d{4})\\b");
    
    // 신용카드 번호 마스킹 패턴
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})\\b");
    
    // 주민등록번호 마스킹 패턴
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b(\\d{6})[-]?(\\d{7})\\b");
    
    // 마스킹 문자
    private static final String MASK_CHAR = "*";
    
    /**
     * 로그 메시지에서 민감한 정보를 마스킹 처리합니다.
     * 
     * @param message 원본 메시지
     * @return 마스킹된 메시지
     */
    public static String maskSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        try {
            String masked = message;
            
            // 패스워드 마스킹
            masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1=***");
            
            // 토큰 마스킹
            masked = TOKEN_PATTERN.matcher(masked).replaceAll("$1=***");
            
            // API 키 마스킹
            masked = API_KEY_PATTERN.matcher(masked).replaceAll("$1=***");
            
            // 이메일 마스킹
            masked = EMAIL_PATTERN.matcher(masked).replaceAll(match -> maskEmail(match.group()));
            
            // 전화번호 마스킹
            masked = PHONE_PATTERN.matcher(masked).replaceAll(match -> maskPhone(match.group()));
            
            // 신용카드 번호 마스킹
            masked = CARD_PATTERN.matcher(masked).replaceAll(match -> maskCardNumber(match.group()));
            
            // 주민등록번호 마스킹
            masked = SSN_PATTERN.matcher(masked).replaceAll(match -> maskSSN(match.group()));
            
            return masked;
        } catch (Exception e) {
            log.warn("로그 마스킹 처리 중 오류 발생: {}", e.getMessage());
            return message; // 오류 발생 시 원본 반환
        }
    }
    
    /**
     * 이메일 주소를 마스킹 처리합니다.
     * 
     * @param email 원본 이메일
     * @return 마스킹된 이메일
     */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return MASK_CHAR.repeat(localPart.length()) + domain;
        }
        
        return localPart.charAt(0) + MASK_CHAR.repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1) + domain;
    }
    
    /**
     * 전화번호를 마스킹 처리합니다.
     * 
     * @param phone 원본 전화번호
     * @return 마스킹된 전화번호
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return phone;
        }
        
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + MASK_CHAR.repeat(3) + "-" + MASK_CHAR.repeat(4);
        } else if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + MASK_CHAR.repeat(4) + "-" + MASK_CHAR.repeat(4);
        }
        
        return phone;
    }
    
    /**
     * 신용카드 번호를 마스킹 처리합니다.
     * 
     * @param cardNumber 원본 카드 번호
     * @return 마스킹된 카드 번호
     */
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return cardNumber;
        }
        
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 16) {
            return cardNumber;
        }
        
        return digits.substring(0, 4) + "-" + MASK_CHAR.repeat(4) + "-" + MASK_CHAR.repeat(4) + "-" + digits.substring(12);
    }
    
    /**
     * 주민등록번호를 마스킹 처리합니다.
     * 
     * @param ssn 원본 주민등록번호
     * @return 마스킹된 주민등록번호
     */
    private static String maskSSN(String ssn) {
        if (ssn == null || ssn.isEmpty()) {
            return ssn;
        }
        
        String digits = ssn.replaceAll("[^0-9]", "");
        if (digits.length() != 13) {
            return ssn;
        }
        
        return digits.substring(0, 6) + "-" + MASK_CHAR.repeat(7);
    }
    
    /**
     * 특정 필드 값을 마스킹 처리합니다.
     * 
     * @param value 원본 값
     * @param fieldName 필드명
     * @return 마스킹된 값
     */
    public static String maskFieldValue(String value, String fieldName) {
        if (value == null || value.isEmpty() || fieldName == null) {
            return value;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        
        // 민감한 필드명 체크
        if (lowerFieldName.contains("password") || lowerFieldName.contains("pwd") || lowerFieldName.contains("pass")) {
            return "***";
        }
        
        if (lowerFieldName.contains("token") || lowerFieldName.contains("auth") || lowerFieldName.contains("key")) {
            return "***";
        }
        
        if (lowerFieldName.contains("email")) {
            return maskEmail(value);
        }
        
        if (lowerFieldName.contains("phone") || lowerFieldName.contains("tel")) {
            return maskPhone(value);
        }
        
        if (lowerFieldName.contains("card") || lowerFieldName.contains("credit")) {
            return maskCardNumber(value);
        }
        
        if (lowerFieldName.contains("ssn") || lowerFieldName.contains("resident")) {
            return maskSSN(value);
        }
        
        return value;
    }
    
    /**
     * 객체의 민감한 필드를 마스킹 처리합니다.
     * 
     * @param obj 원본 객체
     * @return 마스킹된 객체의 문자열 표현
     */
    public static String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        return maskSensitiveData(obj.toString());
    }
    
    /**
     * 테넌트 키를 마스킹 처리합니다.
     * 
     * @param tenantKey 원본 테넌트 키
     * @return 마스킹된 테넌트 키
     */
    public static String maskTenantKey(String tenantKey) {
        if (tenantKey == null || tenantKey.isEmpty()) {
            return tenantKey;
        }
        
        // 테넌트 키가 너무 짧으면 전체 마스킹
        if (tenantKey.length() <= 2) {
            return MASK_CHAR.repeat(tenantKey.length());
        }
        
        // 첫 글자와 마지막 글자만 보여주고 나머지는 마스킹
        return tenantKey.charAt(0) + MASK_CHAR.repeat(tenantKey.length() - 2) + tenantKey.charAt(tenantKey.length() - 1);
    }
}
