package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlCreateTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlDeleteTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlUpdateTableCommand;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlTableManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/**
 * AWS DynamoDB 테이블 관리 어댑터
 * 
 * NoSqlTableManagementPort를 구현하여 DynamoDB 테이블의 생성, 수정, 삭제 기능을 제공합니다.
 * S3/VM 어댑터와 동일한 헥사고날 아키텍처 패턴을 따릅니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbTableManagementAdapter implements NoSqlTableManagementPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;
    private final CloudProviderRepository cloudProviderRepository;

    /**
     * DynamoDB 테이블을 생성합니다.
     *
     * @param command 생성 명령 (테이블명, 키 스키마, 용량 설정 등)
     * @return 생성된 테이블의 CloudResource 표현
     * @throws BusinessException 테이블이 이미 존재하거나 생성 실패 시
     */
    @Override
    public CloudResource createTable(NoSqlCreateTableCommand command) {
        log.info("[AwsDynamoDbTableManagementAdapter] Creating DynamoDB table: {} in region {}",
                command.getTableName(), command.getRegion());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 테이블 생성 요청 변환
            CreateTableRequest request = mapper.toCreateTableRequest(command);

            // 테이블 생성
            CreateTableResponse response = client.createTable(request);
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbTableManagementAdapter] Successfully created DynamoDB table: {}, ARN: {}",
                    tableDescription.tableName(), tableDescription.tableArn());

            // CloudResource로 변환하여 반환
            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (ResourceInUseException e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Table already exists: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "테이블이 이미 존재합니다: " + command.getTableName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Failed to create table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 테이블 구성을 수정합니다.
     * 용량 설정, 빌링 모드 등을 변경할 수 있습니다.
     *
     * @param command 수정 명령 (테이블명, 용량 설정 등)
     * @return 수정된 테이블의 CloudResource 표현
     * @throws BusinessException 수정 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public CloudResource updateTable(NoSqlUpdateTableCommand command) {
        log.info("[AwsDynamoDbTableManagementAdapter] Updating DynamoDB table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 테이블 존재 여부 확인
            TableDescription existingTable = describeTable(client, command.getTableName());
            if (existingTable == null) {
                throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
            }

            // 테이블 업데이트 요청 변환
            UpdateTableRequest request = mapper.toUpdateTableRequest(command);

            // 테이블 업데이트
            UpdateTableResponse response = client.updateTable(request);
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbTableManagementAdapter] Successfully updated DynamoDB table: {}",
                    tableDescription.tableName());

            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Failed to update table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 테이블을 삭제합니다.
     *
     * @param command 삭제 명령 (테이블명)
     * @throws BusinessException 삭제 실패 시
     * @throws ResourceNotFoundException 테이블을 찾을 수 없을 때
     */
    @Override
    public void deleteTable(NoSqlDeleteTableCommand command) {
        log.info("[AwsDynamoDbTableManagementAdapter] Deleting DynamoDB table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), command.getRegion())) {
            // 테이블 삭제 요청 변환
            DeleteTableRequest request = mapper.toDeleteTableRequest(command);

            // 테이블 삭제
            client.deleteTable(request);

            log.info("[AwsDynamoDbTableManagementAdapter] Successfully deleted DynamoDB table: {}",
                    command.getTableName());

        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (ResourceInUseException e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Table is in use: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "테이블이 사용 중입니다. 잠시 후 다시 시도해주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbTableManagementAdapter] Failed to delete table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * 테이블 정보를 조회합니다. (내부 헬퍼 메서드)
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
     * 이 어댑터가 지원하는 클라우드 프로바이더 타입을 반환합니다.
     *
     * @return AWS 프로바이더 타입
     */
    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     * AWS 프로바이더 엔티티를 조회합니다.
     */
    private CloudProvider findAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }
}

