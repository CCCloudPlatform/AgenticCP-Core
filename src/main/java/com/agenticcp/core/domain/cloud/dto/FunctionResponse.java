package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Serverless Function 응답 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResponse {

    private Long id;
    private String resourceId;
    private String resourceName;  // function name
    private ProviderType providerType;
    private String region;
    private String runtime;  // 런타임
    private String handler;  // 핸들러
    private Integer memorySize;  // MB
    private Integer timeout;  // 초
    private String roleArn;  // 실행 역할
    private Map<String, String> environmentVariables;  // 환경 변수
    private String description;  // 함수 설명
    private String vpcId;  // VPC ID
    private String status;  // 상태
    private String lifecycleState;  // 생명주기 상태
    private String lastModified;  // 마지막 수정 시간
    private Map<String, String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastSync;

    /**
     * CloudResource → FunctionResponse 변환
     */
    public static FunctionResponse from(CloudResource resource) {
        if (resource == null) {
            return null;
        }

        // CloudResource의 configuration JSON에서 추가 정보 추출
        FunctionConfigurationInfo configInfo = parseConfiguration(resource.getConfiguration());

        return FunctionResponse.builder()
                .id(resource.getId())
                .resourceId(resource.getResourceId())
                .resourceName(resource.getResourceName())
                .providerType(resource.getProvider() != null ? resource.getProvider().getProviderType() : null)
                .region(resource.getRegion() != null ? resource.getRegion().getRegionKey() : null)
                .runtime(configInfo.runtime != null ? configInfo.runtime : resource.getInstanceType())  // instanceType에 runtime 저장 가능
                .handler(configInfo.handler)
                .memorySize(resource.getMemoryGb() != null ? resource.getMemoryGb() * 1024 : null)  // GB → MB
                .timeout(configInfo.timeout)
                .roleArn(configInfo.roleArn)
                .environmentVariables(configInfo.environmentVariables)
                .description(configInfo.description)
                .vpcId(configInfo.vpcId)
                .status(resource.getStatus() != null ? resource.getStatus().name() : null)
                .lifecycleState(resource.getLifecycleState() != null ? resource.getLifecycleState().name() : null)
                .lastModified(resource.getLastModifiedInCloud() != null ? resource.getLastModifiedInCloud().toString() : null)
                .tags(resource.getTags())
                .createdAt(resource.getCreatedAt())
                .lastSync(resource.getLastSync())
                .build();
    }

    /**
     * configuration JSON 파싱
     */
    private static FunctionConfigurationInfo parseConfiguration(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return new FunctionConfigurationInfo(null, null, null, null, null, null, null, null);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(configuration, new TypeReference<Map<String, Object>>() {});

            return new FunctionConfigurationInfo(
                    (String) config.get("runtime"),
                    (String) config.get("handler"),
                    config.get("timeout") != null ? ((Number) config.get("timeout")).intValue() : null,
                    (String) config.get("roleArn"),
                    (Map<String, String>) config.get("environmentVariables"),
                    (String) config.get("description"),
                    (String) config.get("vpcId"),
                    null
            );
        } catch (Exception e) {
            log.warn("[FunctionResponse] Failed to parse configuration JSON: {}", configuration, e);
            return new FunctionConfigurationInfo(null, null, null, null, null, null, null, null);
        }
    }

    /**
     * configuration 정보 임시 저장용 레코드
     */
    private record FunctionConfigurationInfo(
            String runtime,
            String handler,
            Integer timeout,
            String roleArn,
            Map<String, String> environmentVariables,
            String description,
            String vpcId,
            String lastModified
    ) {}
}
