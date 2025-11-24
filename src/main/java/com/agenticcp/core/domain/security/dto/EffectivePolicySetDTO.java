package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 유효한 정책 집합 DTO
 *
 * <p>테넌트별 유효한 정책 집합(글로벌 + 테넌트 정책)을 표현합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Schema(description = "유효한 정책 집합 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectivePolicySetDTO {
    
    @Schema(description = "테넌트 ID")
    private String tenantId;
    
    @Schema(description = "테넌트 키")
    private String tenantKey;
    
    @Schema(description = "글로벌 정책 목록")
    @Builder.Default
    private List<PolicySummaryDTO> globalPolicies = new ArrayList<>();
    
    @Schema(description = "테넌트 정책 목록")
    @Builder.Default
    private List<PolicySummaryDTO> tenantPolicies = new ArrayList<>();
    
    @Schema(description = "전체 정책 개수")
    private Integer totalPolicyCount;
    
    @Schema(description = "글로벌 정책 개수")
    private Integer globalPolicyCount;
    
    @Schema(description = "테넌트 정책 개수")
    private Integer tenantPolicyCount;
    
    @Schema(description = "조회 시각")
    private LocalDateTime queriedAt;
    
    @Schema(description = "캐시 히트 여부")
    private Boolean cacheHit;
    
    /**
     * SecurityPolicy 리스트로부터 PolicySummaryDTO 리스트 생성
     */
    public static List<PolicySummaryDTO> toPolicySummaryList(List<SecurityPolicy> policies) {
        if (policies == null) {
            return new ArrayList<>();
        }
        return policies.stream()
                .map(PolicySummaryDTO::from)
                .toList();
    }
    
    /**
     * 통계 계산
     */
    public void calculateCounts() {
        this.globalPolicyCount = globalPolicies != null ? globalPolicies.size() : 0;
        this.tenantPolicyCount = tenantPolicies != null ? tenantPolicies.size() : 0;
        this.totalPolicyCount = this.globalPolicyCount + this.tenantPolicyCount;
    }
    
    /**
     * 정책 요약 DTO
     */
    @Schema(description = "정책 요약 DTO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicySummaryDTO {
        
        @Schema(description = "정책 ID")
        private Long id;
        
        @Schema(description = "정책 키")
        private String policyKey;
        
        @Schema(description = "정책 이름")
        private String policyName;
        
        @Schema(description = "정책 타입")
        private String policyType;
        
        @Schema(description = "우선순위")
        private Integer priority;
        
        @Schema(description = "심각도")
        private String severity;
        
        @Schema(description = "활성화 여부")
        private Boolean isEnabled;
        
        @Schema(description = "글로벌 정책 여부")
        private Boolean isGlobal;
        
        @Schema(description = "시스템 정책 여부")
        private Boolean isSystem;
        
        @Schema(description = "유효 시작일")
        private LocalDateTime effectiveFrom;
        
        @Schema(description = "유효 종료일")
        private LocalDateTime effectiveUntil;
        
        /**
         * SecurityPolicy 엔티티로부터 PolicySummaryDTO 생성
         */
        public static PolicySummaryDTO from(SecurityPolicy policy) {
            return PolicySummaryDTO.builder()
                    .id(policy.getId())
                    .policyKey(policy.getPolicyKey())
                    .policyName(policy.getPolicyName())
                    .policyType(policy.getPolicyType() != null ? policy.getPolicyType().name() : null)
                    .priority(policy.getPriority())
                    .severity(policy.getSeverity() != null ? policy.getSeverity().name() : null)
                    .isEnabled(policy.getIsEnabled())
                    .isGlobal(policy.getIsGlobal())
                    .isSystem(policy.getIsSystem())
                    .effectiveFrom(policy.getEffectiveFrom())
                    .effectiveUntil(policy.getEffectiveUntil())
                    .build();
        }
    }
}

