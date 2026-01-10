package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * NoSQL 태그 관리 요청 DTO
 *
 * NoSQL 테이블의 태그 추가/수정/삭제 요청을 정의합니다.
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
public class NoSqlTagRequest {

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
     * 리전
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 추가/수정할 태그
     * - ADD_OR_UPDATE 작업 시 필수
     */
    private Map<String, String> tags;

    /**
     * 삭제할 태그 키 목록
     * - REMOVE 작업 시 필수
     */
    private List<String> tagKeysToRemove;

    /**
     * 작업 타입
     */
    @Builder.Default
    private OperationType operationType = OperationType.ADD_OR_UPDATE;

    /**
     * 태그 작업 타입
     */
    public enum OperationType {
        /**
         * 태그 추가 또는 업데이트 (기존 태그와 병합)
         */
        ADD_OR_UPDATE,
        
        /**
         * 지정된 태그 키 삭제
         */
        REMOVE,
        
        /**
         * 모든 태그를 지정된 태그로 교체
         */
        REPLACE
    }
}

