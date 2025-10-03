package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 기능 플래그 타겟팅 규칙 엔티티
 * 
 * 기능 플래그에 대한 고급 타겟팅 규칙을 정의합니다.
 * 클라우드 프로바이더, 리전, 테넌트 타입, 사용자 역할 등 다양한 조건으로 
 * 기능 플래그를 타겟팅할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "feature_flag_target_rules")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagTargetRule extends BaseEntity {

    /**
     * 연관된 기능 플래그
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    /**
     * 규칙 이름
     */
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    /**
     * 규칙 설명
     */
    @Column(name = "rule_description")
    private String ruleDescription;

    /**
     * 규칙 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    /**
     * 규칙 조건 (JSON 형태)
     * 타겟팅 조건을 JSON으로 저장
     */
    @Column(name = "rule_condition", columnDefinition = "TEXT")
    private String ruleCondition;

    /**
     * 규칙 값 (JSON 형태)
     * 타겟팅 대상 값들을 JSON으로 저장
     */
    @Column(name = "rule_value", columnDefinition = "TEXT")
    private String ruleValue;

    /**
     * 규칙 우선순위
     * 높은 우선순위가 먼저 평가됨
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * 규칙 활성화 여부
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 타겟팅 규칙 타입 열거형
     */
    public enum RuleType {
        /**
         * 클라우드 프로바이더별 타겟팅 (AWS, Azure, GCP 등)
         */
        CLOUD_PROVIDER,
        
        /**
         * 클라우드 리전별 타겟팅
         */
        CLOUD_REGION,
        
        /**
         * 테넌트 타입별 타겟팅 (Enterprise, SMB, Individual 등)
         */
        TENANT_TYPE,
        
        /**
         * 테넌트 등급별 타겟팅 (Premium, Standard, Basic 등)
         */
        TENANT_TIER,
        
        /**
         * 사용자 역할별 타겟팅 (Admin, User, Viewer 등)
         */
        USER_ROLE,
        
        /**
         * 사용자 속성별 타겟팅 (사용자 정의 속성 기반)
         */
        USER_ATTRIBUTE,
        
        /**
         * 커스텀 속성별 타겟팅 (임의의 JSON 조건)
         */
        CUSTOM_ATTRIBUTE,
        
        /**
         * 퍼센트 롤아웃 (이번에는 설정만 지원)
         */
        PERCENTAGE_ROLLOUT
    }

    /**
     * 규칙 활성화
     */
    public void activate() {
        this.isEnabled = true;
    }

    /**
     * 규칙 비활성화
     */
    public void deactivate() {
        this.isEnabled = false;
    }

    /**
     * 우선순위 업데이트
     */
    public void updatePriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 규칙 정보 업데이트
     */
    public void updateRuleInfo(String ruleName, String ruleDescription, 
                             String ruleCondition, String ruleValue) {
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.ruleCondition = ruleCondition;
        this.ruleValue = ruleValue;
    }
}
