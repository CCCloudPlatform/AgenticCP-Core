package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Serverless Function 삭제 요청 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDeleteRequest {

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
     * 함수 ID/ARN (삭제할 함수 식별자)
     */
    private String functionId;

    /**
     * 리전
     */
    private String region;

    /**
     * 삭제 이유 (감사 로그용)
     */
    private String reason;

    /**
     * 테넌트 키
     */
    private String tenantKey;

    /**
     * CSP별 특화 삭제 옵션
     */
    private Map<String, Object> providerSpecificConfig;

    /**
     * 기본 삭제 요청 생성
     */
    public static FunctionDeleteRequest basic(String functionId) {
        return FunctionDeleteRequest.builder()
                .functionId(functionId)
                .build();
    }
}
