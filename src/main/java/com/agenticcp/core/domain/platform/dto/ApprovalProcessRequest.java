package com.agenticcp.core.domain.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 승인 프로세스 요청 DTO
 * 
 * 승인 또는 거부 처리를 위한 요청 DTO입니다.
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
public class ApprovalProcessRequest {

    /**
     * 승인/거부 사유
     */
    @NotBlank(message = "승인/거부 사유는 필수입니다")
    private String reason;
}

