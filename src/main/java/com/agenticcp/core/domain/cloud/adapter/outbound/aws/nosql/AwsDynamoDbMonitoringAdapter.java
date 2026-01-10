package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlMetricQuery;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlMonitoringPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS DynamoDB 모니터링 어댑터
 * 
 * NoSqlMonitoringPort를 구현하여 DynamoDB 테이블의 메트릭 조회 기능을 제공합니다.
 * 
 * 현재 구현:
 * - DescribeTable API를 통한 기본 메트릭 (아이템 수, 테이블 크기, 용량 설정)
 * 
 * 향후 확장:
 * - CloudWatch 연동을 통한 상세 메트릭 (RCU/WCU 사용량, 스로틀링, 지연시간 등)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbMonitoringAdapter implements NoSqlMonitoringPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;

    /**
     * DynamoDB 테이블의 메트릭을 조회합니다.
     * 
     * 현재는 DescribeTable API를 통해 기본적인 테이블 통계를 반환합니다.
     * CloudWatch 연동 시 더 상세한 메트릭 (RCU/WCU 사용량, 스로틀링 횟수 등)을 
     * 조회할 수 있습니다.
     *
     * @param query 메트릭 조회 조건 (테이블명, 메트릭 타입, 기간 등)
     * @return 메트릭 결과 (metricName → 값)
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public Map<String, Object> queryMetrics(NoSqlMetricQuery query) {
        log.info("[AwsDynamoDbMonitoringAdapter] Querying metrics for table: {}, metricTypes: {}",
                query.getTableName(), query.getMetricTypes());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClientWithDefaultCredentials(query.getRegion())) {
            // 테이블 정보 조회
            DescribeTableRequest request = mapper.toDescribeTableRequest(query.getTableName());
            DescribeTableResponse response = client.describeTable(request);
            TableDescription table = response.table();

            Map<String, Object> metrics = new HashMap<>();

            // 기본 테이블 메트릭
            metrics.put("tableName", table.tableName());
            metrics.put("tableStatus", table.tableStatusAsString());
            metrics.put("itemCount", table.itemCount());
            metrics.put("tableSizeBytes", table.tableSizeBytes());

            // 프로비저닝된 용량 정보
            if (table.provisionedThroughput() != null) {
                ProvisionedThroughputDescription throughput = table.provisionedThroughput();
                metrics.put("readCapacityUnits", throughput.readCapacityUnits());
                metrics.put("writeCapacityUnits", throughput.writeCapacityUnits());
                metrics.put("numberOfDecreasesToday", throughput.numberOfDecreasesToday());
            }

            // 빌링 모드
            if (table.billingModeSummary() != null) {
                metrics.put("billingMode", table.billingModeSummary().billingModeAsString());
            }

            // GSI 메트릭
            if (table.globalSecondaryIndexes() != null && !table.globalSecondaryIndexes().isEmpty()) {
                metrics.put("globalSecondaryIndexCount", table.globalSecondaryIndexes().size());
                
                Map<String, Object> gsiMetrics = new HashMap<>();
                for (GlobalSecondaryIndexDescription gsi : table.globalSecondaryIndexes()) {
                    Map<String, Object> gsiInfo = new HashMap<>();
                    gsiInfo.put("indexStatus", gsi.indexStatusAsString());
                    gsiInfo.put("itemCount", gsi.itemCount());
                    gsiInfo.put("indexSizeBytes", gsi.indexSizeBytes());
                    if (gsi.provisionedThroughput() != null) {
                        gsiInfo.put("readCapacityUnits", gsi.provisionedThroughput().readCapacityUnits());
                        gsiInfo.put("writeCapacityUnits", gsi.provisionedThroughput().writeCapacityUnits());
                    }
                    gsiMetrics.put(gsi.indexName(), gsiInfo);
                }
                metrics.put("globalSecondaryIndexes", gsiMetrics);
            }

            // LSI 메트릭
            if (table.localSecondaryIndexes() != null && !table.localSecondaryIndexes().isEmpty()) {
                metrics.put("localSecondaryIndexCount", table.localSecondaryIndexes().size());
                
                Map<String, Object> lsiMetrics = new HashMap<>();
                for (LocalSecondaryIndexDescription lsi : table.localSecondaryIndexes()) {
                    Map<String, Object> lsiInfo = new HashMap<>();
                    lsiInfo.put("itemCount", lsi.itemCount());
                    lsiInfo.put("indexSizeBytes", lsi.indexSizeBytes());
                    lsiMetrics.put(lsi.indexName(), lsiInfo);
                }
                metrics.put("localSecondaryIndexes", lsiMetrics);
            }

            // 스트림 정보
            if (table.streamSpecification() != null) {
                metrics.put("streamEnabled", table.streamSpecification().streamEnabled());
                metrics.put("streamViewType", table.streamSpecification().streamViewTypeAsString());
            }

            // 요청된 메트릭 타입에 따른 추가 처리
            if (query.getMetricTypes() != null && !query.getMetricTypes().isEmpty()) {
                addRequestedMetrics(metrics, query, table);
            }

            log.info("[AwsDynamoDbMonitoringAdapter] Retrieved {} metrics for table: {}",
                    metrics.size(), query.getTableName());

            return metrics;

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbMonitoringAdapter] Table not found: {}", query.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbMonitoringAdapter] Failed to query metrics for table: {}",
                    query.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * 요청된 메트릭 타입에 따라 추가 메트릭을 계산합니다.
     * 
     * TODO: CloudWatch 연동 시 실제 사용량 메트릭을 조회하도록 확장
     */
    private void addRequestedMetrics(Map<String, Object> metrics, 
                                     NoSqlMetricQuery query, 
                                     TableDescription table) {
        for (NoSqlMetricQuery.MetricType metricType : query.getMetricTypes()) {
            switch (metricType) {
                case READ_CAPACITY -> {
                    if (table.provisionedThroughput() != null) {
                        metrics.put("provisionedReadCapacity", table.provisionedThroughput().readCapacityUnits());
                    }
                    // TODO: CloudWatch에서 실제 ConsumedReadCapacityUnits 조회
                    metrics.put("consumedReadCapacity", "CloudWatch integration required");
                }
                case WRITE_CAPACITY -> {
                    if (table.provisionedThroughput() != null) {
                        metrics.put("provisionedWriteCapacity", table.provisionedThroughput().writeCapacityUnits());
                    }
                    // TODO: CloudWatch에서 실제 ConsumedWriteCapacityUnits 조회
                    metrics.put("consumedWriteCapacity", "CloudWatch integration required");
                }
                case THROTTLED_REQUESTS -> {
                    // TODO: CloudWatch에서 ReadThrottleEvents, WriteThrottleEvents 조회
                    metrics.put("throttledRequests", "CloudWatch integration required");
                }
                case LATENCY -> {
                    // TODO: CloudWatch에서 SuccessfulRequestLatency 조회
                    metrics.put("latency", "CloudWatch integration required");
                }
                case ERROR_COUNT -> {
                    // TODO: CloudWatch에서 SystemErrors, UserErrors 조회
                    metrics.put("errorCount", "CloudWatch integration required");
                }
            }
        }
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }
}

