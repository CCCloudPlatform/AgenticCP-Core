package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보안 정책 DTO
 *
 * <p>SecurityPolicy 엔티티의 데이터 전송 객체로, JSON 직렬화 시 Lazy Loading 문제를 해결합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Schema(description = "보안 정책 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPolicyDTO {
    
    @Schema(description = "정책 ID")
    private Long id;
    
    @Schema(description = "정책 키")
    private String policyKey;
    
    @Schema(description = "정책 이름")
    private String policyName;
    
    @Schema(description = "정책 설명")
    private String description;
    
    @Schema(description = "테넌트 ID (tenant 엔티티 대신 ID만 포함)")
    private Long tenantId;
    
    @Schema(description = "상태")
    private Status status;
    
    @Schema(description = "정책 타입")
    private SecurityPolicy.PolicyType policyType;
    
    @Schema(description = "심각도")
    private SecurityPolicy.Severity severity;
    
    @Schema(description = "글로벌 정책 여부")
    private Boolean isGlobal;
    
    @Schema(description = "시스템 정책 여부")
    private Boolean isSystem;
    
    @Schema(description = "활성화 여부")
    private Boolean isEnabled;
    
    @Schema(description = "정책 규칙 (JSON)")
    private String rules;
    
    @Schema(description = "정책 조건 (JSON)")
    private String conditions;
    
    @Schema(description = "정책 액션 (JSON)")
    private String actions;
    
    @Schema(description = "대상 리소스 (JSON)")
    private String targetResources;
    
    @Schema(description = "예외 사항 (JSON)")
    private String exceptions;
    
    @Schema(description = "유효 시작일")
    private LocalDateTime effectiveFrom;
    
    @Schema(description = "유효 종료일")
    private LocalDateTime effectiveUntil;
    
    @Schema(description = "우선순위")
    private Integer priority;
    
    @Schema(description = "버전")
    private String version;
    
    @Schema(description = "메타데이터 (JSON)")
    private String metadata;
    
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "생성자")
    private String createdBy;
    
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
    
    @Schema(description = "수정자")
    private String updatedBy;
    
    @Schema(description = "삭제 여부")
    private Boolean isDeleted;
}

