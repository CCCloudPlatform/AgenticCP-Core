package com.agenticcp.core.domain.cloud.service.function;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.FunctionCreateRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionInvokeRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionQueryRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionUpdateRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionInvokeCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionQuery;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionUpdateCommand;
import com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Serverless Function 유스케이스 서비스
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionUseCaseService {

    private static final String RESOURCE_TYPE = "FUNCTION";

    private final FunctionPortRouter portRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort credentialPort;
    private final CloudResourceManagementHelper resourceHelper;

    /**
     * Serverless Function 생성
     *
     * @param request 생성 요청
     * @return 생성된 Function CloudResource
     */
    @Transactional
    public CloudResource createFunction(FunctionCreateRequest request) {
        String serviceKey = getServiceKeyForProvider(request.getProviderType());
        capabilityGuard.ensureSupported(request.getProviderType(), serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.CREATE);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);

        CloudSessionCredential session = getSession(request.getProviderType(), accountScope);

        FunctionCreateCommand command = FunctionCreateCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(accountScope)
                .region(request.getRegion())
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .functionName(request.getFunctionName())
                .runtime(request.getRuntime())
                .handler(request.getHandler())
                .memorySize(request.getMemorySize())
                .timeout(request.getTimeout())
                .roleArn(request.getRoleArn())
                .environmentVariables(request.getEnvironmentVariables())
                .description(request.getDescription())
                .vpcId(request.getVpcId())
                .codeUri(request.getCodeUri())
                .codeZip(null)  // 직접 업로드는 Phase 2에서 지원 예정
                .tags(request.getTags())
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        CloudResource resource = portRouter.management(request.getProviderType()).createFunction(command);

        // DB에 CloudResource 저장
        try {
            ResourceRegistrationRequest registrationRequest = ResourceRegistrationRequest.builder()
                    .resourceId(resource.getResourceId())
                    .resourceName(request.getFunctionName() != null ? request.getFunctionName() : resource.getResourceName())
                    .resourceType(CloudResource.ResourceType.FUNCTION)
                    .tags(request.getTags())
                    .build();

            resourceHelper.registerResource(request.getProviderType(), serviceKey, registrationRequest);
        } catch (Exception e) {
            log.error("[FunctionUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: functionId={}, error={}",
                    resource.getResourceId(), e.getMessage());

            // 보상 트랜잭션: CSP에 생성된 Function 삭제
            executeCompensatingTransaction(request.getProviderType(), resource.getResourceId(), accountScope, request.getRegion(), session);

            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "Function 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + resource.getResourceId()
            );
        }

        return resource;
    }

    /**
     * Serverless Function 목록 조회
     *
     * @param request 조회 요청
     * @return Function 목록 (Page)
     */
    @Transactional(readOnly = true)
    public Page<CloudResource> listFunctions(FunctionQueryRequest request) {
        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);
        CloudSessionCredential session = getSession(request.getProviderType(), accountScope);

        FunctionQuery query = FunctionQuery.builder()
                .providerType(request.getProviderType())
                .accountScope(accountScope)
                .regions(request.getRegions())
                .functionName(request.getFunctionName())
                .runtime(request.getRuntime())
                .vpcId(request.getVpcId())
                .tagsEquals(request.getTags())
                .page(request.getPage())
                .size(request.getSize())
                .build();

        return portRouter.discovery(request.getProviderType()).listFunctions(query, session);
    }

    /**
     * 특정 Serverless Function 조회
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위
     * @param region 리전
     * @param providerResourceId 함수 ID/ARN
     * @return Function CloudResource (존재하지 않으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<CloudResource> getFunction(
            ProviderType providerType,
            String accountScope,
            String region,
            String providerResourceId
    ) {
        validateAccountScope(accountScope);
        CloudSessionCredential session = getSession(providerType, accountScope);

        String serviceKey = getServiceKeyForProvider(providerType);
        GetFunctionCommand command = GetFunctionCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(providerResourceId)
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .session(session)
                .build();

        return portRouter.discovery(providerType).getFunction(command);
    }

    /**
     * Serverless Function 수정
     *
     * @param request 수정 요청
     * @return 수정된 Function CloudResource
     */
    @Transactional
    public CloudResource updateFunction(FunctionUpdateRequest request) {
        String serviceKey = getServiceKeyForProvider(request.getProviderType());
        capabilityGuard.ensureSupported(request.getProviderType(), serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.UPDATE);

        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);
        CloudSessionCredential session = getSession(request.getProviderType(), accountScope);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // 기존 태그와 병합 (tagsToAdd 추가, tagsToRemove 제거는 어댑터에서 처리)
        Map<String, String> tags = request.getTagsToAdd() != null ? request.getTagsToAdd() : Map.of();

        FunctionUpdateCommand command = FunctionUpdateCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(accountScope)
                .region(request.getRegion())
                .providerResourceId(request.getFunctionId())
                .runtime(request.getRuntime())
                .handler(request.getHandler())
                .memorySize(request.getMemorySize())
                .timeout(request.getTimeout())
                .roleArn(request.getRoleArn())
                .environmentVariables(request.getEnvironmentVariables())
                .description(request.getDescription())
                .codeUri(request.getCodeUri())
                .codeZip(null)  // 직접 업로드는 Phase 2에서 지원 예정
                .tags(tags)
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        return portRouter.management(request.getProviderType()).updateFunction(command);
    }

    /**
     * Serverless Function 삭제
     *
     * @param request 삭제 요청
     */
    @Transactional
    public void deleteFunction(FunctionDeleteRequest request) {
        String serviceKey = getServiceKeyForProvider(request.getProviderType());
        capabilityGuard.ensureSupported(request.getProviderType(), serviceKey, RESOURCE_TYPE, CapabilityGuard.Operation.TERMINATE);

        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);
        CloudSessionCredential session = getSession(request.getProviderType(), accountScope);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        FunctionDeleteCommand command = FunctionDeleteCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(accountScope)
                .region(request.getRegion())
                .providerResourceId(request.getFunctionId())
                .serviceKey(serviceKey)
                .resourceType(RESOURCE_TYPE)
                .tenantKey(tenantKey)
                .providerSpecificConfig(request.getProviderSpecificConfig())
                .session(session)
                .build();

        portRouter.management(request.getProviderType()).deleteFunction(command);
    }

    /**
     * Serverless Function 실행
     *
     * @param request 실행 요청
     * @return 실행 결과 (JSON 문자열)
     */
    @Transactional
    public String invokeFunction(FunctionInvokeRequest request) {
        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);
        CloudSessionCredential session = getSession(request.getProviderType(), accountScope);

        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        FunctionInvokeCommand command = FunctionInvokeCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(accountScope)
                .region(request.getRegion())
                .providerResourceId(request.getFunctionId())
                .invocationType(request.getInvocationType())
                .payload(request.getPayload())
                .qualifier(request.getQualifier())
                .context(request.getContext())
                .tenantKey(tenantKey)
                .session(session)
                .build();

        return portRouter.invocation(request.getProviderType()).invokeFunction(command);
    }

    // ==================== Private Helper Methods ====================

    /**
     * 세션 자격증명 획득 (JIT 패턴)
     */
    private CloudSessionCredential getSession(ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        log.debug("[FunctionUseCaseService] 세션 획득 시작: tenantKey={}, accountScope={}, providerType={}",
                tenantKey, accountScope, providerType);

        try {
            CloudSessionCredential session = credentialPort.getSession(tenantKey, accountScope, providerType);
            log.info("[FunctionUseCaseService] 세션 획득 완료: expiresAt={}", session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("[FunctionUseCaseService] 자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}",
                        tenantKey, accountScope);
                throw new BusinessException(
                        CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                        "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
    }

    /**
     * accountScope 검증
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
     * 프로바이더 타입에 따른 서비스 키 반환
     * AWS: LAMBDA, Azure: AZURE_FUNCTIONS, GCP: CLOUD_FUNCTIONS
     */
    private String getServiceKeyForProvider(ProviderType providerType) {
        return switch (providerType) {
            case AWS -> "LAMBDA";
            case AZURE -> "AZURE_FUNCTIONS";
            case GCP -> "CLOUD_FUNCTIONS";
            default -> "FUNCTION";
        };
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 Function을 삭제합니다.
     * Ghost Resource 방지를 위해 DB 저장 실패 시 호출됩니다.
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            String functionId,
            String accountScope,
            String region,
            CloudSessionCredential session
    ) {
        try {
            log.warn("[FunctionUseCaseService] 보상 트랜잭션 실행: functionId={}", functionId);

            FunctionDeleteCommand deleteCommand = FunctionDeleteCommand.builder()
                    .providerType(providerType)
                    .accountScope(accountScope)
                    .region(region)
                    .providerResourceId(functionId)
                    .serviceKey(getServiceKeyForProvider(providerType))
                    .resourceType(RESOURCE_TYPE)
                    .tenantKey(TenantContextHolder.getCurrentTenantKeyOrThrow())
                    .providerSpecificConfig(Map.of())
                    .session(session)
                    .build();

            portRouter.management(providerType).deleteFunction(deleteCommand);
            log.info("[FunctionUseCaseService] 보상 트랜잭션 완료: functionId={}", functionId);
        } catch (Exception e) {
            log.error("[FunctionUseCaseService] 보상 트랜잭션 실패: functionId={}, error={}",
                    functionId, e.getMessage(), e);
            // 보상 트랜잭션 실패는 로그만 남기고 예외를 다시 던지지 않음
            // (원래 예외를 유지하기 위해)
        }
    }
}
