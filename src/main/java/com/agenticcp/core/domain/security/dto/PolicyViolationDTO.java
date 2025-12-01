package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 정책 위반 DTO
 * 
 * <p>PolicyViolation 엔티티의 데이터 전송 객체입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Schema(description = "정책 위반 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyViolationDTO {

    @Schema(description = "위반 ID")
    private Long id;

    @Schema(description = "테넌트 ID")
    private Long tenantId;

    @Schema(description = "정책 ID")
    private Long policyId;

    @Schema(description = "정책 이름")
    private String policyName;

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "사용자명")
    private String username;

    @Schema(description = "IP 주소")
    private String ipAddress;

    @Schema(description = "User Agent")
    private String userAgent;

    @Schema(description = "위반 타입")
    private PolicyViolation.ViolationType violationType;

    @Schema(description = "심각도")
    private SecurityPolicy.Severity severity;

    @Schema(description = "위반 설명")
    private String description;

    @Schema(description = "감지 시간")
    private LocalDateTime detectedAt;

    @Schema(description = "리소스 타입")
    private String resourceType;

    @Schema(description = "리소스 ID")
    private String resourceId;

    @Schema(description = "시도한 액션")
    private String actionAttempted;

    @Schema(description = "위반 상세 정보 (JSON)")
    private String violationDetails;

    @Schema(description = "처리 상태")
    private PolicyViolation.ViolationStatus status;

    @Schema(description = "자동 대응 실행 여부")
    private Boolean autoResponseExecuted;

    @Schema(description = "대응 액션 (JSON)")
    private String responseAction;

    @Schema(description = "대응 실행 시간")
    private LocalDateTime responseExecutedAt;

    @Schema(description = "알림 발송 여부")
    private Boolean notificationSent;

    @Schema(description = "알림 발송 시간")
    private LocalDateTime notificationSentAt;

    @Schema(description = "해결 시간")
    private LocalDateTime resolvedAt;

    @Schema(description = "해결자")
    private String resolvedBy;

    @Schema(description = "해결 노트")
    private String resolutionNotes;

    @Schema(description = "오탐지 여부")
    private Boolean falsePositive;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간")
    private LocalDateTime updatedAt;
}

