package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlCreateTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlDeleteTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS DynamoDB 테이블과 도메인 모델 간의 데이터 변환을 담당하는 매퍼
 * 
 * 이 매퍼는 AWS SDK의 DynamoDB 객체를 우리 도메인의 CloudResource로 변환하고,
 * CSP 중립적인 도메인 모델을 AWS SDK 요청 객체로 변환하는 역할을 합니다.
 * 
 * CSP별 차이를 흡수하는 핵심 컴포넌트입니다:
 * - 도메인 모델(CSP 중립) → AWS SDK 요청(CSP 특화)
 * - AWS SDK 응답(CSP 특화) → CloudResource(Canonical 모델)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
public class AwsDynamoDbMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== TableDescription → CloudResource 변환 ====================

    /**
     * AWS DynamoDB TableDescription을 CloudResource로 변환합니다.
     * 
     * @param tableDescription AWS SDK TableDescription 객체
     * @param provider CloudProvider 엔티티
     * @return CloudResource 도메인 객체
     */
    public CloudResource toCloudResource(TableDescription tableDescription, CloudProvider provider) {
        try {
            log.debug("[AwsDynamoDbMapper] Converting TableDescription to CloudResource: {}", 
                    tableDescription.tableName());

            return CloudResource.builder()
                    .resourceId(tableDescription.tableArn())
                    .resourceName(tableDescription.tableName())
                    .displayName(tableDescription.tableName())
                    .provider(provider)
                    .resourceType(CloudResource.ResourceType.DATABASE)
                    .lifecycleState(mapTableStatus(tableDescription.tableStatusAsString()))
                    .instanceType("DYNAMODB_TABLE")
                    .instanceSize(mapBillingMode(tableDescription.billingModeSummary()))
                    .storageGb(tableDescription.tableSizeBytes() != null 
                            ? tableDescription.tableSizeBytes() / (1024 * 1024 * 1024) : 0L)
                    .tags(new HashMap<>()) // 태그는 별도 API 호출 필요
                    .configuration(buildConfigurationJson(tableDescription))
                    .metadata(buildMetadata(tableDescription))
                    .createdInCloud(toLocalDateTime(tableDescription.creationDateTime()))
                    .lastModifiedInCloud(toLocalDateTime(tableDescription.creationDateTime()))
                    .lastSync(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("[AwsDynamoDbMapper] Failed to convert TableDescription to CloudResource: {}", 
                    tableDescription.tableName(), e);
            throw new BusinessException(CloudErrorCode.MAPPING_FAILED,
                    "DynamoDB 테이블을 CloudResource로 변환하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ==================== NoSqlCreateTableCommand → CreateTableRequest 변환 ====================

    /**
     * NoSqlCreateTableCommand(CSP 중립)를 AWS CreateTableRequest(AWS 특화)로 변환합니다.
     * 
     * @param command CSP 중립적인 도메인 생성 커맨드
     * @return AWS SDK CreateTableRequest 객체
     */
    public CreateTableRequest toCreateTableRequest(NoSqlCreateTableCommand command) {
        log.debug("[AwsDynamoDbMapper] Converting NoSqlCreateTableCommand to CreateTableRequest: tableName={}", 
                command.getTableName());

        CreateTableRequest.Builder builder = CreateTableRequest.builder()
                .tableName(command.getTableName())
                .keySchema(buildKeySchema(command))
                .attributeDefinitions(buildAttributeDefinitions(command));

        // 빌링 모드 설정
        if (command.getBillingMode() == NoSqlCreateTableCommand.BillingMode.PAY_PER_REQUEST) {
            builder.billingMode(BillingMode.PAY_PER_REQUEST);
        } else {
            builder.billingMode(BillingMode.PROVISIONED);
            builder.provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(command.getReadCapacityUnits() != null ? command.getReadCapacityUnits() : 5L)
                    .writeCapacityUnits(command.getWriteCapacityUnits() != null ? command.getWriteCapacityUnits() : 5L)
                    .build());
        }

        // GSI 설정
        List<GlobalSecondaryIndex> gsiList = buildGlobalSecondaryIndexes(command);
        if (!gsiList.isEmpty()) {
            builder.globalSecondaryIndexes(gsiList);
        }

        // 스트림 설정
        StreamSpecification streamSpec = buildStreamSpecification(command);
        if (streamSpec != null) {
            builder.streamSpecification(streamSpec);
        }

        // 태그 설정
        if (command.getTags() != null && !command.getTags().isEmpty()) {
            builder.tags(buildTags(command.getTags()));
        }

        return builder.build();
    }

    // ==================== NoSqlUpdateTableCommand → UpdateTableRequest 변환 ====================

    /**
     * NoSqlUpdateTableCommand(CSP 중립)를 AWS UpdateTableRequest(AWS 특화)로 변환합니다.
     * 
     * @param command CSP 중립적인 도메인 업데이트 커맨드
     * @return AWS SDK UpdateTableRequest 객체
     */
    public UpdateTableRequest toUpdateTableRequest(NoSqlUpdateTableCommand command) {
        log.debug("[AwsDynamoDbMapper] Converting NoSqlUpdateTableCommand to UpdateTableRequest: tableName={}", 
                command.getTableName());

        UpdateTableRequest.Builder builder = UpdateTableRequest.builder()
                .tableName(command.getTableName());

        // 빌링 모드 변경
        if (command.getBillingMode() != null) {
            if (command.getBillingMode() == NoSqlCreateTableCommand.BillingMode.PAY_PER_REQUEST) {
                builder.billingMode(BillingMode.PAY_PER_REQUEST);
            } else {
                builder.billingMode(BillingMode.PROVISIONED);
                builder.provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(command.getReadCapacityUnits() != null ? command.getReadCapacityUnits() : 5L)
                        .writeCapacityUnits(command.getWriteCapacityUnits() != null ? command.getWriteCapacityUnits() : 5L)
                        .build());
            }
        } else if (command.getReadCapacityUnits() != null || command.getWriteCapacityUnits() != null) {
            // 용량만 변경하는 경우
            builder.provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(command.getReadCapacityUnits() != null ? command.getReadCapacityUnits() : 5L)
                    .writeCapacityUnits(command.getWriteCapacityUnits() != null ? command.getWriteCapacityUnits() : 5L)
                    .build());
        }

        // 스트림 설정 변경
        StreamSpecification streamSpec = buildStreamSpecificationFromUpdate(command);
        if (streamSpec != null) {
            builder.streamSpecification(streamSpec);
        }

        return builder.build();
    }

    // ==================== NoSqlDeleteTableCommand → DeleteTableRequest 변환 ====================

    /**
     * NoSqlDeleteTableCommand(CSP 중립)를 AWS DeleteTableRequest(AWS 특화)로 변환합니다.
     * 
     * @param command CSP 중립적인 도메인 삭제 커맨드
     * @return AWS SDK DeleteTableRequest 객체
     */
    public DeleteTableRequest toDeleteTableRequest(NoSqlDeleteTableCommand command) {
        log.debug("[AwsDynamoDbMapper] Converting NoSqlDeleteTableCommand to DeleteTableRequest: tableName={}", 
                command.getTableName());

        return DeleteTableRequest.builder()
                .tableName(command.getTableName())
                .build();
    }

    // ==================== DescribeTableRequest 생성 ====================

    /**
     * 테이블 이름으로 DescribeTableRequest를 생성합니다.
     * 
     * @param tableName 테이블 이름
     * @return AWS SDK DescribeTableRequest 객체
     */
    public DescribeTableRequest toDescribeTableRequest(String tableName) {
        return DescribeTableRequest.builder()
                .tableName(tableName)
                .build();
    }

    /**
     * ListTablesRequest를 생성합니다.
     * 
     * @param exclusiveStartTableName 시작 테이블 이름 (페이지네이션용, null 가능)
     * @param limit 조회 제한 수 (null이면 기본값 사용)
     * @return AWS SDK ListTablesRequest 객체
     */
    public ListTablesRequest toListTablesRequest(String exclusiveStartTableName, Integer limit) {
        ListTablesRequest.Builder builder = ListTablesRequest.builder();
        
        if (exclusiveStartTableName != null) {
            builder.exclusiveStartTableName(exclusiveStartTableName);
        }
        if (limit != null) {
            builder.limit(limit);
        }
        
        return builder.build();
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * DynamoDB 테이블 상태를 도메인 LifecycleState로 매핑합니다.
     */
    private CloudResource.LifecycleState mapTableStatus(String tableStatus) {
        if (tableStatus == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }
        
        return switch (tableStatus.toUpperCase()) {
            case "ACTIVE" -> CloudResource.LifecycleState.RUNNING;
            case "CREATING" -> CloudResource.LifecycleState.PENDING;
            case "UPDATING" -> CloudResource.LifecycleState.RUNNING;
            case "DELETING" -> CloudResource.LifecycleState.TERMINATING;
            case "ARCHIVED", "INACCESSIBLE_ENCRYPTION_CREDENTIALS" -> CloudResource.LifecycleState.STOPPED;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }

    /**
     * 빌링 모드 요약 정보를 문자열로 변환합니다.
     */
    private String mapBillingMode(BillingModeSummary billingModeSummary) {
        if (billingModeSummary == null || billingModeSummary.billingMode() == null) {
            return "PROVISIONED";
        }
        return billingModeSummary.billingModeAsString();
    }

    /**
     * NoSqlCreateTableCommand에서 KeySchema를 생성합니다.
     */
    private List<KeySchemaElement> buildKeySchema(NoSqlCreateTableCommand command) {
        List<KeySchemaElement> keySchema = new ArrayList<>();

        // 파티션 키 (필수)
        keySchema.add(KeySchemaElement.builder()
                .attributeName(command.getPartitionKeyName())
                .keyType(KeyType.HASH)
                .build());

        // 정렬 키 (선택)
        if (command.getSortKeyName() != null && !command.getSortKeyName().isEmpty()) {
            keySchema.add(KeySchemaElement.builder()
                    .attributeName(command.getSortKeyName())
                    .keyType(KeyType.RANGE)
                    .build());
        }

        return keySchema;
    }

    /**
     * NoSqlCreateTableCommand에서 AttributeDefinitions를 생성합니다.
     */
    private List<AttributeDefinition> buildAttributeDefinitions(NoSqlCreateTableCommand command) {
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        Map<String, String> keyTypes = command.getKeyTypes() != null ? command.getKeyTypes() : new HashMap<>();

        // 파티션 키
        String pkType = keyTypes.getOrDefault(command.getPartitionKeyName(), "STRING");
        attributeDefinitions.add(AttributeDefinition.builder()
                .attributeName(command.getPartitionKeyName())
                .attributeType(mapScalarAttributeType(pkType))
                .build());

        // 정렬 키
        if (command.getSortKeyName() != null && !command.getSortKeyName().isEmpty()) {
            String skType = keyTypes.getOrDefault(command.getSortKeyName(), "STRING");
            attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName(command.getSortKeyName())
                    .attributeType(mapScalarAttributeType(skType))
                    .build());
        }

        // GSI 키들 추가
        if (command.getGlobalSecondaryIndexes() != null) {
            for (Map<String, Object> gsi : command.getGlobalSecondaryIndexes()) {
                String gsiPartitionKey = (String) gsi.get("partitionKey");
                String gsiSortKey = (String) gsi.get("sortKey");

                if (gsiPartitionKey != null && !isKeyAlreadyDefined(attributeDefinitions, gsiPartitionKey)) {
                    String gsiPkType = keyTypes.getOrDefault(gsiPartitionKey, "STRING");
                    attributeDefinitions.add(AttributeDefinition.builder()
                            .attributeName(gsiPartitionKey)
                            .attributeType(mapScalarAttributeType(gsiPkType))
                            .build());
                }

                if (gsiSortKey != null && !isKeyAlreadyDefined(attributeDefinitions, gsiSortKey)) {
                    String gsiSkType = keyTypes.getOrDefault(gsiSortKey, "STRING");
                    attributeDefinitions.add(AttributeDefinition.builder()
                            .attributeName(gsiSortKey)
                            .attributeType(mapScalarAttributeType(gsiSkType))
                            .build());
                }
            }
        }

        return attributeDefinitions;
    }

    /**
     * 도메인 키 타입을 DynamoDB ScalarAttributeType으로 변환합니다.
     */
    private ScalarAttributeType mapScalarAttributeType(String type) {
        if (type == null) {
            return ScalarAttributeType.S;
        }
        
        return switch (type.toUpperCase()) {
            case "STRING", "S" -> ScalarAttributeType.S;
            case "NUMBER", "N" -> ScalarAttributeType.N;
            case "BINARY", "B" -> ScalarAttributeType.B;
            default -> ScalarAttributeType.S;
        };
    }

    /**
     * 속성 정의 목록에 이미 해당 키가 정의되어 있는지 확인합니다.
     */
    private boolean isKeyAlreadyDefined(List<AttributeDefinition> attributeDefinitions, String keyName) {
        return attributeDefinitions.stream()
                .anyMatch(attr -> attr.attributeName().equals(keyName));
    }

    /**
     * NoSqlCreateTableCommand에서 GlobalSecondaryIndexes를 생성합니다.
     */
    private List<GlobalSecondaryIndex> buildGlobalSecondaryIndexes(NoSqlCreateTableCommand command) {
        if (command.getGlobalSecondaryIndexes() == null || command.getGlobalSecondaryIndexes().isEmpty()) {
            return new ArrayList<>();
        }

        return command.getGlobalSecondaryIndexes().stream()
                .map(this::buildGlobalSecondaryIndex)
                .collect(Collectors.toList());
    }

    /**
     * 단일 GSI 정의를 DynamoDB GlobalSecondaryIndex로 변환합니다.
     */
    @SuppressWarnings("unchecked")
    private GlobalSecondaryIndex buildGlobalSecondaryIndex(Map<String, Object> gsiDef) {
        String indexName = (String) gsiDef.get("indexName");
        String partitionKey = (String) gsiDef.get("partitionKey");
        String sortKey = (String) gsiDef.get("sortKey");
        String projectionType = (String) gsiDef.getOrDefault("projectionType", "ALL");

        GlobalSecondaryIndex.Builder builder = GlobalSecondaryIndex.builder()
                .indexName(indexName);

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
        builder.keySchema(keySchema);

        // 프로젝션
        Projection.Builder projectionBuilder = Projection.builder()
                .projectionType(ProjectionType.fromValue(projectionType));
        if ("INCLUDE".equals(projectionType)) {
            List<String> nonKeyAttributes = (List<String>) gsiDef.get("nonKeyAttributes");
            if (nonKeyAttributes != null) {
                projectionBuilder.nonKeyAttributes(nonKeyAttributes);
            }
        }
        builder.projection(projectionBuilder.build());

        // 프로비저닝된 용량 (온디맨드가 아닌 경우)
        Long readCapacity = getLongValue(gsiDef.get("readCapacityUnits"));
        Long writeCapacity = getLongValue(gsiDef.get("writeCapacityUnits"));
        if (readCapacity != null || writeCapacity != null) {
            builder.provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(readCapacity != null ? readCapacity : 5L)
                    .writeCapacityUnits(writeCapacity != null ? writeCapacity : 5L)
                    .build());
        }

        return builder.build();
    }

    /**
     * Object를 Long으로 안전하게 변환합니다.
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * NoSqlCreateTableCommand에서 StreamSpecification을 생성합니다.
     */
    private StreamSpecification buildStreamSpecification(NoSqlCreateTableCommand command) {
        if (command.getStreamOptions() == null || command.getStreamOptions().isEmpty()) {
            return null;
        }

        Boolean enabled = (Boolean) command.getStreamOptions().get("enabled");
        if (enabled == null || !enabled) {
            return null;
        }

        String viewType = (String) command.getStreamOptions().getOrDefault("viewType", "NEW_AND_OLD_IMAGES");
        
        return StreamSpecification.builder()
                .streamEnabled(true)
                .streamViewType(mapStreamViewType(viewType))
                .build();
    }

    /**
     * NoSqlUpdateTableCommand에서 StreamSpecification을 생성합니다.
     */
    private StreamSpecification buildStreamSpecificationFromUpdate(NoSqlUpdateTableCommand command) {
        if (command.getStreamOptions() == null || command.getStreamOptions().isEmpty()) {
            return null;
        }

        Boolean enabled = (Boolean) command.getStreamOptions().get("enabled");
        if (enabled == null) {
            return null;
        }

        if (!enabled) {
            return StreamSpecification.builder()
                    .streamEnabled(false)
                    .build();
        }

        String viewType = (String) command.getStreamOptions().getOrDefault("viewType", "NEW_AND_OLD_IMAGES");
        
        return StreamSpecification.builder()
                .streamEnabled(true)
                .streamViewType(mapStreamViewType(viewType))
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
     * 태그 맵을 DynamoDB Tag 리스트로 변환합니다.
     */
    private List<Tag> buildTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * TableDescription의 설정 정보를 JSON으로 변환합니다.
     */
    private String buildConfigurationJson(TableDescription tableDescription) {
        try {
            Map<String, Object> config = new HashMap<>();
            
            config.put("tableName", tableDescription.tableName());
            config.put("tableArn", tableDescription.tableArn());
            config.put("tableStatus", tableDescription.tableStatusAsString());
            config.put("itemCount", tableDescription.itemCount());
            config.put("tableSizeBytes", tableDescription.tableSizeBytes());
            
            // 키 스키마
            if (tableDescription.keySchema() != null) {
                config.put("keySchema", tableDescription.keySchema().stream()
                        .map(ks -> Map.of(
                                "attributeName", ks.attributeName(),
                                "keyType", ks.keyTypeAsString()
                        ))
                        .collect(Collectors.toList()));
            }
            
            // 빌링 모드
            if (tableDescription.billingModeSummary() != null) {
                config.put("billingMode", tableDescription.billingModeSummary().billingModeAsString());
            }
            
            // 프로비저닝된 용량
            if (tableDescription.provisionedThroughput() != null) {
                config.put("provisionedThroughput", Map.of(
                        "readCapacityUnits", tableDescription.provisionedThroughput().readCapacityUnits(),
                        "writeCapacityUnits", tableDescription.provisionedThroughput().writeCapacityUnits()
                ));
            }
            
            // GSI 정보
            if (tableDescription.globalSecondaryIndexes() != null && !tableDescription.globalSecondaryIndexes().isEmpty()) {
                config.put("globalSecondaryIndexCount", tableDescription.globalSecondaryIndexes().size());
            }
            
            // 스트림 정보
            if (tableDescription.streamSpecification() != null) {
                config.put("streamEnabled", tableDescription.streamSpecification().streamEnabled());
                config.put("streamViewType", tableDescription.streamSpecification().streamViewTypeAsString());
            }
            
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.warn("[AwsDynamoDbMapper] Failed to serialize configuration for table: {}", 
                    tableDescription.tableName(), e);
            return "{}";
        }
    }

    /**
     * TableDescription의 메타데이터를 JSON으로 변환합니다.
     */
    private String buildMetadata(TableDescription tableDescription) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            
            metadata.put("tableName", tableDescription.tableName());
            metadata.put("tableArn", tableDescription.tableArn());
            metadata.put("tableId", tableDescription.tableId());
            metadata.put("creationDateTime", tableDescription.creationDateTime());
            metadata.put("itemCount", tableDescription.itemCount());
            metadata.put("tableSizeBytes", tableDescription.tableSizeBytes());
            
            // 속성 정의
            if (tableDescription.attributeDefinitions() != null) {
                metadata.put("attributeDefinitions", tableDescription.attributeDefinitions().stream()
                        .map(ad -> Map.of(
                                "attributeName", ad.attributeName(),
                                "attributeType", ad.attributeTypeAsString()
                        ))
                        .collect(Collectors.toList()));
            }
            
            // LSI 정보
            if (tableDescription.localSecondaryIndexes() != null) {
                metadata.put("localSecondaryIndexes", tableDescription.localSecondaryIndexes().stream()
                        .map(lsi -> Map.of(
                                "indexName", lsi.indexName(),
                                "indexArn", lsi.indexArn() != null ? lsi.indexArn() : ""
                        ))
                        .collect(Collectors.toList()));
            }
            
            // GSI 정보
            if (tableDescription.globalSecondaryIndexes() != null) {
                metadata.put("globalSecondaryIndexes", tableDescription.globalSecondaryIndexes().stream()
                        .map(gsi -> Map.of(
                                "indexName", gsi.indexName(),
                                "indexArn", gsi.indexArn() != null ? gsi.indexArn() : "",
                                "indexStatus", gsi.indexStatusAsString() != null ? gsi.indexStatusAsString() : ""
                        ))
                        .collect(Collectors.toList()));
            }
            
            // 글로벌 테이블 버전
            if (tableDescription.globalTableVersion() != null) {
                metadata.put("globalTableVersion", tableDescription.globalTableVersion());
            }
            
            // 복제 정보
            if (tableDescription.replicas() != null && !tableDescription.replicas().isEmpty()) {
                metadata.put("replicas", tableDescription.replicas().stream()
                        .map(r -> Map.of(
                                "regionName", r.regionName(),
                                "replicaStatus", r.replicaStatusAsString() != null ? r.replicaStatusAsString() : ""
                        ))
                        .collect(Collectors.toList()));
            }
            
            // 서비스 타입
            metadata.put("serviceType", "DynamoDB");
            
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsDynamoDbMapper] Failed to serialize metadata for table: {}", 
                    tableDescription.tableName(), e);
            return "{}";
        }
    }

    /**
     * Instant를 LocalDateTime으로 변환합니다.
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

