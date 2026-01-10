package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlTagCommand;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlTaggingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS DynamoDB 태그 관리 어댑터
 * 
 * NoSqlTaggingPort를 구현하여 DynamoDB 테이블의 태그 관리 기능을 제공합니다.
 * DynamoDB는 테이블 ARN을 기준으로 태그를 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbTaggingAdapter implements NoSqlTaggingPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;

    /**
     * DynamoDB 테이블에 태그를 추가하거나 업데이트합니다.
     *
     * @param command 태그 추가/업데이트 명령
     * @throws BusinessException 태그 작업 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public void putTags(NoSqlTagCommand command) {
        log.info("[AwsDynamoDbTaggingAdapter] Putting tags for table: {}, tagCount: {}",
                command.getTableName(), command.getTags() != null ? command.getTags().size() : 0);

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 테이블 ARN 조회
            String tableArn = getTableArn(client, command.getTableName());

            if (command.getTags() == null || command.getTags().isEmpty()) {
                log.info("[AwsDynamoDbTaggingAdapter] No tags to add for table: {}", command.getTableName());
                return;
            }

            // 태그 추가/업데이트
            List<Tag> tags = command.getTags().entrySet().stream()
                    .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            TagResourceRequest request = TagResourceRequest.builder()
                    .resourceArn(tableArn)
                    .tags(tags)
                    .build();

            client.tagResource(request);

            log.info("[AwsDynamoDbTaggingAdapter] Successfully added {} tags to table: {}",
                    tags.size(), command.getTableName());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbTaggingAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTaggingAdapter] Failed to put tags for table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 테이블에서 태그를 제거합니다.
     *
     * @param command 태그 제거 명령 (tags 맵의 키만 사용)
     * @throws BusinessException 태그 작업 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public void removeTags(NoSqlTagCommand command) {
        log.info("[AwsDynamoDbTaggingAdapter] Removing tags from table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 테이블 ARN 조회
            String tableArn = getTableArn(client, command.getTableName());

            if (command.getTags() == null || command.getTags().isEmpty()) {
                log.info("[AwsDynamoDbTaggingAdapter] No tags to remove for table: {}", command.getTableName());
                return;
            }

            // 태그 키 목록 추출
            List<String> tagKeys = command.getTags().keySet().stream().toList();

            UntagResourceRequest request = UntagResourceRequest.builder()
                    .resourceArn(tableArn)
                    .tagKeys(tagKeys)
                    .build();

            client.untagResource(request);

            log.info("[AwsDynamoDbTaggingAdapter] Successfully removed {} tags from table: {}",
                    tagKeys.size(), command.getTableName());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbTaggingAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTaggingAdapter] Failed to remove tags from table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 테이블의 태그를 조회합니다.
     *
     * @param providerType 공급자 타입 (AWS)
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전 (선택)
     * @return 태그 맵
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public Map<String, String> getTags(CloudProvider.ProviderType providerType,
                                       String accountScope,
                                       String tableName,
                                       String region) {
        log.info("[AwsDynamoDbTaggingAdapter] Getting tags for table: {}", tableName);

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClientWithDefaultCredentials(region)) {
            // 테이블 ARN 조회
            String tableArn = getTableArn(client, tableName);

            // 태그 조회
            ListTagsOfResourceRequest request = ListTagsOfResourceRequest.builder()
                    .resourceArn(tableArn)
                    .build();

            ListTagsOfResourceResponse response = client.listTagsOfResource(request);

            Map<String, String> tags = new HashMap<>();
            if (response.tags() != null) {
                for (Tag tag : response.tags()) {
                    tags.put(tag.key(), tag.value());
                }
            }

            log.info("[AwsDynamoDbTaggingAdapter] Found {} tags for table: {}", tags.size(), tableName);

            return tags;

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbTaggingAdapter] Table not found: {}", tableName);
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTaggingAdapter] Failed to get tags for table: {}", tableName, e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * 테이블 ARN을 조회합니다.
     */
    private String getTableArn(DynamoDbClient client, String tableName) {
        DescribeTableRequest request = mapper.toDescribeTableRequest(tableName);
        DescribeTableResponse response = client.describeTable(request);
        return response.table().tableArn();
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }
}

