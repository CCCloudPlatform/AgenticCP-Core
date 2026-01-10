package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlStreamManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

/**
 * AWS DynamoDB 스트림 관리 어댑터
 * 
 * NoSqlStreamManagementPort를 구현하여 DynamoDB Streams의
 * 활성화/비활성화 및 구성 변경 기능을 제공합니다.
 * 
 * DynamoDB Streams를 통해 테이블의 변경 이벤트를 캡처할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbStreamManagementAdapter implements NoSqlStreamManagementPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;
    private final CloudProviderRepository cloudProviderRepository;

    /**
     * DynamoDB 테이블의 스트림 구성을 변경합니다.
     * 
     * 스트림을 활성화하거나 비활성화할 수 있으며,
     * 스트림 뷰 타입(KEYS_ONLY, NEW_IMAGE, OLD_IMAGE, NEW_AND_OLD_IMAGES)을 설정할 수 있습니다.
     *
     * @param command 스트림 설정 변경을 포함하는 업데이트 명령
     * @return 변경 후 테이블 리소스 표현
     * @throws BusinessException 스트림 작업 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public CloudResource updateStreamConfiguration(NoSqlUpdateTableCommand command) {
        log.info("[AwsDynamoDbStreamManagementAdapter] Updating stream configuration for table: {}",
                command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 현재 테이블 정보 조회
            TableDescription currentTable = describeTable(client, command.getTableName());
            if (currentTable == null) {
                throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
            }

            // 스트림 설정 추출
            StreamSpecification streamSpec = buildStreamSpecification(command);

            if (streamSpec == null) {
                log.info("[AwsDynamoDbStreamManagementAdapter] No stream configuration changes for table: {}",
                        command.getTableName());
                return mapper.toCloudResource(currentTable, findAwsProvider());
            }

            // 테이블 업데이트 (스트림 설정 변경)
            UpdateTableRequest request = UpdateTableRequest.builder()
                    .tableName(command.getTableName())
                    .streamSpecification(streamSpec)
                    .build();

            UpdateTableResponse response = client.updateTable(request);
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbStreamManagementAdapter] Successfully updated stream configuration for table: {}, " +
                            "StreamEnabled: {}, StreamViewType: {}",
                    tableDescription.tableName(),
                    tableDescription.streamSpecification() != null 
                            ? tableDescription.streamSpecification().streamEnabled() : false,
                    tableDescription.streamSpecification() != null 
                            ? tableDescription.streamSpecification().streamViewTypeAsString() : "N/A");

            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbStreamManagementAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (ResourceInUseException e) {
            log.error("[AwsDynamoDbStreamManagementAdapter] Table is being updated: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_STREAM_OPERATION_FAILED,
                    "테이블이 업데이트 중입니다. 잠시 후 다시 시도해주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbStreamManagementAdapter] Failed to update stream configuration for table: {}",
                    command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * 스트림 설정 객체를 생성합니다.
     */
    private StreamSpecification buildStreamSpecification(NoSqlUpdateTableCommand command) {
        Map<String, Object> streamOptions = command.getStreamOptions();

        if (streamOptions == null || streamOptions.isEmpty()) {
            return null;
        }

        Boolean enabled = (Boolean) streamOptions.get("enabled");
        if (enabled == null) {
            return null;
        }

        if (!enabled) {
            // 스트림 비활성화
            return StreamSpecification.builder()
                    .streamEnabled(false)
                    .build();
        }

        // 스트림 활성화
        String viewType = (String) streamOptions.getOrDefault("viewType", "NEW_AND_OLD_IMAGES");
        StreamViewType streamViewType = mapStreamViewType(viewType);

        return StreamSpecification.builder()
                .streamEnabled(true)
                .streamViewType(streamViewType)
                .build();
    }

    /**
     * 도메인 스트림 뷰 타입을 DynamoDB StreamViewType으로 변환합니다.
     */
    private StreamViewType mapStreamViewType(String viewType) {
        if (viewType == null) {
            return StreamViewType.NEW_AND_OLD_IMAGES;
        }

        return switch (viewType.toUpperCase()) {
            case "KEYS_ONLY" -> StreamViewType.KEYS_ONLY;
            case "NEW_IMAGE" -> StreamViewType.NEW_IMAGE;
            case "OLD_IMAGE" -> StreamViewType.OLD_IMAGE;
            case "NEW_AND_OLD_IMAGES" -> StreamViewType.NEW_AND_OLD_IMAGES;
            default -> StreamViewType.NEW_AND_OLD_IMAGES;
        };
    }

    /**
     * 테이블 정보를 조회합니다.
     */
    private TableDescription describeTable(DynamoDbClient client, String tableName) {
        try {
            DescribeTableRequest request = mapper.toDescribeTableRequest(tableName);
            DescribeTableResponse response = client.describeTable(request);
            return response.table();
        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            return null;
        }
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    private CloudProvider findAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }
}

