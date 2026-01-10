package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlDiscoveryPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AWS DynamoDB 테이블 조회(Discovery) 어댑터
 *
 * NoSqlDiscoveryPort를 구현하여 DynamoDB 테이블 목록 조회 및 상세 조회 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbDiscoveryAdapter implements NoSqlDiscoveryPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;
    private final CloudProviderRepository cloudProviderRepository;
    private final AccountCredentialManagementPort accountCredentialManagementPort;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    /**
     * DynamoDB 테이블 목록을 조회합니다.
     *
     * @param query 조회 조건 (Provider, AccountScope, Region, ResourceType 등)
     * @return CloudResource 페이지
     */
    @Override
    public Page<CloudResource> listTables(ResourceQuery query) {
        log.info("[AwsDynamoDbDiscoveryAdapter] listTables - accountScope={}, regions={}",
                query.getAccountScope(), query.getRegions());

        String accountScope = requireAccountScope(query.getAccountScope());
        String region = getFirstRegion(query);

        return executeWithClient(accountScope, region, client -> {
            CloudProvider awsProvider = getAwsProvider();
            List<CloudResource> allResources = new ArrayList<>();
            String exclusiveStartTableName = null;

            // 모든 테이블 가져오기
            do {
                ListTablesRequest request = mapper.toListTablesRequest(exclusiveStartTableName, 100);
                ListTablesResponse response = client.listTables(request);

                for (String tableName : response.tableNames()) {
                    try {
                        DescribeTableResponse describeResponse = client.describeTable(
                                mapper.toDescribeTableRequest(tableName)
                        );

                        CloudResource resource = mapper.toCloudResource(
                                describeResponse.table(),
                                awsProvider
                        );
                        allResources.add(resource);
                    } catch (Exception e) {
                        log.warn("[AwsDynamoDbDiscoveryAdapter] Failed to describe table: {}", tableName, e);
                    }
                }

                exclusiveStartTableName = response.lastEvaluatedTableName();
            } while (exclusiveStartTableName != null);

            // 이름 필터링
            List<CloudResource> filtered = allResources;
            if (query.getNameContains() != null && !query.getNameContains().isEmpty()) {
                filtered = filtered.stream()
                        .filter(r -> r.getResourceName() != null &&
                                r.getResourceName().contains(query.getNameContains()))
                        .collect(Collectors.toList());
            }

            // 페이징 처리
            int page = query.getPage();
            int size = query.getSize();
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            List<CloudResource> paged = (start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

            log.info("[AwsDynamoDbDiscoveryAdapter] listTables - success, totalTables={}, pageSize={}",
                    filtered.size(), paged.size());

            return new PageImpl<>(paged, PageRequest.of(page, size), filtered.size());
        });
    }

    /**
     * 특정 DynamoDB 테이블을 조회합니다.
     *
     * @param id 리소스 식별 정보
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    @Override
    public Optional<CloudResource> getTable(ResourceIdentity id) {
        log.info("[AwsDynamoDbDiscoveryAdapter] getTable - tableName={}, region={}",
                id.getProviderResourceId(), id.getRegion());

        String accountScope = requireAccountScope(id.getAccountScope());
        String region = id.getRegion();

        return executeWithClient(accountScope, region, client -> {
            try {
                DescribeTableRequest request = mapper.toDescribeTableRequest(id.getProviderResourceId());
                DescribeTableResponse response = client.describeTable(request);

                TableDescription tableDescription = response.table();
                if (tableDescription == null) {
                    log.warn("[AwsDynamoDbDiscoveryAdapter] getTable - table not found: {}",
                            id.getProviderResourceId());
                    return Optional.empty();
                }

                CloudProvider awsProvider = getAwsProvider();
                CloudResource resource = mapper.toCloudResource(tableDescription, awsProvider);

                log.info("[AwsDynamoDbDiscoveryAdapter] getTable - success, tableName={}",
                        id.getProviderResourceId());

                return Optional.of(resource);

            } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
                log.warn("[AwsDynamoDbDiscoveryAdapter] getTable - table not found: {}",
                        id.getProviderResourceId());
                return Optional.empty();
            }
        });
    }

    // ==================== Private Helper Methods ====================

    /**
     * DynamoDB 클라이언트를 사용하여 작업을 수행합니다.
     */
    private <R> R executeWithClient(String accountScope, String region, Function<DynamoDbClient, R> action) {
        CloudSessionCredential session = acquireSession(accountScope);

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(session, region)) {
            return action.apply(client);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw errorTranslator.translate(e);
        }
    }

    /**
     * AWS Provider를 조회합니다.
     */
    private CloudProvider getAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }

    /**
     * AccountScope가 필수인지 검증합니다.
     */
    private String requireAccountScope(String accountScope) {
        if (accountScope == null || accountScope.isBlank()) {
            throw new BusinessException(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED, "AccountScope가 필요합니다.");
        }
        return accountScope;
    }

    /**
     * ResourceQuery에서 첫 번째 리전을 가져옵니다.
     */
    private String getFirstRegion(ResourceQuery query) {
        if (query.getRegions() != null && !query.getRegions().isEmpty()) {
            return query.getRegions().iterator().next();
        }
        return null;
    }

    /**
     * 세션 자격증명을 획득합니다.
     */
    private CloudSessionCredential acquireSession(String accountScope) {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            CloudSessionCredential session = accountCredentialManagementPort.getSession(
                    tenantKey, accountScope, getProviderType());
            log.debug("세션 획득 완료: accountScope={}, expiresAt={}", accountScope, session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("세션 획득 실패: accountScope={}", accountScope, e);
            throw new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS,
                    "세션 획득에 실패했습니다: " + e.getMessage());
        }
    }
}
