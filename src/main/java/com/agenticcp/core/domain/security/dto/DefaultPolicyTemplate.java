package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기본 정책 템플릿
 * 
 * <p>테넌트 생성 시 자동으로 생성되는 기본 보안 정책 템플릿을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultPolicyTemplate {
    
    private String policyKey;
    private String policyName;
    private String description;
    private SecurityPolicy.PolicyType policyType;
    private SecurityPolicy.Severity severity;
    private Integer priority;
    private String rules;
    private String conditions;
    private String actions;
    private Boolean isSystem;
    
    /**
     * 접근 제어 기본 정책 템플릿
     */
    public static DefaultPolicyTemplate accessControlTemplate(String tenantKey) {
        return DefaultPolicyTemplate.builder()
                .policyKey(tenantKey + "_DEFAULT_ACCESS_CONTROL")
                .policyName("기본 접근 제어 정책")
                .description("테넌트 기본 접근 제어 정책")
                .policyType(SecurityPolicy.PolicyType.ACCESS_CONTROL)
                .severity(SecurityPolicy.Severity.HIGH)
                .priority(100)
                .rules("{\"defaultAction\": \"DENY\", \"allowOwner\": true}")
                .conditions("{\"requireAuthentication\": true}")
                .actions("{\"onDeny\": \"LOG_AND_ALERT\"}")
                .isSystem(true)
                .build();
    }
    
    /**
     * 인증 기본 정책 템플릿
     */
    public static DefaultPolicyTemplate authenticationTemplate(String tenantKey) {
        return DefaultPolicyTemplate.builder()
                .policyKey(tenantKey + "_DEFAULT_AUTHENTICATION")
                .policyName("기본 인증 정책")
                .description("테넌트 기본 인증 정책")
                .policyType(SecurityPolicy.PolicyType.AUTHENTICATION)
                .severity(SecurityPolicy.Severity.CRITICAL)
                .priority(200)
                .rules("{\"requireMFA\": false, \"passwordComplexity\": \"MEDIUM\", \"sessionTimeout\": 3600}")
                .conditions("{\"ipWhitelist\": [], \"allowedTimeRange\": \"ALL\"}")
                .actions("{\"onFailure\": \"INCREMENT_ATTEMPT\", \"maxAttempts\": 5}")
                .isSystem(true)
                .build();
    }
    
    /**
     * 인가 기본 정책 템플릿
     */
    public static DefaultPolicyTemplate authorizationTemplate(String tenantKey) {
        return DefaultPolicyTemplate.builder()
                .policyKey(tenantKey + "_DEFAULT_AUTHORIZATION")
                .policyName("기본 인가 정책")
                .description("테넌트 기본 권한 부여 정책")
                .policyType(SecurityPolicy.PolicyType.AUTHORIZATION)
                .severity(SecurityPolicy.Severity.HIGH)
                .priority(150)
                .rules("{\"defaultRole\": \"VIEWER\", \"roleHierarchy\": true}")
                .conditions("{\"checkOwnership\": true, \"checkTenant\": true}")
                .actions("{\"onUnauthorized\": \"LOG_AND_DENY\"}")
                .isSystem(true)
                .build();
    }
    
    /**
     * 데이터 보호 기본 정책 템플릿
     */
    public static DefaultPolicyTemplate dataProtectionTemplate(String tenantKey) {
        return DefaultPolicyTemplate.builder()
                .policyKey(tenantKey + "_DEFAULT_DATA_PROTECTION")
                .policyName("기본 데이터 보호 정책")
                .description("테넌트 기본 데이터 보호 정책")
                .policyType(SecurityPolicy.PolicyType.DATA_PROTECTION)
                .severity(SecurityPolicy.Severity.HIGH)
                .priority(120)
                .rules("{\"encryptAtRest\": true, \"encryptInTransit\": true, \"dataRetentionDays\": 90}")
                .conditions("{\"sensitiveDataPatterns\": [\"SSN\", \"CREDIT_CARD\", \"EMAIL\"]}")
                .actions("{\"onViolation\": \"BLOCK_AND_ALERT\"}")
                .isSystem(true)
                .build();
    }
    
    /**
     * 감사 로깅 기본 정책 템플릿
     */
    public static DefaultPolicyTemplate auditLoggingTemplate(String tenantKey) {
        return DefaultPolicyTemplate.builder()
                .policyKey(tenantKey + "_DEFAULT_AUDIT_LOGGING")
                .policyName("기본 감사 로깅 정책")
                .description("테넌트 기본 감사 로깅 정책")
                .policyType(SecurityPolicy.PolicyType.AUDIT_LOGGING)
                .severity(SecurityPolicy.Severity.MEDIUM)
                .priority(80)
                .rules("{\"logLevel\": \"INFO\", \"retentionDays\": 365, \"logLocation\": \"DATABASE\"}")
                .conditions("{\"logEvents\": [\"CREATE\", \"UPDATE\", \"DELETE\", \"ACCESS\"]}")
                .actions("{\"onCritical\": \"IMMEDIATE_ALERT\"}")
                .isSystem(true)
                .build();
    }
}

