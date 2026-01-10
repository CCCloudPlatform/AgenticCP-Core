package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlIndexManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AWS DynamoDB 인덱스 관리 어댑터
 * 
 * NoSqlIndexManagementPort를 구현하여 DynamoDB GSI(글로벌 보조 인덱스)의
 * 생성, 수정, 삭제 기능을 제공합니다.
 * 
 * DynamoDB에서 GSI 관리는 UpdateTable API를 통해 수행됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbIndexManagementAdapter implements NoSqlIndexManagementPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;
    private final CloudProviderRepository cloudProviderRepository;

    /**
     * DynamoDB 테이블의 GSI 구성을 변경합니다.
     * 
     * GSI 추가, 삭제, 용량 수정이 가능합니다.
     * 주의: DynamoDB는 한 번의 UpdateTable 호출에서 하나의 GSI만 추가/삭제할 수 있습니다.
     *
     * @param command 인덱스 변경을 포함하는 업데이트 명령
     * @return 변경 후 테이블 리소스 표현
     * @throws BusinessException 인덱스 작업 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public CloudResource updateIndexes(NoSqlUpdateTableCommand command) {
        log.info("[AwsDynamoDbIndexManagementAdapter] Updating indexes for table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 현재 테이블 정보 조회
            TableDescription currentTable = describeTable(client, command.getTableName());
            if (currentTable == null) {
                throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
            }

            // GSI 업데이트 요청 생성
            List<GlobalSecondaryIndexUpdate> gsiUpdates = buildGsiUpdates(command, currentTable);

            if (gsiUpdates.isEmpty()) {
                log.info("[AwsDynamoDbIndexManagementAdapter] No GSI updates to apply for table: {}",
                        command.getTableName());
                return mapper.toCloudResource(currentTable, findAwsProvider());
            }

            // 테이블 업데이트 (GSI 변경)
            UpdateTableRequest request = UpdateTableRequest.builder()
                    .tableName(command.getTableName())
                    .globalSecondaryIndexUpdates(gsiUpdates)
                    .build();

            UpdateTableResponse response = client.updateTable(request);
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbIndexManagementAdapter] Successfully updated indexes for table: {}",
                    tableDescription.tableName());

            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbIndexManagementAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (LimitExceededException e) {
            log.error("[AwsDynamoDbIndexManagementAdapter] GSI limit exceeded for table: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_INDEX_OPERATION_FAILED,
                    "GSI 개수 제한을 초과했습니다.");
        } catch (ResourceInUseException e) {
            log.error("[AwsDynamoDbIndexManagementAdapter] Table is being updated: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_INDEX_OPERATION_FAILED,
                    "테이블이 업데이트 중입니다. 잠시 후 다시 시도해주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbIndexManagementAdapter] Failed to update indexes for table: {}",
                    command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * GSI 업데이트 목록을 생성합니다.
     */
    private List<GlobalSecondaryIndexUpdate> buildGsiUpdates(NoSqlUpdateTableCommand command,
                                                              TableDescription currentTable) {
        List<GlobalSecondaryIndexUpdate> updates = new ArrayList<>();

        if (command.getGlobalSecondaryIndexes() == null || command.getGlobalSecondaryIndexes().isEmpty()) {
            return updates;
        }

        // 현재 GSI 이름 목록
        List<String> existingGsiNames = currentTable.globalSecondaryIndexes() != null
                ? currentTable.globalSecondaryIndexes().stream()
                    .map(GlobalSecondaryIndexDescription::indexName)
                    .toList()
                : List.of();

        for (Map<String, Object> gsiDef : command.getGlobalSecondaryIndexes()) {
            String indexName = (String) gsiDef.get("indexName");
            String action = (String) gsiDef.getOrDefault("action", "CREATE");

            switch (action.toUpperCase()) {
                case "CREATE" -> {
                    if (!existingGsiNames.contains(indexName)) {
                        updates.add(buildCreateGsiUpdate(gsiDef));
                    } else {
                        log.warn("[AwsDynamoDbIndexManagementAdapter] GSI already exists, skipping create: {}",
                                indexName);
                    }
                }
                case "DELETE" -> {
                    if (existingGsiNames.contains(indexName)) {
                        updates.add(GlobalSecondaryIndexUpdate.builder()
                                .delete(DeleteGlobalSecondaryIndexAction.builder()
                                        .indexName(indexName)
                                        .build())
                                .build());
                    } else {
                        log.warn("[AwsDynamoDbIndexManagementAdapter] GSI not found, skipping delete: {}",
                                indexName);
                    }
                }
                case "UPDATE" -> {
                    if (existingGsiNames.contains(indexName)) {
                        Long readCapacity = getLongValue(gsiDef.get("readCapacityUnits"));
                        Long writeCapacity = getLongValue(gsiDef.get("writeCapacityUnits"));

                        if (readCapacity != null || writeCapacity != null) {
                            updates.add(GlobalSecondaryIndexUpdate.builder()
                                    .update(UpdateGlobalSecondaryIndexAction.builder()
                                            .indexName(indexName)
                                            .provisionedThroughput(ProvisionedThroughput.builder()
                                                    .readCapacityUnits(readCapacity != null ? readCapacity : 5L)
                                                    .writeCapacityUnits(writeCapacity != null ? writeCapacity : 5L)
                                                    .build())
                                            .build())
                                    .build());
                        }
                    } else {
                        log.warn("[AwsDynamoDbIndexManagementAdapter] GSI not found, skipping update: {}",
                                indexName);
                    }
                }
                default -> log.warn("[AwsDynamoDbIndexManagementAdapter] Unknown action: {}", action);
            }
        }

        return updates;
    }

    /**
     * GSI 생성 업데이트 객체를 생성합니다.
     */
    @SuppressWarnings("unchecked")
    private GlobalSecondaryIndexUpdate buildCreateGsiUpdate(Map<String, Object> gsiDef) {
        String indexName = (String) gsiDef.get("indexName");
        String partitionKey = (String) gsiDef.get("partitionKey");
        String sortKey = (String) gsiDef.get("sortKey");
        String projectionType = (String) gsiDef.getOrDefault("projectionType", "ALL");

        // 키 스키마
        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(KeySchemaElement.builder()
                .attributeName(partitionKey)
                .keyType(KeyType.HASH)
                .build());
        if (sortKey != null) {
            keySchema.add(KeySchemaElement.builder()
                    .attributeName(sortKey)
                    .keyType(KeyType.RANGE)
                    .build());
        }

        // 프로젝션
        Projection.Builder projectionBuilder = Projection.builder()
                .projectionType(ProjectionType.fromValue(projectionType));
        if ("INCLUDE".equals(projectionType)) {
            List<String> nonKeyAttributes = (List<String>) gsiDef.get("nonKeyAttributes");
            if (nonKeyAttributes != null) {
                projectionBuilder.nonKeyAttributes(nonKeyAttributes);
            }
        }

        // 프로비저닝된 용량
        Long readCapacity = getLongValue(gsiDef.get("readCapacityUnits"));
        Long writeCapacity = getLongValue(gsiDef.get("writeCapacityUnits"));

        CreateGlobalSecondaryIndexAction.Builder createBuilder = CreateGlobalSecondaryIndexAction.builder()
                .indexName(indexName)
                .keySchema(keySchema)
                .projection(projectionBuilder.build());

        if (readCapacity != null || writeCapacity != null) {
            createBuilder.provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(readCapacity != null ? readCapacity : 5L)
                    .writeCapacityUnits(writeCapacity != null ? writeCapacity : 5L)
                    .build());
        }

        return GlobalSecondaryIndexUpdate.builder()
                .create(createBuilder.build())
                .build();
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

    /**
     * Object를 Long으로 안전하게 변환합니다.
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
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

