package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsCloudFrontConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.cdn.CreateInvalidationCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.InvalidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNInvalidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * AWS CloudFront 캐시 무효화 어댑터
 * AWS CloudFront SDK를 사용하여 캐시 무효화 생성 및 조회 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsCloudFrontInvalidationAdapter implements CDNInvalidationPort, ProviderScoped {

    private final AwsCloudFrontConfig awsCloudFrontConfig;
    private final AwsCloudFrontErrorTranslator errorTranslator;
    private final AccountCredentialManagementPort accountCredentialManagementPort;

    /**
     * 캐시 무효화를 생성합니다.
     * 
     * @param command 무효화 생성 명령 (세션, Distribution ID, 경로 목록 포함)
     * @return 생성된 무효화 결과 (무효화 ID, 상태 포함)
     */
    @Override
    public InvalidationResult createInvalidation(CreateInvalidationCommand command) {
        log.debug("[AwsCloudFrontInvalidationAdapter] Creating invalidation for distribution: {}, paths: {}", 
                command.distributionId(), command.paths());

        return executeWithCloudFrontClient(command.accountScope(), client -> {
            // CallerReference 생성 (중복 방지용)
            String callerReference = command.callerReference() != null && !command.callerReference().isEmpty()
                    ? command.callerReference()
                    : UUID.randomUUID().toString();

            // Paths 설정 (최소 1개 필요)
            List<String> paths = command.paths() != null && !command.paths().isEmpty()
                    ? command.paths()
                    : List.of("/*");  // 기본값: 모든 경로

            // InvalidationBatch 생성
            InvalidationBatch invalidationBatch = InvalidationBatch.builder()
                    .paths(Paths.builder()
                            .quantity(paths.size())
                            .items(paths)
                            .build())
                    .callerReference(callerReference)
                    .build();

            // CreateInvalidationRequest 생성
            CreateInvalidationRequest request = CreateInvalidationRequest.builder()
                    .distributionId(command.distributionId())
                    .invalidationBatch(invalidationBatch)
                    .build();

            // 무효화 생성
            CreateInvalidationResponse response = client.createInvalidation(request);
            Invalidation invalidation = response.invalidation();

            log.info("[AwsCloudFrontInvalidationAdapter] Invalidation created successfully: invalidationId={}, distributionId={}", 
                    invalidation.id(), command.distributionId());

            // InvalidationResult로 변환
            return InvalidationResult.builder()
                    .invalidationId(invalidation.id())
                    .distributionId(command.distributionId())
                    .status(invalidation.status())
                    .createTime(invalidation.createTime() != null
                            ? LocalDateTime.ofInstant(invalidation.createTime(), ZoneId.systemDefault())
                            : LocalDateTime.now())
                    .paths(paths)
                    .build();
        });
    }

    /**
     * 캐시 무효화 상태를 조회합니다.
     * 
     * @param accountScope 조회 대상 Cloud 계정 범위
     * @param distributionId Distribution ID
     * @param invalidationId 무효화 ID
     * @return 무효화 결과 (존재하지 않으면 Optional.empty())
     */
    @Override
    public Optional<InvalidationResult> getInvalidation(
            String accountScope,
            String distributionId,
            String invalidationId) {
        log.debug("[AwsCloudFrontInvalidationAdapter] Getting invalidation: invalidationId={}, distributionId={}, accountScope={}", 
                invalidationId, distributionId, accountScope);

        return executeWithCloudFrontClient(accountScope, client -> {
            try {
                GetInvalidationRequest request = GetInvalidationRequest.builder()
                        .distributionId(distributionId)
                        .id(invalidationId)
                        .build();

                GetInvalidationResponse response = client.getInvalidation(request);
                Invalidation invalidation = response.invalidation();

                log.debug("[AwsCloudFrontInvalidationAdapter] Successfully retrieved invalidation: {}", invalidationId);

                // InvalidationResult로 변환
                InvalidationResult result = InvalidationResult.builder()
                        .invalidationId(invalidation.id())
                        .distributionId(distributionId)
                        .status(invalidation.status())
                        .createTime(invalidation.createTime() != null
                                ? LocalDateTime.ofInstant(invalidation.createTime(), ZoneId.systemDefault())
                                : LocalDateTime.now())
                        .paths(invalidation.invalidationBatch() != null 
                                && invalidation.invalidationBatch().paths() != null
                                && invalidation.invalidationBatch().paths().items() != null
                                ? invalidation.invalidationBatch().paths().items()
                                : List.of())
                        .build();

                return Optional.of(result);

            } catch (NoSuchInvalidationException e) {
                log.info("[AwsCloudFrontInvalidationAdapter] Invalidation not found: {}", invalidationId);
                return Optional.empty();
            }
        });
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     * CloudFrontClient를 사용하여 작업을 실행합니다.
     */
    private <R> R executeWithCloudFrontClient(String accountScope, Function<CloudFrontClient, R> action) {
        String resolvedScope = requireAccountScope(accountScope);
        CloudSessionCredential session = acquireSession(resolvedScope);

        try (CloudFrontClient client = awsCloudFrontConfig.createCloudFrontClient(session)) {
            return action.apply(client);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw errorTranslator.translate(e);
        }
    }

    private String requireAccountScope(String accountScope) {
        if (accountScope == null || accountScope.isBlank()) {
            throw new BusinessException(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED, "AccountScope가 필요합니다.");
        }
        return accountScope;
    }

    private CloudSessionCredential acquireSession(String accountScope) {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            CloudSessionCredential session = accountCredentialManagementPort.getSession(
                    tenantKey, accountScope, getProviderType());
            log.debug("[AwsCloudFrontInvalidationAdapter] 세션 획득 완료: accountScope={}, expiresAt={}", 
                    accountScope, session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsCloudFrontInvalidationAdapter] 세션 획득 실패: accountScope={}", accountScope, e);
            throw new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS,
                    "세션 획득에 실패했습니다: " + e.getMessage());
        }
    }
}

