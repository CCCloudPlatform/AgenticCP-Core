package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * NoSQL 테이블 삭제 요청 DTO
 *
 * CSP 중립적인 NoSQL 테이블 삭제 요청을 정의합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class NoSqlDeleteTableRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 대상 테이블 이름
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String tableName;

    /**
     * 리전/위치
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 강제 삭제 여부
     * true인 경우 백업 없이 즉시 삭제
     */
    @Builder.Default
    private boolean force = false;

    /**
     * 삭제 사유 (감사 로그용)
     */
    private String reason;
}

