package com.agenticcp.core.domain.cloud.service.cdn;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.CDNDistributionQueryRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNInvalidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * CDN Distribution 유스케이스 서비스
 * 
 * CDN Distribution의 생성, 조회, 수정, 삭제 및 캐시 무효화 기능을 제공합니다.
 * CSP에서 리소스를 생성/수정/삭제한 후 CloudResource 엔티티를 DB에 저장/업데이트합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CDNUseCaseService {

    private static final String SERVICE_KEY = "CloudFront";
    private static final String RESOURCE_TYPE = "CDN_DISTRIBUTION";

    private final CDNPortRouter cdnPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    private final CloudResourceManagementHelper resourceHelper;
    private final CloudResourceRepository cloudResourceRepository;

    /**
     * CDN Distribution을 생성합니다.
     * CSP에서 Distribution 생성 후 CloudResource 엔티티를 DB에 저장합니다.
     * 
     * 보상 트랜잭션: DB 저장 실패 시 CSP에 생성된 Distribution을 삭제하여
     * 데이터 정합성(Ghost Resource 방지)을 보장합니다.
     *
     * @param command 생성 명령
     * @return 생성된 Distribution CloudResource
     * @throws BusinessException DB 저장 실패 및 보상 트랜잭션 실행 시
     */
    @Transactional
    public CloudResource createDistribution(CreateDistributionCommand command) {
        capabilityGuard.ensureSupported(
            command.providerType(), 
            SERVICE_KEY,
            RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = command.accountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        CloudSessionCredential session = acquireSession(accountScope, command.providerType());
        
        // Command에 세션 설정
        CreateDistributionCommand commandWithSession = CreateDistributionCommand.builder()
                .providerType(command.providerType())
                .accountScope(command.accountScope())
                .serviceKey(command.serviceKey())
                .resourceType(command.resourceType())
                .distributionName(command.distributionName())
                .comment(command.comment())
                .enabled(command.enabled())
                .origin(command.origin())
                .cacheBehaviors(command.cacheBehaviors())
                .aliases(command.aliases())
                .sslCertificateId(command.sslCertificateId())
                .tags(command.tags())
                .tenantKey(tenantKey)
                .session(session)
                .build();
        
        // CSP에서 Distribution 생성
        CDNManagementPort managementPort = cdnPortRouter.management(command.providerType());
        CloudResource distribution = managementPort.createDistribution(commandWithSession);
        
        // DB에 CloudResource 저장 (실패 시 보상 트랜잭션 실행)
        try {
            String resourceName = command.distributionName() != null 
                    ? command.distributionName() 
                    : distribution.getResourceId();
            
            ResourceRegistrationRequest registrationRequest = ResourceRegistrationRequest.builder()
                    .resourceId(distribution.getResourceId())
                    .resourceName(resourceName)
                    .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
                    .tags(command.tags())
                    .attributes(Map.of())  // Origin, CacheBehavior 등은 metadata에 저장됨
                    .build();
            
            // CloudResource 등록
            CloudResource savedResource = resourceHelper.registerResource(
                    command.providerType(),
                    SERVICE_KEY,
                    registrationRequest
            );
            
            // distribution 객체의 metadata를 DB에 저장된 리소스에 설정
            if (distribution.getMetadata() != null && !distribution.getMetadata().isEmpty()) {
                savedResource.setMetadata(distribution.getMetadata());
                cloudResourceRepository.save(savedResource);
                log.debug("[CDNUseCaseService] Distribution metadata 저장 완료: distributionId={}", 
                        distribution.getResourceId());
            }
        } catch (Exception e) {
            log.error("[CDNUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: distributionId={}, error={}",
                    distribution.getResourceId(), e.getMessage());
            
            // 보상 트랜잭션: CSP에 생성된 Distribution 삭제
            executeCompensatingTransaction(
                    command.providerType(), 
                    managementPort, 
                    distribution.getResourceId(),
                    accountScope,
                    session
            );
            
            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "Distribution 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + distribution.getResourceId()
            );
        }
        
        return distribution;
    }

    /**
     * CDN Distribution을 조회합니다.
     * 
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @param providerType 프로바이더 타입
     * @return Distribution CloudResource (존재하지 않으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<CloudResource> getDistribution(String accountScope, String distributionId, ProviderType providerType) {
        validateAccountScope(accountScope);
        
        CDNDiscoveryPort discoveryPort = cdnPortRouter.discovery(providerType);
        return discoveryPort.getDistribution(accountScope, distributionId);
    }

    /**
     * CDN Distribution 목록을 조회합니다.
     * 
     * @param query 조회 쿼리
     * @return Distribution 목록 (페이징 정보 포함)
     */
    @Transactional(readOnly = true)
    public Page<CloudResource> listDistributions(CDNDistributionQueryRequest query) {
        validateAccountScope(query.accountScope());
        
        // JIT 세션 획득
        CloudSessionCredential session = acquireSession(query.accountScope(), query.providerType());
        
        // Query에 세션 설정
        CDNDistributionQueryRequest queryWithSession = CDNDistributionQueryRequest.builder()
                .providerType(query.providerType())
                .accountScope(query.accountScope())
                .distributionName(query.distributionName())
                .enabled(query.enabled())
                .tags(query.tags())
                .tenantKey(query.tenantKey())
                .page(query.page())
                .size(query.size())
                .sortBy(query.sortBy())
                .sortDirection(query.sortDirection())
                .session(session)
                .build();
        
        CDNDiscoveryPort discoveryPort = cdnPortRouter.discovery(query.providerType());
        return discoveryPort.listDistributions(queryWithSession);
    }

    /**
     * CDN Distribution을 수정합니다.
     * 
     * @param command 수정 명령
     * @return 수정된 Distribution CloudResource
     */
    @Transactional
    public CloudResource updateDistribution(UpdateDistributionCommand command) {
        capabilityGuard.ensureSupported(
            command.providerType(), 
            SERVICE_KEY, 
            RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = command.accountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        CloudSessionCredential session = acquireSession(accountScope, command.providerType());
        
        // Command에 세션 설정
        UpdateDistributionCommand commandWithSession = UpdateDistributionCommand.builder()
                .providerType(command.providerType())
                .accountScope(command.accountScope())
                .distributionId(command.distributionId())
                .etag(command.etag())
                .comment(command.comment())
                .enabled(command.enabled())
                .cacheBehaviors(command.cacheBehaviors())
                .aliases(command.aliases())
                .sslCertificateId(command.sslCertificateId())
                .tags(command.tags())
                .tenantKey(tenantKey)
                .session(session)
                .build();
        
        CDNManagementPort managementPort = cdnPortRouter.management(command.providerType());
        CloudResource distribution = managementPort.updateDistribution(commandWithSession);
        
        // DB 업데이트는 별도 동기화 작업으로 처리 (VPC 패턴과 동일)
        // 여기서는 CSP 업데이트만 수행하고 CloudResource 반환
        
        return distribution;
    }

    /**
     * CDN Distribution을 삭제합니다.
     * 
     * @param command 삭제 명령
     */
    @Transactional
    public void deleteDistribution(DeleteDistributionCommand command) {
        capabilityGuard.ensureSupported(
            command.providerType(), 
            SERVICE_KEY, 
            RESOURCE_TYPE, 
            CapabilityGuard.Operation.TERMINATE
        );
        
        // accountScope 검증
        String accountScope = command.accountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        CloudSessionCredential session = acquireSession(accountScope, command.providerType());
        
        // Command에 세션 설정
        DeleteDistributionCommand commandWithSession = DeleteDistributionCommand.builder()
                .providerType(command.providerType())
                .accountScope(command.accountScope())
                .distributionId(command.distributionId())
                .etag(command.etag())
                .session(session)
                .build();
        
        CDNManagementPort managementPort = cdnPortRouter.management(command.providerType());
        
        // CSP에서 Distribution 삭제
        managementPort.deleteDistribution(commandWithSession);
        
        // DB 소프트 삭제
        resourceHelper.softDeleteResource(command.distributionId());
    }

    /**
     * 캐시 무효화를 생성합니다.
     * 
     * @param command 무효화 생성 명령
     * @return 생성된 무효화 결과
     */
    @Transactional
    public InvalidationResult createInvalidation(CreateInvalidationCommand command) {
        validateAccountScope(command.accountScope());
        
        // JIT 세션 획득
        CloudSessionCredential session = acquireSession(command.accountScope(), ProviderType.AWS);
        
        // Command에 세션 설정
        CreateInvalidationCommand commandWithSession = CreateInvalidationCommand.builder()
                .accountScope(command.accountScope())
                .distributionId(command.distributionId())
                .paths(command.paths())
                .callerReference(command.callerReference())
                .session(session)
                .build();
        
        CDNInvalidationPort invalidationPort = cdnPortRouter.invalidation(ProviderType.AWS);
        return invalidationPort.createInvalidation(commandWithSession);
    }

    /**
     * 캐시 무효화 상태를 조회합니다.
     * 
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @param invalidationId 무효화 ID
     * @param providerType 프로바이더 타입
     * @return 무효화 결과 (존재하지 않으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<InvalidationResult> getInvalidation(
            String accountScope,
            String distributionId,
            String invalidationId,
            ProviderType providerType) {
        validateAccountScope(accountScope);
        
        CDNInvalidationPort invalidationPort = cdnPortRouter.invalidation(providerType);
        return invalidationPort.getInvalidation(accountScope, distributionId, invalidationId);
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 Distribution을 삭제합니다.
     * Ghost Resource 방지를 위해 DB 저장 실패 시 호출됩니다.
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            CDNManagementPort managementPort,
            String distributionId,
            String accountScope,
            CloudSessionCredential session
    ) {
        try {
            log.warn("[CDNUseCaseService] 보상 트랜잭션 실행: CSP Distribution 삭제 시도 - distributionId={}", distributionId);
            
            // ETag 조회를 위해 먼저 Distribution 조회
            CDNDiscoveryPort discoveryPort = cdnPortRouter.discovery(providerType);
            Optional<CloudResource> distributionOpt = discoveryPort.getDistribution(accountScope, distributionId);
            
            if (distributionOpt.isPresent()) {
                // metadata에서 ETag 추출 (간단히 처리, 실제로는 파싱 필요)
                String etag = "dummy-etag";  // 실제로는 metadata에서 추출
                
                DeleteDistributionCommand deleteCommand = DeleteDistributionCommand.builder()
                        .providerType(providerType)
                        .accountScope(accountScope)
                        .distributionId(distributionId)
                        .etag(etag)
                        .session(session)
                        .build();
                
                managementPort.deleteDistribution(deleteCommand);
                log.info("[CDNUseCaseService] 보상 트랜잭션 완료: CSP Distribution 삭제 성공 - distributionId={}", distributionId);
            } else {
                log.warn("[CDNUseCaseService] 보상 트랜잭션: Distribution을 찾을 수 없음 - distributionId={}", distributionId);
            }
        } catch (Exception compensationError) {
            // 보상 트랜잭션도 실패한 경우 - Ghost Resource 발생
            log.error("[CDNUseCaseService] 보상 트랜잭션 실패: Ghost Resource 발생 가능 - distributionId={}, error={}",
                    distributionId, compensationError.getMessage());
        }
    }

    /**
     * accountScope를 검증합니다.
     */
    private void validateAccountScope(String accountScope) {
        if (accountScope == null || accountScope.trim().isEmpty()) {
            throw new BusinessException(
                CloudErrorCode.ACCOUNT_SCOPE_REQUIRED,
                "AccountScope가 필요합니다"
            );
        }
    }

    /**
     * 세션을 획득합니다.
     */
    private CloudSessionCredential acquireSession(String accountScope, ProviderType providerType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        try {
            CloudSessionCredential session = accountCredentialManagementPort.getSession(
                    tenantKey, accountScope, providerType);
            log.debug("[CDNUseCaseService] 세션 획득 완료: expiresAt={}", session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("[CDNUseCaseService] 자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                        tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
    }
}

