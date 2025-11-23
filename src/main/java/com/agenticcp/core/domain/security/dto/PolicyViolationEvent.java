package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 정책 위반 이벤트 DTO
 * 
 * <p>정책 위반이 감지되었을 때 발행되는 이벤트 데이터입니다.</p>
 * <p>Spring Events를 통해 비동기 처리됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Schema(description = "정책 위반 이벤트")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyViolationEvent {

    @Schema(description = "이벤트 ID (UUID)")
    private String eventId;

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

    @Schema(description = "위반 상세 정보")
    private Map<String, Object> violationDetails;

    @Schema(description = "컨텍스트 정보")
    private Map<String, Object> context;

    @Schema(description = "자동 대응 필요 여부")
    private Boolean requiresAutoResponse;

    @Schema(description = "알림 필요 여부")
    private Boolean requiresNotification;
}

