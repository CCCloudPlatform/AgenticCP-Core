package com.agenticcp.core.domain.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기능 플래그 승인 요청 DTO
 * 
 * 기능 플래그 변경에 대한 승인을 요청하기 위한 DTO입니다.
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
public class FeatureFlagApprovalRequest {

    /**
     * 기능 플래그 키
     */
    @NotBlank(message = "기능 플래그 키는 필수입니다")
    private String flagKey;

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
    @NotBlank(message = "변경 후 값은 필수입니다")
    private String newValue;
}

