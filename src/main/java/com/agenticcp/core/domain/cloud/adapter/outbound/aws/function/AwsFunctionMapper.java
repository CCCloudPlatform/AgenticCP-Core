package com.agenticcp.core.domain.cloud.adapter.outbound.aws.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionQuery;
import com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS Lambda Function 응답을 CloudResource로 변환하는 매퍼
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AwsFunctionMapper {

    private final ObjectMapper objectMapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final CloudRegionRepository cloudRegionRepository;

    public AwsFunctionMapper(
            ObjectMapper objectMapper,
            CloudProviderRepository cloudProviderRepository,
            CloudServiceRepository cloudServiceRepository,
            CloudRegionRepository cloudRegionRepository) {
        this.objectMapper = objectMapper;
        this.cloudProviderRepository = cloudProviderRepository;
        this.cloudServiceRepository = cloudServiceRepository;
        this.cloudRegionRepository = cloudRegionRepository;
    }

    /**
     * AWS FunctionConfiguration → CloudResource 변환 (생성 시)
     */
    public CloudResource toCloudResource(FunctionConfiguration functionConfig, FunctionCreateCommand command) {
        return buildCloudResource(functionConfig, command.providerType(), command.serviceKey(), command.region(), command.tags());
    }

    /**
     * AWS FunctionConfiguration → CloudResource 변환 (조회 시)
     */
    public CloudResource toCloudResource(FunctionConfiguration functionConfig, GetFunctionCommand command) {
        return buildCloudResource(functionConfig, command.providerType(), command.serviceKey(), command.region(), extractTags(functionConfig));
    }

    /**
     * AWS FunctionConfiguration → CloudResource 변환 (목록 조회 시)
     */
    public CloudResource toCloudResource(FunctionConfiguration functionConfig, FunctionQuery query, String region) {
        return buildCloudResource(functionConfig, query.providerType(), null, region, extractTags(functionConfig));
    }

    /**
     * CloudResource 빌드
     */
    private CloudResource buildCloudResource(
            FunctionConfiguration functionConfig,
            CloudProvider.ProviderType providerType,
            String serviceKey,
            String region,
            Map<String, String> tags) {

        String resourceId = functionConfig.functionArn();
        String resourceName = extractFunctionName(functionConfig, tags);

        // 엔티티 조회
        CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + providerType));

        String serviceKeyToUse = serviceKey != null ? serviceKey : "LAMBDA";
        CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKeyToUse)
                .orElseThrow(() -> new IllegalStateException("CloudService not found: providerType=" + providerType + ", serviceKey=" + serviceKeyToUse));

        CloudRegion cloudRegion = null;
        if (region != null && !region.isEmpty()) {
            cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, region)
                    .orElse(null);
            if (cloudRegion == null) {
                log.warn("[AwsFunctionMapper] CloudRegion not found for providerType: {}, regionKey: {}", providerType, region);
            }
        }

        // Configuration JSON 생성 (FunctionResponse에서 사용)
        String configurationJson = buildConfigurationJson(functionConfig);

        // Metadata JSON 생성
        String metadataJson = buildMetadataJson(functionConfig);

        // 메모리 GB 변환 (MB → GB)
        Integer memoryGb = functionConfig.memorySize() != null ? functionConfig.memorySize() / 1024 : null;

        return CloudResource.builder()
                .resourceId(resourceId)
                .resourceName(resourceName)
                .displayName(resourceName)
                .provider(provider)
                .service(service)
                .region(cloudRegion)
                .resourceType(CloudResource.ResourceType.FUNCTION)
                .lifecycleState(mapStateToLifecycleState(functionConfig.stateAsString()))
                .instanceType(functionConfig.runtime() != null ? functionConfig.runtime().toString() : null)  // 런타임을 instanceType에 저장
                .memoryGb(memoryGb)
                .tags(tags != null ? tags : extractTags(functionConfig))
                .configuration(configurationJson)
                .metadata(metadataJson)
                .createdInCloud(functionConfig.lastModified() != null ?
                        parseLastModified(functionConfig.lastModified()) : null)
                .lastModifiedInCloud(functionConfig.lastModified() != null ?
                        parseLastModified(functionConfig.lastModified()) : null)
                .lastSync(LocalDateTime.now())
                .build();
    }

    /**
     * Function 이름 추출 (태그의 Name 또는 FunctionName 사용)
     */
    private String extractFunctionName(FunctionConfiguration functionConfig, Map<String, String> tags) {
        if (tags != null && tags.containsKey("Name")) {
            return tags.get("Name");
        }
        return functionConfig.functionName();
    }

    /**
     * FunctionConfiguration에서 태그 추출
     * 실제로는 별도 API 호출이 필요하지만, 여기서는 빈 Map 반환
     * (실제 구현에서는 ListTags API 호출 필요)
     */
    private Map<String, String> extractTags(FunctionConfiguration functionConfig) {
        // AWS Lambda의 FunctionConfiguration에는 tags가 포함되지 않으므로
        // 별도로 ListTags API를 호출해야 합니다.
        // 여기서는 빈 Map을 반환하고, 실제 어댑터에서 태그를 조회하여 전달해야 합니다.
        return new HashMap<>();
    }

    /**
     * AWS Lambda의 lastModified String을 LocalDateTime으로 변환
     * AWS SDK는 ISO-8601 형식의 String을 반환함
     */
    private LocalDateTime parseLastModified(String lastModified) {
        if (lastModified == null || lastModified.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            TemporalAccessor temporalAccessor = formatter.parse(lastModified);
            Instant instant = Instant.from(temporalAccessor);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("[AwsFunctionMapper] Failed to parse lastModified: {}", lastModified, e);
            return null;
        }
    }

    /**
     * Configuration JSON 생성 (FunctionResponse에서 사용하는 정보 포함)
     */
    private String buildConfigurationJson(FunctionConfiguration functionConfig) {
        Map<String, Object> config = new HashMap<>();
        config.put("runtime", functionConfig.runtime() != null ? functionConfig.runtime().toString() : null);
        config.put("handler", functionConfig.handler());
        config.put("timeout", functionConfig.timeout());
        config.put("roleArn", functionConfig.role());
        config.put("memorySize", functionConfig.memorySize());

        if (functionConfig.environment() != null && functionConfig.environment().variables() != null) {
            config.put("environmentVariables", functionConfig.environment().variables());
        }

        if (functionConfig.description() != null) {
            config.put("description", functionConfig.description());
        }

        if (functionConfig.vpcConfig() != null && functionConfig.vpcConfig().vpcId() != null) {
            config.put("vpcId", functionConfig.vpcConfig().vpcId());
        }

        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.warn("[AwsFunctionMapper] Failed to serialize configuration JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Metadata JSON 생성
     */
    private String buildMetadataJson(FunctionConfiguration functionConfig) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("functionArn", functionConfig.functionArn());
        metadata.put("functionName", functionConfig.functionName());
        metadata.put("codeSize", functionConfig.codeSize());
        metadata.put("codeSha256", functionConfig.codeSha256());
        metadata.put("version", functionConfig.version());
        metadata.put("state", functionConfig.stateAsString());
        metadata.put("stateReason", functionConfig.stateReason());
        metadata.put("stateReasonCode", functionConfig.stateReasonCodeAsString());
        metadata.put("lastUpdateStatus", functionConfig.lastUpdateStatusAsString());
        metadata.put("lastUpdateStatusReason", functionConfig.lastUpdateStatusReason());
        metadata.put("lastUpdateStatusReasonCode", functionConfig.lastUpdateStatusReasonCodeAsString());
        metadata.put("packageType", functionConfig.packageTypeAsString());
        // architectures는 List<Architecture> enum이므로 String 리스트로 변환
        if (functionConfig.architectures() != null && !functionConfig.architectures().isEmpty()) {
            metadata.put("architectures", functionConfig.architectures().stream()
                    .map(arch -> arch.toString())
                    .collect(Collectors.toList()));
        }
        metadata.put("ephemeralStorageSize", functionConfig.ephemeralStorage() != null ? functionConfig.ephemeralStorage().size() : null);
        metadata.put("deadLetterQueueTargetArn", functionConfig.deadLetterConfig() != null ? functionConfig.deadLetterConfig().targetArn() : null);
        metadata.put("kmsKeyArn", functionConfig.kmsKeyArn());
        metadata.put("masterArn", functionConfig.masterArn());
        metadata.put("revisionId", functionConfig.revisionId());

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsFunctionMapper] Failed to serialize metadata JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AWS Lambda 상태 → CloudResource LifecycleState 변환
     */
    private CloudResource.LifecycleState mapStateToLifecycleState(String state) {
        if (state == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }

        return switch (state.toUpperCase()) {
            case "PENDING" -> CloudResource.LifecycleState.PENDING;
            case "ACTIVE" -> CloudResource.LifecycleState.RUNNING;
            case "INACTIVE" -> CloudResource.LifecycleState.STOPPED;
            case "FAILED" -> CloudResource.LifecycleState.FAILED;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }

    // ==================== Command → AWS SDK Request 변환 ====================

    /**
     * FunctionCreateCommand → CreateFunctionRequest 변환
     */
    public software.amazon.awssdk.services.lambda.model.CreateFunctionRequest toCreateFunctionRequest(FunctionCreateCommand command) {
        var builder = software.amazon.awssdk.services.lambda.model.CreateFunctionRequest.builder()
                .functionName(command.functionName())
                .runtime(software.amazon.awssdk.services.lambda.model.Runtime.fromValue(command.runtime()))
                .handler(command.handler())
                .role(command.roleArn());

        // 메모리 및 타임아웃
        if (command.memorySize() != null) {
            builder.memorySize(command.memorySize());
        }
        if (command.timeout() != null) {
            builder.timeout(command.timeout());
        }

        // 설명
        if (command.description() != null) {
            builder.description(command.description());
        }

        // 환경 변수
        if (command.environmentVariables() != null && !command.environmentVariables().isEmpty()) {
            builder.environment(software.amazon.awssdk.services.lambda.model.Environment.builder()
                    .variables(command.environmentVariables())
                    .build());
        }

        // VPC 설정
        if (command.vpcId() != null && command.providerSpecificConfig() != null) {
            var vpcConfig = buildVpcConfig(command);
            if (vpcConfig != null) {
                builder.vpcConfig(vpcConfig);
            }
        }

        // 코드 설정 (S3 또는 ZIP)
        var functionCode = buildFunctionCode(command);
        builder.code(functionCode);

        // 태그
        if (command.tags() != null && !command.tags().isEmpty()) {
            builder.tags(command.tags());
        }

        // Layers
        if (command.providerSpecificConfig() != null && command.providerSpecificConfig().containsKey("layers")) {
            @SuppressWarnings("unchecked")
            var layers = (java.util.List<String>) command.providerSpecificConfig().get("layers");
            if (layers != null && !layers.isEmpty()) {
                builder.layers(layers);
            }
        }

        // Dead Letter Queue
        if (command.providerSpecificConfig() != null && command.providerSpecificConfig().containsKey("deadLetterQueueTargetArn")) {
            String dlqArn = (String) command.providerSpecificConfig().get("deadLetterQueueTargetArn");
            if (dlqArn != null) {
                builder.deadLetterConfig(software.amazon.awssdk.services.lambda.model.DeadLetterConfig.builder()
                        .targetArn(dlqArn)
                        .build());
            }
        }

        return builder.build();
    }

    /**
     * VPC Config 빌드
     */
    private software.amazon.awssdk.services.lambda.model.VpcConfig buildVpcConfig(FunctionCreateCommand command) {
        if (command.providerSpecificConfig() == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        java.util.List<String> subnetIds = (java.util.List<String>) command.providerSpecificConfig().get("subnetIds");
        @SuppressWarnings("unchecked")
        java.util.List<String> securityGroupIds = (java.util.List<String>) command.providerSpecificConfig().get("securityGroupIds");

        if (subnetIds == null && securityGroupIds == null) {
            return null;
        }

        var vpcConfigBuilder = software.amazon.awssdk.services.lambda.model.VpcConfig.builder();
        if (subnetIds != null && !subnetIds.isEmpty()) {
            vpcConfigBuilder.subnetIds(subnetIds);
        }
        if (securityGroupIds != null && !securityGroupIds.isEmpty()) {
            vpcConfigBuilder.securityGroupIds(securityGroupIds);
        }

        return vpcConfigBuilder.build();
    }

    /**
     * FunctionCode 빌드 (S3 또는 ZIP)
     */
    private software.amazon.awssdk.services.lambda.model.FunctionCode buildFunctionCode(FunctionCreateCommand command) {
        if (command.codeZip() != null && command.codeZip().length > 0) {
            // ZIP 바이너리 직접 업로드
            return software.amazon.awssdk.services.lambda.model.FunctionCode.builder()
                    .zipFile(software.amazon.awssdk.core.SdkBytes.fromByteArray(command.codeZip()))
                    .build();
        } else if (command.codeUri() != null) {
            // S3 URI 파싱: s3://bucket/key 또는 bucket/key 형식
            S3CodeLocation s3Location = parseS3Uri(command.codeUri());
            return software.amazon.awssdk.services.lambda.model.FunctionCode.builder()
                    .s3Bucket(s3Location.bucket())
                    .s3Key(s3Location.key())
                    .build();
        } else {
            throw new IllegalArgumentException("codeUri 또는 codeZip이 필요합니다.");
        }
    }

    /**
     * S3 URI 파싱
     */
    public S3CodeLocation parseS3Uri(String codeUri) {
        String uri = codeUri.trim();
        
        // s3://bucket/key 형식
        if (uri.startsWith("s3://")) {
            String path = uri.substring(5);
            int slashIndex = path.indexOf('/');
            if (slashIndex == -1) {
                throw new IllegalArgumentException("S3 URI 형식이 올바르지 않습니다: " + codeUri);
            }
            String bucket = path.substring(0, slashIndex);
            String key = path.substring(slashIndex + 1);
            return new S3CodeLocation(bucket, key);
        }
        
        // bucket/key 형식
        int slashIndex = uri.indexOf('/');
        if (slashIndex == -1) {
            throw new IllegalArgumentException("S3 URI 형식이 올바르지 않습니다: " + codeUri);
        }
        String bucket = uri.substring(0, slashIndex);
        String key = uri.substring(slashIndex + 1);
        return new S3CodeLocation(bucket, key);
    }

    /**
     * S3 코드 위치
     */
    public record S3CodeLocation(String bucket, String key) {}
}
