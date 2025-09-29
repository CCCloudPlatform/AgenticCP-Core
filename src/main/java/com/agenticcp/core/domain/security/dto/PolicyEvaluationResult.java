package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 정책 평가 결과 데이터 전송 객체
 * 
 * <p>정책 엔진이 평가한 결과를 담는 DTO입니다.
 * 정책 평가 결과에 대한 모든 정보를 포함합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResult {
    
    /**
     * 정책 결정 결과
     */
    private PolicyDecision decision;
    
    /**
     * 적용된 정책 키
     */
    private String policyKey;
    
    /**
     * 적용된 정책 이름
     */
    private String policyName;
    
    /**
     * 결정 이유
     */
    private String reason;
    
    /**
     * 수행할 액션 목록
     */
    private List<PolicyAction> actions;
    
    /**
     * 평가 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime evaluatedAt;
    
    /**
     * 결과 만료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    /**
     * 결과가 만료되었는지 여부
     */
    @Builder.Default
    private boolean expired = false;
    
    /**
     * 평가에 소요된 시간 (밀리초)
     */
    private Long evaluationTimeMs;
    
    /**
     * 평가된 정책의 우선순위
     */
    private Integer policyPriority;
    
    /**
     * 평가 과정에서 발생한 경고 메시지
     */
    private List<String> warnings;
    
    /**
     * 평가 과정에서 발생한 오류 메시지
     */
    private List<String> errors;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, Object> metadata;
    
    /**
     * 요청 ID (원본 요청과 연결)
     */
    private String requestId;
    
    /**
     * 평가된 조건들
     */
    private Map<String, Boolean> evaluatedConditions;
    
    /**
     * 평가된 규칙들
     */
    private Map<String, String> evaluatedRules;
    
    /**
     * 정책 버전
     */
    private String policyVersion;
    
    /**
     * 정책 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime policyCreatedAt;
    
    /**
     * 정책 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime policyUpdatedAt;
    
    /**
     * 허용 결과 생성 팩토리 메서드
     * 
     * @param reason 허용 이유
     * @return PolicyEvaluationResult
     */
    public static PolicyEvaluationResult allow(String reason) {
        return PolicyEvaluationResult.builder()
                .decision(PolicyDecision.ALLOW)
                .reason(reason)
                .evaluatedAt(LocalDateTime.now())
                .expired(false)
                .build();
    }
    
    /**
     * 허용 결과 생성 팩토리 메서드 (정책 정보 포함)
     * 
     * @param policyKey 정책 키
     * @param policyName 정책 이름
     * @param reason 허용 이유
     * @return PolicyEvaluationResult
     */
    public static PolicyEvaluationResult allow(String policyKey, String policyName, String reason) {
        return PolicyEvaluationResult.builder()
                .decision(PolicyDecision.ALLOW)
                .policyKey(policyKey)
                .policyName(policyName)
                .reason(reason)
                .evaluatedAt(LocalDateTime.now())
                .expired(false)
                .build();
    }
    
    /**
     * 거부 결과 생성 팩토리 메서드
     * 
     * @param reason 거부 이유
     * @return PolicyEvaluationResult
     */
    public static PolicyEvaluationResult deny(String reason) {
        return PolicyEvaluationResult.builder()
                .decision(PolicyDecision.DENY)
                .reason(reason)
                .evaluatedAt(LocalDateTime.now())
                .expired(false)
                .build();
    }
    
    /**
     * 거부 결과 생성 팩토리 메서드 (정책 정보 포함)
     * 
     * @param policyKey 정책 키
     * @param policyName 정책 이름
     * @param reason 거부 이유
     * @return PolicyEvaluationResult
     */
    public static PolicyEvaluationResult deny(String policyKey, String policyName, String reason) {
        return PolicyEvaluationResult.builder()
                .decision(PolicyDecision.DENY)
                .policyKey(policyKey)
                .policyName(policyName)
                .reason(reason)
                .evaluatedAt(LocalDateTime.now())
                .expired(false)
                .build();
    }
    
    /**
     * 결정할 수 없음 결과 생성 팩토리 메서드
     * 
     * @param reason 이유
     * @return PolicyEvaluationResult
     */
    public static PolicyEvaluationResult inconclusive(String reason) {
        return PolicyEvaluationResult.builder()
                .decision(PolicyDecision.INCONCLUSIVE)
                .reason(reason)
                .evaluatedAt(LocalDateTime.now())
                .expired(false)
                .build();
    }
    
    /**
     * 결과가 만료되었는지 확인
     * 
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return expired;
        }
        return LocalDateTime.now().isAfter(expiresAt) || expired;
    }
    
    /**
     * 접근이 허용되었는지 확인
     * 
     * @return 허용되었으면 true, 그렇지 않으면 false
     */
    public boolean isAllowed() {
        return decision != null && decision.isAllow();
    }
    
    /**
     * 접근이 거부되었는지 확인
     * 
     * @return 거부되었으면 true, 그렇지 않으면 false
     */
    public boolean isDenied() {
        return decision != null && decision.isDeny();
    }
    
    /**
     * 결정할 수 없는 상태인지 확인
     * 
     * @return 결정할 수 없으면 true, 그렇지 않으면 false
     */
    public boolean isInconclusive() {
        return decision != null && decision.isInconclusive();
    }
    
    /**
     * 메타데이터에서 특정 키의 값을 가져옴
     * 
     * @param key 메타데이터 키
     * @return 메타데이터 값
     */
    public Object getMetadataValue(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
    
    /**
     * 메타데이터에 값을 설정
     * 
     * @param key 메타데이터 키
     * @param value 메타데이터 값
     */
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 경고 메시지 추가
     * 
     * @param warning 경고 메시지
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new java.util.ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * 오류 메시지 추가
     * 
     * @param error 오류 메시지
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new java.util.ArrayList<>();
        }
        errors.add(error);
    }
    
    /**
     * 평가 시간 설정
     * 
     * @param startTime 평가 시작 시간
     */
    public void setEvaluationTime(LocalDateTime startTime) {
        if (startTime != null) {
            this.evaluationTimeMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
    }
    
    /**
     * 결과 만료 시간 설정 (현재 시간으로부터 지정된 분 후)
     * 
     * @param minutes 만료까지 남은 분
     */
    public void setExpirationMinutes(int minutes) {
        this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
    }
    
    /**
     * 결과 만료 시간 설정 (현재 시간으로부터 지정된 초 후)
     * 
     * @param seconds 만료까지 남은 초
     */
    public void setExpirationSeconds(int seconds) {
        this.expiresAt = LocalDateTime.now().plusSeconds(seconds);
    }
    
    @Override
    public String toString() {
        return String.format("PolicyEvaluationResult{decision=%s, policyKey='%s', reason='%s', evaluatedAt=%s, expired=%s}", 
            decision, policyKey, reason, evaluatedAt, expired);
    }
}
