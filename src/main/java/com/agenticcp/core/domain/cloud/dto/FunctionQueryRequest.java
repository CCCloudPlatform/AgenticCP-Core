package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * Serverless Function 조회 요청 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionQueryRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 조회할 리전 목록
     */
    private Set<String> regions;

    /**
     * 함수 이름으로 필터링
     */
    private String functionName;

    /**
     * 런타임으로 필터링
     */
    private String runtime;

    /**
     * VPC ID로 필터링
     */
    private String vpcId;

    /**
     * 태그로 필터링
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
    @Min(value = 1, message = "페이지 크기는 최소 1입니다")
    @Max(value = 100, message = "페이지 크기는 최대 100입니다")
    @Builder.Default
    private int size = 20;
}
