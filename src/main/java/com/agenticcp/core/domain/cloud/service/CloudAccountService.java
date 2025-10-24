package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 클라우드 계정 관리 도메인 서비스
 * 
 * 테넌트의 클라우드 계정(AWS, GCP, Azure) 연결 정보를 관리합니다.
 * 계정 CRUD, 중복 검증, 상태 관리, CSP별 형식 검증 등의 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudAccountService {

    private final CloudAccountRepository cloudAccountRepository;
    
    // ==================== 검증 패턴 상수 ====================
    
    /**
     * AWS Account ID 패턴: 정확히 12자리 숫자
     */
    private static final Pattern AWS_ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{12}$");
    
    /**
     * GCP Project ID 패턴: 6-30자, 소문자, 숫자, 하이픈만 가능 (하이픈으로 시작 불가)
     */
    private static final Pattern GCP_PROJECT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{5,29}$");
    
    /**
     * Azure Subscription ID 패턴: UUID 형식 (8-4-4-4-12)
     */
    private static final Pattern AZURE_SUBSCRIPTION_ID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // ==================== 조회 메서드 ====================
    
    /**
     * 특정 테넌트의 모든 클라우드 계정을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @return 클라우드 계정 목록
     */
    public List<CloudAccount> getAccountsByTenantId(Long tenantId) {
        log.info("[CloudAccountService] getAccountsByTenantId - tenantId={}", tenantId);
        List<CloudAccount> accounts = cloudAccountRepository.findByTenantId(tenantId);
        log.info("[CloudAccountService] getAccountsByTenantId - success count={} tenantId={}", 
                accounts.size(), tenantId);
        return accounts;
    }
    
    /**
     * 특정 테넌트의 특정 프로바이더(CSP)에 해당하는 클라우드 계정을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param providerId 클라우드 프로바이더 ID
     * @return 클라우드 계정 목록
     */
    public List<CloudAccount> getAccountsByTenantIdAndProviderId(Long tenantId, Long providerId) {
        log.info("[CloudAccountService] getAccountsByTenantIdAndProviderId - tenantId={} providerId={}", 
                tenantId, providerId);
        List<CloudAccount> accounts = cloudAccountRepository.findByTenantIdAndProviderId(tenantId, providerId);
        log.info("[CloudAccountService] getAccountsByTenantIdAndProviderId - success count={} tenantId={} providerId={}", 
                accounts.size(), tenantId, providerId);
        return accounts;
    }
    
    /**
     * 특정 테넌트의 특정 프로바이더(CSP)에 해당하는 기본 클라우드 계정을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param providerId 클라우드 프로바이더 ID
     * @return 기본 클라우드 계정 (Optional)
     */
    public Optional<CloudAccount> getDefaultAccountByTenantIdAndProviderId(Long tenantId, Long providerId) {
        log.info("[CloudAccountService] getDefaultAccountByTenantIdAndProviderId - tenantId={} providerId={}", 
                tenantId, providerId);
        Optional<CloudAccount> account = cloudAccountRepository.findByTenantIdAndProviderIdAndIsDefaultTrue(tenantId, providerId);
        log.info("[CloudAccountService] getDefaultAccountByTenantIdAndProviderId - found={} tenantId={} providerId={}", 
                account.isPresent(), tenantId, providerId);
        return account;
    }
    
    /**
     * ID로 클라우드 계정을 조회합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 클라우드 계정 (Optional)
     */
    public Optional<CloudAccount> getAccountById(Long id) {
        log.info("[CloudAccountService] getAccountById - id={}", id);
        Optional<CloudAccount> account = cloudAccountRepository.findByIdAndIsDeletedFalse(id);
        log.info("[CloudAccountService] getAccountById - found={} id={}", account.isPresent(), id);
        return account;
    }
    
    /**
     * ID로 클라우드 계정을 조회하며, 없으면 예외를 던집니다.
     *
     * @param id 클라우드 계정 ID
     * @return 클라우드 계정
     * @throws ResourceNotFoundException 계정이 존재하지 않을 경우
     */
    public CloudAccount getAccountByIdOrThrow(Long id) {
        log.info("[CloudAccountService] getAccountByIdOrThrow - id={}", id);
        CloudAccount account = cloudAccountRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_ACCOUNT_NOT_FOUND));
        log.info("[CloudAccountService] getAccountByIdOrThrow - success id={}", id);
        return account;
    }
    
    /**
     * 특정 테넌트의 총 클라우드 계정 수를 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @return 클라우드 계정 수
     */
    public long countAccountsByTenantId(Long tenantId) {
        log.info("[CloudAccountService] countAccountsByTenantId - tenantId={}", tenantId);
        long count = cloudAccountRepository.countByTenantId(tenantId);
        log.info("[CloudAccountService] countAccountsByTenantId - success count={} tenantId={}", count, tenantId);
        return count;
    }
    
    // ==================== 생성 메서드 ====================
    
    /**
     * 새로운 클라우드 계정을 생성합니다.
     *
     * @param cloudAccount 클라우드 계정 엔티티
     * @return 저장된 클라우드 계정
     */
    @Transactional
    public CloudAccount createAccount(CloudAccount cloudAccount) {
        log.info("[CloudAccountService] createAccount - accountId={} providerId={} tenantId={}", 
                LogMaskingUtils.mask(cloudAccount.getAccountId(), 2, 2),
                cloudAccount.getProvider().getId(),
                cloudAccount.getTenant().getId());
        
        // 1. 중복 검증
        validateAccountUniqueness(
            cloudAccount.getTenant().getId(), 
            cloudAccount.getProvider().getId(), 
            cloudAccount.getAccountId()
        );
        
        // 2. CSP별 형식 검증
        validateAccountIdFormat(cloudAccount.getProvider().getProviderType(), cloudAccount.getAccountId());
        
        // 3. 저장
        CloudAccount saved = cloudAccountRepository.save(cloudAccount);
        
        log.info("[CloudAccountService] createAccount - success id={} accountId={}", 
                saved.getId(), LogMaskingUtils.mask(saved.getAccountId(), 2, 2));
        return saved;
    }
    
    // ==================== 수정 메서드 ====================
    
    /**
     * 클라우드 계정 정보를 수정합니다.
     *
     * @param id 클라우드 계정 ID
     * @param updatedAccount 수정할 정보가 담긴 CloudAccount 객체
     * @return 수정된 클라우드 계정
     */
    @Transactional
    public CloudAccount updateAccount(Long id, CloudAccount updatedAccount) {
        log.info("[CloudAccountService] updateAccount - id={}", id);
        
        CloudAccount existingAccount = getAccountByIdOrThrow(id);
        
        // 수정 가능한 필드만 업데이트
        existingAccount.setAccountName(updatedAccount.getAccountName());
        existingAccount.setDescription(updatedAccount.getDescription());
        existingAccount.setDefaultRegion(updatedAccount.getDefaultRegion());
        existingAccount.setMetadata(updatedAccount.getMetadata());
        
        // CSP별 필드 업데이트
        if (existingAccount.isAwsAccount()) {
            existingAccount.setRoleArn(updatedAccount.getRoleArn());
            existingAccount.setExternalId(updatedAccount.getExternalId());
        } else if (existingAccount.isGcpAccount()) {
            existingAccount.setServiceAccountEmail(updatedAccount.getServiceAccountEmail());
        } else if (existingAccount.isAzureAccount()) {
            existingAccount.setAzureTenantId(updatedAccount.getAzureTenantId());
            existingAccount.setAzureClientId(updatedAccount.getAzureClientId());
        }
        
        CloudAccount saved = cloudAccountRepository.save(existingAccount);
        log.info("[CloudAccountService] updateAccount - success id={}", id);
        return saved;
    }
    
    // ==================== 상태 관리 메서드 ====================
    
    /**
     * 클라우드 계정을 활성화합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 활성화된 클라우드 계정
     */
    @Transactional
    public CloudAccount activateAccount(Long id) {
        log.info("[CloudAccountService] activateAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.activate();
        account.updateLastVerified();
        
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("[CloudAccountService] activateAccount - success id={}", id);
        return saved;
    }
    
    /**
     * 클라우드 계정을 일시정지합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 일시정지된 클라우드 계정
     */
    @Transactional
    public CloudAccount suspendAccount(Long id) {
        log.info("[CloudAccountService] suspendAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.suspend();
        
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("[CloudAccountService] suspendAccount - success id={}", id);
        return saved;
    }
    
    /**
     * 클라우드 계정을 만료 처리합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 만료 처리된 클라우드 계정
     */
    @Transactional
    public CloudAccount expireAccount(Long id) {
        log.info("[CloudAccountService] expireAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.expire();
        
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("[CloudAccountService] expireAccount - success id={}", id);
        return saved;
    }
    
    /**
     * 마지막 검증 시간을 업데이트합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 업데이트된 클라우드 계정
     */
    @Transactional
    public CloudAccount updateLastVerified(Long id) {
        log.info("[CloudAccountService] updateLastVerified - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.updateLastVerified();
        
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("[CloudAccountService] updateLastVerified - success id={}", id);
        return saved;
    }
    
    // ==================== 기본 계정 관리 메서드 ====================
    
    /**
     * 특정 계정을 해당 테넌트와 프로바이더의 기본 계정으로 설정합니다.
     * 기존 기본 계정이 있다면 자동으로 해제됩니다.
     *
     * @param id 클라우드 계정 ID
     * @return 기본 계정으로 설정된 클라우드 계정
     */
    @Transactional
    public CloudAccount setAsDefaultAccount(Long id) {
        log.info("[CloudAccountService] setAsDefaultAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        
        // 기존 기본 계정 해제
        Optional<CloudAccount> existingDefault = cloudAccountRepository
                .findByTenantIdAndProviderIdAndIsDefaultTrue(
                    account.getTenant().getId(), 
                    account.getProvider().getId()
                );
        
        if (existingDefault.isPresent() && !existingDefault.get().getId().equals(id)) {
            CloudAccount oldDefault = existingDefault.get();
            oldDefault.unsetAsDefault();
            cloudAccountRepository.save(oldDefault);
            log.info("[CloudAccountService] setAsDefaultAccount - unset old default id={}", oldDefault.getId());
        }
        
        // 새로운 기본 계정 설정
        account.setAsDefault();
        CloudAccount saved = cloudAccountRepository.save(account);
        
        log.info("[CloudAccountService] setAsDefaultAccount - success id={}", id);
        return saved;
    }
    
    /**
     * 기본 계정 설정을 해제합니다.
     *
     * @param id 클라우드 계정 ID
     * @return 기본 계정에서 해제된 클라우드 계정
     */
    @Transactional
    public CloudAccount unsetAsDefaultAccount(Long id) {
        log.info("[CloudAccountService] unsetAsDefaultAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.unsetAsDefault();
        
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("[CloudAccountService] unsetAsDefaultAccount - success id={}", id);
        return saved;
    }
    
    // ==================== 삭제 메서드 ====================
    
    /**
     * 클라우드 계정을 소프트 삭제합니다.
     *
     * @param id 클라우드 계정 ID
     */
    @Transactional
    public void deleteAccount(Long id) {
        log.info("[CloudAccountService] deleteAccount - id={}", id);
        
        CloudAccount account = getAccountByIdOrThrow(id);
        account.setIsDeleted(true);
        cloudAccountRepository.save(account);
        
        log.info("[CloudAccountService] deleteAccount - success id={}", id);
    }
    
    // ==================== 검증 메서드 ====================
    
    /**
     * 계정 유일성을 검증합니다 (tenant_id + provider_id + account_id 조합).
     * 중복된 계정이 있으면 예외를 던집니다.
     *
     * @param tenantId 테넌트 ID
     * @param providerId 프로바이더 ID
     * @param accountId 계정 ID
     * @throws BusinessException 중복 계정이 존재할 경우
     */
    public void validateAccountUniqueness(Long tenantId, Long providerId, String accountId) {
        log.info("[CloudAccountService] validateAccountUniqueness - tenantId={} providerId={} accountId={}", 
                tenantId, providerId, LogMaskingUtils.mask(accountId, 2, 2));
        
        boolean exists = cloudAccountRepository.existsByTenantIdAndProviderIdAndAccountId(
            tenantId, providerId, accountId
        );
        
        if (exists) {
            log.warn("[CloudAccountService] validateAccountUniqueness - duplicate found tenantId={} providerId={} accountId={}", 
                    tenantId, providerId, LogMaskingUtils.mask(accountId, 2, 2));
            throw new BusinessException(CloudErrorCode.DUPLICATE_CLOUD_ACCOUNT);
        }
        
        log.info("[CloudAccountService] validateAccountUniqueness - success (no duplicate)");
    }
    
    /**
     * CSP별 Account ID 형식을 검증합니다.
     *
     * @param providerType 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountId 계정 ID
     * @throws BusinessException 형식이 올바르지 않을 경우
     */
    public void validateAccountIdFormat(CloudProvider.ProviderType providerType, String accountId) {
        log.info("[CloudAccountService] validateAccountIdFormat - providerType={} accountId={}", 
                providerType, LogMaskingUtils.mask(accountId, 2, 2));
        
        switch (providerType) {
            case AWS:
                validateAwsAccountId(accountId);
                break;
            case GCP:
                validateGcpProjectId(accountId);
                break;
            case AZURE:
                validateAzureSubscriptionId(accountId);
                break;
            default:
                // 다른 CSP는 현재 검증 생략
                log.info("[CloudAccountService] validateAccountIdFormat - skip validation for providerType={}", providerType);
        }
        
        log.info("[CloudAccountService] validateAccountIdFormat - success providerType={}", providerType);
    }
    
    /**
     * AWS Account ID 형식을 검증합니다 (12자리 숫자).
     *
     * @param accountId AWS Account ID
     * @throws BusinessException 형식이 올바르지 않을 경우
     */
    public void validateAwsAccountId(String accountId) {
        log.info("[CloudAccountService] validateAwsAccountId - accountId={}", 
                LogMaskingUtils.mask(accountId, 2, 2));
        
        if (accountId == null || !AWS_ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            log.warn("[CloudAccountService] validateAwsAccountId - invalid format accountId={}", 
                    LogMaskingUtils.mask(accountId, 2, 2));
            throw new BusinessException(CloudErrorCode.INVALID_AWS_ACCOUNT_ID);
        }
        
        log.info("[CloudAccountService] validateAwsAccountId - success");
    }
    
    /**
     * GCP Project ID 형식을 검증합니다 (6-30자, 소문자/숫자/하이픈만 가능).
     *
     * @param projectId GCP Project ID
     * @throws BusinessException 형식이 올바르지 않을 경우
     */
    public void validateGcpProjectId(String projectId) {
        log.info("[CloudAccountService] validateGcpProjectId - projectId={}", 
                LogMaskingUtils.mask(projectId, 2, 2));
        
        if (projectId == null || !GCP_PROJECT_ID_PATTERN.matcher(projectId).matches()) {
            log.warn("[CloudAccountService] validateGcpProjectId - invalid format projectId={}", 
                    LogMaskingUtils.mask(projectId, 2, 2));
            throw new BusinessException(CloudErrorCode.INVALID_GCP_PROJECT_ID);
        }
        
        log.info("[CloudAccountService] validateGcpProjectId - success");
    }
    
    /**
     * Azure Subscription ID 형식을 검증합니다 (UUID 형식).
     *
     * @param subscriptionId Azure Subscription ID
     * @throws BusinessException 형식이 올바르지 않을 경우
     */
    public void validateAzureSubscriptionId(String subscriptionId) {
        log.info("[CloudAccountService] validateAzureSubscriptionId - subscriptionId={}", 
                LogMaskingUtils.mask(subscriptionId, 2, 2));
        
        if (subscriptionId == null || !AZURE_SUBSCRIPTION_ID_PATTERN.matcher(subscriptionId).matches()) {
            log.warn("[CloudAccountService] validateAzureSubscriptionId - invalid format subscriptionId={}", 
                    LogMaskingUtils.mask(subscriptionId, 2, 2));
            throw new BusinessException(CloudErrorCode.INVALID_AZURE_SUBSCRIPTION_ID);
        }
        
        log.info("[CloudAccountService] validateAzureSubscriptionId - success");
    }
}

