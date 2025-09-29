package com.agenticcp.core.domain.security.enums;

/**
 * 정책 평가 결과를 나타내는 열거형
 * 
 * <p>정책 엔진이 요청을 평가한 후 내리는 결정을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum PolicyDecision {
    
    /**
     * 접근 허용
     * 정책 조건을 만족하여 요청이 허용됨
     */
    ALLOW("ALLOW", "접근 허용"),
    
    /**
     * 접근 거부
     * 정책 조건을 만족하지 않아 요청이 거부됨
     */
    DENY("DENY", "접근 거부"),
    
    /**
     * 결정할 수 없음
     * 현재 정책으로는 결정을 내릴 수 없어 다른 정책으로 넘어감
     */
    INCONCLUSIVE("INCONCLUSIVE", "결정할 수 없음");
    
    private final String code;
    private final String description;
    
    /**
     * PolicyDecision 생성자
     * 
     * @param code 정책 결정 코드
     * @param description 정책 결정 설명
     */
    PolicyDecision(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 정책 결정 코드 반환
     * 
     * @return 정책 결정 코드
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 정책 결정 설명 반환
     * 
     * @return 정책 결정 설명
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 코드로부터 PolicyDecision 찾기
     * 
     * @param code 정책 결정 코드
     * @return 해당하는 PolicyDecision, 없으면 null
     */
    public static PolicyDecision fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (PolicyDecision decision : values()) {
            if (decision.code.equalsIgnoreCase(code)) {
                return decision;
            }
        }
        return null;
    }
    
    /**
     * 접근 허용 여부 확인
     * 
     * @return 허용이면 true, 그렇지 않으면 false
     */
    public boolean isAllow() {
        return this == ALLOW;
    }
    
    /**
     * 접근 거부 여부 확인
     * 
     * @return 거부면 true, 그렇지 않으면 false
     */
    public boolean isDeny() {
        return this == DENY;
    }
    
    /**
     * 결정할 수 없음 여부 확인
     * 
     * @return 결정할 수 없으면 true, 그렇지 않으면 false
     */
    public boolean isInconclusive() {
        return this == INCONCLUSIVE;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", code, description);
    }
}
