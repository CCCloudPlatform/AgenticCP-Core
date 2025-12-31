package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.Set;

/**
 * RDBMS 조회 요청 DTO (CSP 중립적)
 * 
 * RDBMS 인스턴스 목록 조회를 위한 요청 객체입니다.
 * CSP 중립적인 필터링 조건을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdbmsQueryRequest {
    
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
     * 조회할 리전 목록
     * null이면 모든 리전에서 조회 (일부 CSP만 지원)
     */
    private Set<String> regions;
    
    /**
     * 인스턴스 이름으로 필터링 (CSP 중립적)
     */
    private String instanceName;
    
    /**
     * 엔진 타입으로 필터링
     * 예: mysql, postgresql, mariadb, oracle, sqlserver
     */
    private String engine;
    
    /**
     * 인스턴스 크기로 필터링 (CSP 중립적)
     */
    private String instanceSize;
    
    /**
     * 상태로 필터링
     * CSP별 상태 값이 다를 수 있음
     * - AWS: available, creating, deleting, modifying 등
     * - Azure: Ready, Creating, Deleting 등
     * - GCP: RUNNABLE, CREATING, DELETING 등
     */
    private String status;
    
    /**
     * 태그로 필터링
     * 키-값 쌍으로 필터링
     */
    private Map<String, String> tags;
    
    /**
     * 페이지 번호 (0부터 시작)
     */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    @Builder.Default
    private int page = 0;
    
    /**
     * 페이지 크기
     */
    @Min(value = 1, message = "페이지 크기는 최소 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 최대 100 이하여야 합니다")
    @Builder.Default
    private int size = 20;
}
