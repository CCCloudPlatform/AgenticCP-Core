package com.agenticcp.core.domain.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 연결 테스트 요청 DTO
 * 
 * 등록된 클라우드 계정의 연결 상태를 실시간으로 테스트합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "연결 테스트 요청")
public class ConnectionTestRequest {
    
    @NotNull(message = "계정 ID는 필수입니다")
    @Schema(description = "테스트할 클라우드 계정 ID", example = "1", required = true)
    private Long accountId;
}

