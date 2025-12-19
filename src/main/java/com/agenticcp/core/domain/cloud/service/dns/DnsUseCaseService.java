package com.agenticcp.core.domain.cloud.service.dns;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.DnsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.DnsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.DnsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.DnsUpdateRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest.AttributeKeys;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsQuery;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DnsUseCaseService {

    private static final String RESOURCE_TYPE = "DNS_ZONE";

    private final DnsPortRouter portRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort credentialPort;
    private final CloudResourceManagementHelper resourceHelper;

    /**
     * DNS 호스팅 존 생성
     */
    @Transactional
    public CloudResource createHostedZone(DnsCreateRequest request) {
        log.info("[DnsUseCaseService] createHostedZone - provider={}, accountScope={}, zoneName={}",
                request.getProviderType(), request.getAccountScope(), request.getZoneName());

        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();

        // Capability 검증
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.CREATE);

        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // accountScope 검증
        validateAccountScope(accountScope);

        // JIT 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        DnsCreateCommand command = DnsCreateCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(request.getRegion())
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .zoneName(request.getZoneName())
                .zoneType(request.getZoneType())
                .vpcId(request.getVpcId())
                .comment(request.getComment())
                .tags(request.getTags())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        // 어댑터 호출
        CloudResource resource = portRouter.management(providerType).createHostedZone(command);

        // DB에 CloudResource 저장
        try {
            String resourceName = request.getZoneName() != null ? request.getZoneName() : resource.getResourceId();
            ResourceRegistrationRequest registrationRequest = ResourceRegistrationRequest.builder()
                    .resourceId(resource.getResourceId())
                    .resourceName(resourceName)
                    .resourceType(CloudResource.ResourceType.NETWORK) // DNS는 네트워크 리소스로 분류
                    .tags(request.getTags())
                    .attributes(Map.of(
                            AttributeKeys.CONFIGURATION, buildConfigurationJson(request)
                    ))
                    .build();

            CloudResource savedResource = resourceHelper.registerResource(
                    providerType,
                    serviceKey,
                    registrationRequest
            );

            log.info("[DnsUseCaseService] createHostedZone - success resourceId={}", savedResource.getResourceId());
            return savedResource;
        } catch (Exception e) {
            log.error("[DnsUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: zoneId={}, error={}",
                    resource.getResourceId(), e.getMessage());

            // 보상 트랜잭션: CSP에 생성된 호스팅 존 삭제
            executeCompensatingTransaction(providerType, resource.getResourceId(), accountScope,
                    request.getRegion(), session);

            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "DNS 호스팅 존 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + resource.getResourceId()
            );
        }
    }

    /**
     * DNS 호스팅 존 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CloudResource> listHostedZones(DnsQueryRequest request) {
        log.info("[DnsUseCaseService] listHostedZones - provider={}, accountScope={}",
                request.getProviderType(), request.getAccountScope());

        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();

        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // accountScope 검증
        validateAccountScope(accountScope);

        // JIT 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Query 변환
        DnsQuery query = DnsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .regions(request.getRegions())
                .zoneName(request.getZoneName())
                .zoneType(request.getZoneType())
                .vpcId(request.getVpcId())
                .tagsEquals(request.getTags())
                .page(request.getPage())
                .size(request.getSize())
                .build();

        return portRouter.discovery(providerType).listHostedZones(query, session);
    }

    /**
     * 특정 DNS 호스팅 존 조회
     */
    @Transactional(readOnly = true)
    public Optional<CloudResource> getHostedZone(ProviderType providerType, String accountScope,
                                                 String region, String zoneId) {
        log.info("[DnsUseCaseService] getHostedZone - provider={}, accountScope={}, zoneId={}",
                providerType, accountScope, zoneId);

        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // accountScope 검증
        validateAccountScope(accountScope);

        // JIT 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        return portRouter.discovery(providerType).getHostedZone(zoneId, session);
    }

    /**
     * DNS 호스팅 존 수정
     */
    @Transactional
    public CloudResource updateHostedZone(ProviderType providerType, String accountScope,
                                          String region, String zoneId, DnsUpdateRequest request) {
        log.info("[DnsUseCaseService] updateHostedZone - provider={}, accountScope={}, zoneId={}",
                providerType, accountScope, zoneId);

        // Capability 검증
        String serviceKey = getServiceKeyForProvider(providerType);
        capabilityGuard.ensureSupported(providerType, serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.UPDATE);

        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // accountScope 검증
        validateAccountScope(accountScope);

        // JIT 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        DnsUpdateCommand command = DnsUpdateCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(zoneId)
                .comment(request.getComment())
                .tags(request.getTagsToAdd())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        return portRouter.management(providerType).updateHostedZone(command);
    }

    /**
     * DNS 호스팅 존 삭제
     */
    @Transactional
    public void deleteHostedZone(DnsDeleteRequest request) {
        log.info("[DnsUseCaseService] deleteHostedZone - provider={}, accountScope={}, zoneId={}",
                request.getProviderType(), request.getAccountScope(), request.getZoneId());

        ProviderType providerType = request.getProviderType();
        String accountScope = request.getAccountScope();

        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // accountScope 검증
        validateAccountScope(accountScope);

        // JIT 세션 획득
        CloudSessionCredential session = getSession(providerType, accountScope);

        // Command 변환
        DnsDeleteCommand command = DnsDeleteCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(request.getRegion())
                .providerResourceId(request.getZoneId())
                .forceDelete(request.getForceDelete())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        // CSP에서 호스팅 존 삭제
        portRouter.management(providerType).deleteHostedZone(command);

        // DB 소프트 삭제
        resourceHelper.softDeleteResource(request.getZoneId());

        log.info("[DnsUseCaseService] deleteHostedZone - success zoneId={}", request.getZoneId());
    }

    // ==================== Private Helper Methods ====================

    private CloudSessionCredential getSession(ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}",
                tenantKey, accountScope, providerType);

        try {
            CloudSessionCredential session = credentialPort.getSession(tenantKey, accountScope, providerType);
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}",
                        tenantKey, accountScope);
                throw new BusinessException(
                        CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                        "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
    }

    private String getServiceKeyForProvider(ProviderType providerType) {
        return switch (providerType) {
            case AWS -> "ROUTE53";
            case AZURE -> "AZURE_DNS";
            case GCP -> "CLOUD_DNS";
            default -> throw new BusinessException(CloudErrorCode.CLOUD_PROVIDER_NOT_SUPPORTED,
                    "지원하지 않는 프로바이더입니다: " + providerType);
        };
    }

    private void validateAccountScope(String accountScope) {
        if (accountScope == null || accountScope.trim().isEmpty()) {
            throw new BusinessException(
                    CloudErrorCode.ACCOUNT_SCOPE_REQUIRED,
                    "AccountScope가 필요합니다"
            );
        }
    }

    private String buildConfigurationJson(DnsCreateRequest request) {
        // 간단한 JSON 문자열 생성 (실제로는 JSON 라이브러리 사용 권장)
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"zoneType\":\"").append(request.getZoneType()).append("\"");
        if (request.getVpcId() != null) {
            sb.append(",\"vpcId\":\"").append(request.getVpcId()).append("\"");
        }
        if (request.getComment() != null) {
            sb.append(",\"comment\":\"").append(request.getComment()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 호스팅 존을 삭제합니다.
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            String zoneId,
            String accountScope,
            String region,
            CloudSessionCredential session
    ) {
        try {
            log.warn("[DnsUseCaseService] 보상 트랜잭션 실행: CSP 호스팅 존 삭제 시도 - zoneId={}", zoneId);

            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            DnsDeleteCommand deleteCommand = DnsDeleteCommand.builder()
                    .providerType(providerType)
                    .accountScope(accountScope)
                    .region(region)
                    .providerResourceId(zoneId)
                    .forceDelete(false)
                    .tenantKey(tenantKey)
                    .session(session)
                    .build();

            portRouter.management(providerType).deleteHostedZone(deleteCommand);
            log.info("[DnsUseCaseService] 보상 트랜잭션 완료: CSP 호스팅 존 삭제 성공 - zoneId={}", zoneId);
        } catch (Exception compensationError) {
            log.error("[DnsUseCaseService] 보상 트랜잭션 실패: Ghost Resource 발생 가능 - zoneId={}, error={}",
                    zoneId, compensationError.getMessage());
        }
    }
}
