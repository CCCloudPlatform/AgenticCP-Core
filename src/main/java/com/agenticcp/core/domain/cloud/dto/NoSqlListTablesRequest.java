package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.Set;

/**
 * NoSQL 테이블 목록 조회 요청 DTO
 *
 * NoSQL 테이블 목록 조회 시 필터링 및 페이징 조건을 정의합니다.
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
public class NoSqlListTablesRequest {

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
     * 조회할 리전 목록 (비어있으면 모든 리전)
     */
    private Set<String> regions;

    /**
     * 테이블 이름 검색 필터 (contains 검색)
     */
    private String nameContains;

    /**
     * 태그 기반 필터 (key=value 일치)
     */
    private Map<String, String> tagsEquals;

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Builder.Default
    private int page = 0;

    /**
     * 페이지 크기
     */
    @Builder.Default
    private int size = 20;

    /**
     * 정렬 필드
     */
    private String sortBy;

    /**
     * 정렬 방향 (asc/desc)
     */
    @Builder.Default
    private String sortDirection = "asc";
}

