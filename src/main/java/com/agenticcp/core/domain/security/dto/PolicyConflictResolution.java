package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.enums.ConflictResolutionStrategy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 정책 충돌 해결 결과를 표현하는 DTO
 * 충돌이 발생한 정책들과 해결 과정, 최종 결정을 담음
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyConflictResolution {
    
    /**
     * 충돌 해결 고유 ID
     */
    private String resolutionId;
    
    /**
     * 충돌이 발생한 리소스 ID
     */
    private String resourceId;
    
    /**
     * 충돌이 발생한 리소스 타입
     */
    private String resourceType;
    
    /**
     * 요청된 액션
     */
    private String action;
    
    /**
     * 충돌이 발생한 정책들의 ID 목록
     */
    @Builder.Default
    private List<String> conflictingPolicyIds = new ArrayList<>();
    
    /**
     * 각 정책의 평가 결과
     * Key: Policy ID, Value: PolicyDecision
     */
    @Builder.Default
    private Map<String, PolicyDecision> policyDecisions = new HashMap<>();
    
    /**
     * 각 정책의 우선순위
     * Key: Policy ID, Value: Priority (1-1000)
     */
    @Builder.Default
    private Map<String, Integer> policyPriorities = new HashMap<>();
    
    /**
     * 사용된 충돌 해결 전략
     */
    private ConflictResolutionStrategy strategy;
    
    /**
     * 최종 결정
     */
    private PolicyDecision finalDecision;
    
    /**
     * 최종 결정에 영향을 준 정책 ID
     */
    private String decidingPolicyId;
    
    /**
     * 충돌 해결 과정 설명
     */
    private String resolutionReason;
    
    /**
     * 충돌 감지 시간
     */
    private LocalDateTime conflictDetectedAt;
    
    /**
     * 충돌 해결 시간
     */
    private LocalDateTime resolvedAt;
    
    /**
     * 해결에 소요된 시간 (밀리초)
     */
    private Long resolutionTimeMs;
    
    /**
     * 충돌 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private ConflictSeverity severity;
    
    /**
     * 추가 메타데이터
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 경고 메시지 목록
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 충돌 여부 확인
     * @return true if 충돌이 발생함
     */
    public boolean hasConflict() {
        return conflictingPolicyIds != null && conflictingPolicyIds.size() > 1;
    }
    
    /**
     * ALLOW와 DENY가 혼재된 충돌 여부 확인
     * @return true if ALLOW와 DENY가 모두 있음
     */
    public boolean hasMixedDecisions() {
        if (policyDecisions == null || policyDecisions.isEmpty()) {
            return false;
        }
        
        boolean hasAllow = policyDecisions.values().stream()
            .anyMatch(d -> d == PolicyDecision.ALLOW);
        boolean hasDeny = policyDecisions.values().stream()
            .anyMatch(d -> d == PolicyDecision.DENY);
        
        return hasAllow && hasDeny;
    }
    
    /**
     * 충돌한 정책 개수 반환
     * @return 충돌한 정책 개수
     */
    public int getConflictCount() {
        return conflictingPolicyIds != null ? conflictingPolicyIds.size() : 0;
    }
    
    /**
     * ALLOW 결정 개수 반환
     * @return ALLOW 개수
     */
    public long getAllowCount() {
        if (policyDecisions == null) {
            return 0;
        }
        return policyDecisions.values().stream()
            .filter(d -> d == PolicyDecision.ALLOW)
            .count();
    }
    
    /**
     * DENY 결정 개수 반환
     * @return DENY 개수
     */
    public long getDenyCount() {
        if (policyDecisions == null) {
            return 0;
        }
        return policyDecisions.values().stream()
            .filter(d -> d == PolicyDecision.DENY)
            .count();
    }
    
    /**
     * 경고 메시지 추가
     * @param warning 경고 메시지
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * 메타데이터 추가
     * @param key 키
     * @param value 값
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 정책 결정 추가
     * @param policyId 정책 ID
     * @param decision 결정
     * @param priority 우선순위
     */
    public void addPolicyDecision(String policyId, PolicyDecision decision, Integer priority) {
        if (conflictingPolicyIds == null) {
            conflictingPolicyIds = new ArrayList<>();
        }
        if (policyDecisions == null) {
            policyDecisions = new HashMap<>();
        }
        if (policyPriorities == null) {
            policyPriorities = new HashMap<>();
        }
        
        conflictingPolicyIds.add(policyId);
        policyDecisions.put(policyId, decision);
        if (priority != null) {
            policyPriorities.put(policyId, priority);
        }
    }
    
    /**
     * 최고 우선순위 정책 ID 반환
     * @return 최고 우선순위 정책 ID
     */
    public String getHighestPriorityPolicyId() {
        if (policyPriorities == null || policyPriorities.isEmpty()) {
            return null;
        }
        
        return policyPriorities.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * 최저 우선순위 정책 ID 반환
     * @return 최저 우선순위 정책 ID
     */
    public String getLowestPriorityPolicyId() {
        if (policyPriorities == null || policyPriorities.isEmpty()) {
            return null;
        }
        
        return policyPriorities.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * 충돌 심각도 Enum
     */
    public enum ConflictSeverity {
        LOW("낮음", "경미한 충돌"),
        MEDIUM("보통", "일반적인 충돌"),
        HIGH("높음", "주요 충돌"),
        CRITICAL("치명적", "심각한 충돌");
        
        private final String displayName;
        private final String description;
        
        ConflictSeverity(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 빌더에 시간 계산을 추가한 커스텀 빌더
     */
    public static class PolicyConflictResolutionBuilder {
        public PolicyConflictResolutionBuilder calculateResolutionTime() {
            if (this.conflictDetectedAt != null && this.resolvedAt != null) {
                long duration = java.time.Duration.between(
                    this.conflictDetectedAt, 
                    this.resolvedAt
                ).toMillis();
                this.resolutionTimeMs = duration;
            }
            return this;
        }
    }
}

