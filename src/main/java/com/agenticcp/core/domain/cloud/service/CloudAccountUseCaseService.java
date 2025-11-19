package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.port.model.account.*;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.mapper.CloudAccountMapper;
import com.agenticcp.core.domain.cloud.mapper.CredentialCommandMapper;
import com.agenticcp.core.domain.cloud.port.outbound.AccountSyncPort;
import com.agenticcp.core.domain.cloud.port.outbound.AccountValidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountCredentialRepository;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsCredentialManager;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 클라우드 계정 UseCase 서비스
 * 클라우드 계정의 등록, 수정, 삭제, 조회 등의 유즈케이스를 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudAccountUseCaseService {

    private final TenantRepository tenantRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final CloudAccountCredentialRepository cloudAccountCredentialRepository;
    private final CloudAccountDomainService cloudAccountDomainService;
    private final AccountValidationPort accountValidationPort;
    private final CredentialProviderPort credentialProviderPort;
    private final CredentialCommandMapper credentialCommandMapper;
    private final AuditEventPort auditEventPort;
    private final AccountSyncPort accountSyncPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 클라우드 계정을 등록합니다. (핵심 로직)
     * 
     * @param request 계정 등록 요청
     * @return CloudAccountDto 등록된 계정 정보
     * @throws BusinessException 등록 실패 시
     */
    @Transactional
    public CloudAccountDto registerCloudAccount(RegisterCloudAccountRequest request) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] registerCloudAccount - tenantKey={}, providerType={}", 
                 tenantKey, request.getProviderType());
        
        // 1. 테넌트 및 프로바이더 조회
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudProvider provider = cloudProviderRepository.findByProviderType(request.getProviderType())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND,
                    "프로바이더를 찾을 수 없습니다: " + request.getProviderType()
                ));
        
        // 2. 계정 중복 검증 (Domain Service 위임)
        cloudAccountDomainService.validateAccountUniqueness(
            tenant.getId(), request.getAccountId(), request.getProviderType());
        
        // 3. AWS 자격증명으로 계정 검증 (동기 검증)
        AccountValidationRequest validationRequest = AccountValidationRequest.builder()
                .providerType(request.getProviderType())
                .accessKeyId(request.getAccessKey())
                .secretAccessKey(request.getSecretKey())
                .region(request.getRegion())
                .build();
        
        AccountValidationResult validationResult = accountValidationPort.validateAccount(validationRequest);
        
        if (!validationResult.getValid()) {
            log.warn("[CloudAccountUseCaseService] registerCloudAccount - validation failed: {}", 
                     validationResult.getMessage());
            throw new BusinessException(
                CloudErrorCode.ACCOUNT_VERIFICATION_FAILED, 
                validationResult.getMessage()
            );
        }
        
        // 4. 자격증명 암호화 저장 (CredentialProviderPort 사용)
        StoreCredentialCommand storeCommand = credentialCommandMapper.toStoreCommand(
                tenant.getTenantKey(),
                request.getProviderType(),
                request.getAccountId() != null ? request.getAccountId() : validationResult.getAccountId(),
                request.getAccessKey(),
                request.getSecretKey(),
                request.getRegion() != null ? request.getRegion() : validationResult.getRegion()
        );
        
        String credentialKey = credentialProviderPort.storeCredentials(
                storeCommand.getTenantKey(),
                storeCommand.getProviderType(),
                storeCommand.getAccountScope(),
                storeCommand.getCredentials()
        );
        
        // credentialKey로 CloudAccountCredential 엔티티 조회
        CloudAccountCredential savedCredential = cloudAccountCredentialRepository.findByCredentialKey(credentialKey)
                .orElseThrow(() -> new BusinessException(
                    CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                    "저장된 자격증명을 찾을 수 없습니다: " + credentialKey
                ));
        
        // 5. 검증 결과(AccountId 등)와 함께 CloudAccount 엔티티 생성 및 저장
        CloudAccount cloudAccount = CloudAccount.builder()
                .tenant(tenant)
                .provider(provider)
                .accountName(request.getAccountName())
                .accountId(validationResult.getAccountId() != null ? validationResult.getAccountId() : request.getAccountId())
                .credential(savedCredential)
                .accountStatus(AccountStatus.VERIFIED)
                .isDefault(request.getIsDefault() != null && request.getIsDefault())
                .verifiedAt(LocalDateTime.now())
                .metadata(buildMetadata(validationResult, request.getMetadata()))
                .build();
        
        // 6. 기본 계정 설정 시 기존 기본 계정 자동 해제 (Domain Service)
        if (cloudAccount.getIsDefault()) {
            cloudAccountDomainService.handleDefaultAccountSetting(
                tenant.getId(), request.getProviderType());
        }
        
        CloudAccount savedAccount = cloudAccountRepository.save(cloudAccount);
        
        // 7. 감사 로그 기록
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("accountId", savedAccount.getId());
        auditData.put("accountName", savedAccount.getAccountName());
        auditData.put("providerType", request.getProviderType().name());
        auditEventPort.record("REGISTER_CLOUD_ACCOUNT", "CloudAccount", "SUCCESS", auditData);
        
        log.info("[CloudAccountUseCaseService] registerCloudAccount - success accountId={}", 
                 savedAccount.getId());
        
        return CloudAccountMapper.toDto(savedAccount);
    }

    /**
     * 클라우드 계정 정보를 수정합니다.
     * 
     * @param accountId 계정 ID
     * @param request 수정 요청
     * @return CloudAccountDto 수정된 계정 정보
     */
    @Transactional
    public CloudAccountDto updateCloudAccount(Long accountId, UpdateCloudAccountRequest request) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] updateCloudAccount - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudAccount account = cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        // 필드 업데이트
        if (request.getAccountName() != null) {
            account.setAccountName(request.getAccountName());
        }
        
        if (request.getAccountStatus() != null) {
            account.setAccountStatus(request.getAccountStatus());
        }
        
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // 기본 계정으로 설정하는 경우, 기존 기본 계정 해제
            cloudAccountDomainService.handleDefaultAccountSetting(
                tenant.getId(), account.getProvider().getProviderType());
            account.setAsDefault();
        } else if (request.getIsDefault() != null && !request.getIsDefault()) {
            account.unsetAsDefault();
        }
        
        if (request.getRegion() != null && account.getCredential() != null) {
            account.getCredential().setRegion(request.getRegion());
        }
        
        if (request.getMetadata() != null) {
            String metadataJson = CloudAccountMapper.metadataToJson(request.getMetadata());
            account.setMetadata(metadataJson);
        }
        
        CloudAccount updatedAccount = cloudAccountRepository.save(account);
        
        // 감사 로그 기록
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("accountId", accountId);
        auditData.put("accountName", updatedAccount.getAccountName());
        auditEventPort.record("UPDATE_CLOUD_ACCOUNT", "CloudAccount", "SUCCESS", auditData);
        
        log.info("[CloudAccountUseCaseService] updateCloudAccount - success");
        return CloudAccountMapper.toDto(updatedAccount);
    }

    /**
     * 클라우드 계정을 삭제합니다.
     * 
     * @param accountId 계정 ID
     */
    @Transactional
    public void deleteCloudAccount(Long accountId) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] deleteCloudAccount - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudAccount account = cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        // 삭제 전 검증
        cloudAccountDomainService.validateAccountDeletion(account);
        
        // 자격증명 삭제 (CredentialProviderPort 사용)
        if (account.getCredential() != null) {
            DeleteCredentialCommand deleteCommand = credentialCommandMapper.toDeleteCommand(
                    account.getProvider().getProviderType(),
                    account.getCredential().getCredentialKey()
            );
            credentialProviderPort.deleteCredentials(
                    deleteCommand.getProviderType(),
                    deleteCommand.getCredentialKey()
            );
        }
        
        // 소프트 삭제
        account.setIsDeleted(true);
        cloudAccountRepository.save(account);
        
        // 감사 로그 기록
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("accountId", accountId);
        auditData.put("accountName", account.getAccountName());
        auditEventPort.record("DELETE_CLOUD_ACCOUNT", "CloudAccount", "SUCCESS", auditData);
        
        log.info("[CloudAccountUseCaseService] deleteCloudAccount - success");
    }

    /**
     * 계정 연결을 테스트합니다.
     * 
     * @param accountId 계정 ID
     * @return ConnectionTestResult 테스트 결과
     */
    @Transactional(readOnly = true)
    public ConnectionTestResult testConnection(Long accountId) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] testConnection - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudAccount account = cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        // 자격증명 조회 (CredentialProviderPort 사용)
        if (account.getCredential() == null) {
            throw new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "계정에 자격증명이 없습니다: " + accountId
            );
        }
        
        ResolveCredentialCommand resolveCommand = credentialCommandMapper.toResolveCommand(
                tenant.getTenantKey(),
                account.getProvider().getProviderType(),
                account.getAccountId() != null ? account.getAccountId() : account.getId().toString()
        );
        
        Object credentials = credentialProviderPort.resolveCredentials(
                resolveCommand.getTenantKey(),
                resolveCommand.getProviderType(),
                resolveCommand.getAccountScope()
        );
        
        // 연결 테스트를 위한 credentials Map 생성
        Map<String, String> credentialsMap = new HashMap<>();
        if (credentials instanceof AwsCredentialManager.AwsCredentials awsCredentials) {
            credentialsMap.put("accessKeyId", awsCredentials.getAccessKeyId());
            credentialsMap.put("secretAccessKey", awsCredentials.getSecretAccessKey());
            credentialsMap.put("region", awsCredentials.getRegion());
        }
        
        return accountValidationPort.testConnection(accountId, credentialsMap);
    }

    /**
     * 계정 정보를 동기화합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto 동기화된 계정 정보
     */
    @Transactional
    public CloudAccountDto syncAccountInfo(Long accountId) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] syncAccountInfo - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        // 계정 존재 여부 확인
        cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        // 동기화 수행
        CloudAccount syncedAccount = accountSyncPort.syncAccountInfo(accountId);
        
        log.info("[CloudAccountUseCaseService] syncAccountInfo - success");
        return CloudAccountMapper.toDto(syncedAccount);
    }

    /**
     * 계정 등록 전 자격증명을 검증합니다.
     * 
     * @param request 검증 요청
     * @return AccountValidationResult 검증 결과
     */
    @Transactional(readOnly = true)
    public AccountValidationResult validateAccountBeforeRegistration(AccountValidationRequest request) {
        log.info("[CloudAccountUseCaseService] validateAccountBeforeRegistration - providerType={}", 
                 request.getProviderType());
        
        AccountValidationResult result = accountValidationPort.validateAccount(request);
        
        log.info("[CloudAccountUseCaseService] validateAccountBeforeRegistration - result: valid={}", 
                 result.getValid());
        return result;
    }

    /**
     * 테넌트의 모든 클라우드 계정을 조회합니다.
     * 
     * @return CloudAccountDto 리스트
     */
    @Transactional(readOnly = true)
    public List<CloudAccountDto> getAllCloudAccounts() {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] getAllCloudAccounts - tenantKey={}", tenantKey);
        
        List<CloudAccount> accounts = cloudAccountRepository.findByTenantKey(tenantKey);
        return CloudAccountMapper.toDtoList(accounts);
    }

    /**
     * 계정 ID로 특정 클라우드 계정을 조회합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto
     */
    @Transactional(readOnly = true)
    public CloudAccountDto getCloudAccountById(Long accountId) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] getCloudAccountById - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudAccount account = cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        return CloudAccountMapper.toDto(account);
    }

    /**
     * 프로바이더 타입별 계정을 조회합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return CloudAccountDto 리스트
     */
    @Transactional(readOnly = true)
    public List<CloudAccountDto> getAccountsByProviderType(CloudProvider.ProviderType providerType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] getAccountsByProviderType - tenantKey={}, providerType={}", 
                 tenantKey, providerType);
        
        List<CloudAccount> accounts = cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType);
        return CloudAccountMapper.toDtoList(accounts);
    }

    /**
     * 특정 계정을 기본 계정으로 설정합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto 업데이트된 계정 정보
     */
    @Transactional
    public CloudAccountDto setAccountAsDefault(Long accountId) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] setAccountAsDefault - accountId={}, tenantKey={}", 
                 accountId, tenantKey);
        
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new BusinessException(
                    CommonErrorCode.TENANT_CONTEXT_NOT_SET,
                    "테넌트를 찾을 수 없습니다: " + tenantKey
                ));
        
        CloudAccount account = cloudAccountRepository.findByIdAndTenantId(accountId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountId
                ));
        
        // 기본 계정으로 설정하기 전에 기존 기본 계정 해제
        cloudAccountDomainService.handleDefaultAccountSetting(
            tenant.getId(), account.getProvider().getProviderType());
        
        // 현재 계정을 기본 계정으로 설정
        account.setAsDefault();
        CloudAccount updated = cloudAccountRepository.save(account);
        
        // 감사 로그 기록
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("accountId", accountId);
        auditData.put("accountName", updated.getAccountName());
        auditData.put("providerType", account.getProvider().getProviderType().name());
        auditEventPort.record("SET_DEFAULT_CLOUD_ACCOUNT", "CloudAccount", "SUCCESS", auditData);
        
        log.info("[CloudAccountUseCaseService] setAccountAsDefault - success");
        return CloudAccountMapper.toDto(updated);
    }

    /**
     * 프로바이더 타입별 기본 계정을 조회합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return CloudAccountDto
     */
    @Transactional(readOnly = true)
    public CloudAccountDto getDefaultAccountByProviderType(CloudProvider.ProviderType providerType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("[CloudAccountUseCaseService] getDefaultAccountByProviderType - tenantKey={}, providerType={}", 
                 tenantKey, providerType);
        
        CloudAccount account = cloudAccountRepository.findDefaultByTenantKeyAndProviderType(tenantKey, providerType)
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    String.format("프로바이더 타입 %s의 기본 계정을 찾을 수 없습니다", providerType)
                ));
        
        return CloudAccountMapper.toDto(account);
    }

    /**
     * 검증 결과와 메타데이터로 JSON 메타데이터를 생성합니다.
     * 
     * @param validationResult 검증 결과
     * @param additionalMetadata 추가 메타데이터
     * @return JSON 문자열
     */
    private String buildMetadata(AccountValidationResult validationResult, Map<String, String> additionalMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 검증 결과에서 메타데이터 추출
        if (validationResult.getRegion() != null) {
            metadata.put("region", validationResult.getRegion());
        }
        
        if (validationResult.getMetadata() != null) {
            metadata.putAll(validationResult.getMetadata());
        }
        
        // 추가 메타데이터 병합
        if (additionalMetadata != null) {
            metadata.putAll(additionalMetadata);
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[CloudAccountUseCaseService] buildMetadata - JSON conversion failed", e);
            return "{}";
        }
    }
}

