package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Serverless Function 수정 요청 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionUpdateRequest {

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
     * 함수 ID/ARN (수정할 함수 식별자)
     */
    private String functionId;

    /**
     * 런타임 변경
     */
    private String runtime;

    /**
     * 핸들러 변경
     */
    private String handler;

    /**
     * 메모리 크기 변경 (MB)
     */
    @Min(value = 128, message = "메모리는 최소 128MB입니다")
    @Max(value = 10240, message = "메모리는 최대 10240MB입니다")
    private Integer memorySize;

    /**
     * 타임아웃 변경 (초)
     */
    @Min(value = 1, message = "타임아웃은 최소 1초입니다")
    @Max(value = 900, message = "타임아웃은 최대 900초입니다")
    private Integer timeout;

    /**
     * 실행 역할 변경
     */
    private String roleArn;

    /**
     * 환경 변수 변경
     */
    private Map<String, String> environmentVariables;

    /**
     * 설명 변경
     */
    private String description;

    /**
     * 코드 업데이트 URI
     */
    private String codeUri;

    /**
     * 추가할 태그
     */
    private Map<String, String> tagsToAdd;

    /**
     * 제거할 태그
     */
    private Map<String, String> tagsToRemove;

    /**
     * 테넌트 키
     */
    private String tenantKey;

    /**
     * CSP별 특화 설정
     */
    private Map<String, Object> providerSpecificConfig;
}
