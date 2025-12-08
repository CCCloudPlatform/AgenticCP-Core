package com.agenticcp.core.domain.platform.dto;

import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기능 플래그 승인 응답 DTO
 * 
 * 기능 플래그 승인 정보를 반환하는 응답 DTO입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureFlagApprovalResponse {

    /**
     * 승인 ID
     */
    private Long id;

    /**
     * 기능 플래그 ID
     */
    private Long featureFlagId;

    /**
     * 기능 플래그 키
     */
    private String flagKey;

    /**
     * 승인 상태
     */
    private ApprovalStatus status;

    /**
     * 요청자 ID
     */
    private String requestedBy;

    /**
     * 요청 사유
     */
    private String requestReason;

    /**
     * 변경 전 값 (JSON 형태)
     */
    private String oldValue;

    /**
     * 변경 후 값 (JSON 형태)
     */
    private String newValue;

    /**
     * 요청 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime requestedAt;

    /**
     * 승인자 ID
     */
    private String approvedBy;

    /**
     * 승인 사유
     */
    private String approvalReason;

    /**
     * 승인 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime approvedAt;

    /**
     * 거부자 ID
     */
    private String rejectedBy;

    /**
     * 거부 사유
     */
    private String rejectionReason;

    /**
     * 거부 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime rejectedAt;

    /**
     * 생성일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime updatedAt;
}

