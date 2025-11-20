package com.agenticcp.core.domain.cloud.service.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 클라우드 계정 도메인 서비스
 * 클라우드 계정 관련 비즈니스 로직을 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudAccountDomainService {

    private final CloudAccountRepository cloudAccountRepository;
    private final MaskingService maskingService;
    private final SessionCacheService sessionCacheService;

    /**
     * 계정 고유성을 검증합니다.
     * 동일한 테넌트 내에서 같은 accountScope가 이미 등록되어 있는지 확인합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @param providerType 프로바이더 타입
     * @throws BusinessException 중복된 계정이 존재하는 경우
     */
    public void validateAccountUniqueness(Long tenantId, String accountScope, ProviderType providerType) {
        if (accountScope == null || accountScope.trim().isEmpty()) {
            // accountScope가 없으면 검증하지 않음 (검증 후 자동 설정될 수 있음)
            return;
        }
        
        boolean exists = cloudAccountRepository.existsByTenantIdAndAccountScope(tenantId, accountScope);
        if (exists) {
            String maskedAccountScope = maskingService.maskAccountScope(accountScope);
            log.warn("[CloudAccountDomainService] validateAccountUniqueness - duplicate accountScope found: tenantId={}, accountScope={}, providerType={}", 
                     tenantId, maskedAccountScope, providerType);
            throw new BusinessException(
                CloudErrorCode.DUPLICATE_ACCOUNT,
                String.format("이미 등록된 계정입니다. Account Scope: %s", accountScope)
            );
        }
    }

    /**
     * 기본 계정 설정을 처리합니다.
     * 동일 프로바이더 타입의 기존 기본 계정을 자동으로 해제합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param providerType 프로바이더 타입
     */
    @Transactional
    public void handleDefaultAccountSetting(Long tenantId, ProviderType providerType) {
        log.info("[CloudAccountDomainService] handleDefaultAccountSetting - tenantId={}, providerType={}", 
                 tenantId, providerType);
        
        // 동일 프로바이더 타입의 기존 기본 계정들을 조회
        List<CloudAccount> existingDefaultAccounts = 
            cloudAccountRepository.findDefaultAccountsByTenantAndProviderType(tenantId, providerType);
        
        if (!existingDefaultAccounts.isEmpty()) {
            log.info("[CloudAccountDomainService] handleDefaultAccountSetting - unsetting {} existing default accounts", 
                     existingDefaultAccounts.size());
            
            // 기존 기본 계정들의 isDefault를 false로 변경
            for (CloudAccount account : existingDefaultAccounts) {
                account.unsetAsDefault();
                cloudAccountRepository.save(account);
            }
        }
    }

    /**
     * 계정 상태를 변경합니다.
     * 
     * @param account 계정 엔티티
     * @param newStatus 새로운 상태
     */
    @Transactional
    public void changeAccountStatus(CloudAccount account, AccountStatus newStatus) {
        log.info("[CloudAccountDomainService] changeAccountStatus - accountId={}, newStatus={}", 
                 account.getId(), newStatus);
        
        switch (newStatus) {
            case ACTIVE:
                account.activate();
                break;
            case INACTIVE:
                account.deactivate();
                break;
            case SUSPENDED:
                account.suspend();
                break;
            case VERIFIED:
                account.markAsVerified();
                break;
            case FAILED:
                account.markAsFailed();
                break;
            case VERIFYING:
                throw new BusinessException(
                    CloudErrorCode.INVALID_ACCOUNT_STATUS,
                    "VERIFYING 상태로 직접 변경할 수 없습니다."
                );
            default:
                throw new BusinessException(
                    CloudErrorCode.INVALID_ACCOUNT_STATUS,
                    String.format("지원하지 않는 계정 상태입니다: %s", newStatus)
                );
        }
        cloudAccountRepository.save(account);
        evictCachedSession(account);
        log.info("[CloudAccountDomainService] changeAccountStatus - success accountId={}, newStatus={}", account.getId(), newStatus);
    }

    /**
     * 계정 삭제 전 검증을 수행합니다.
     * 
     * @param account 삭제할 계정
     * @throws BusinessException 삭제할 수 없는 경우
     */
    public void validateAccountDeletion(CloudAccount account) {
        // 필요시 추가 검증 로직 구현
        // 예: 연결된 리소스가 있는지 확인 등
        log.info("[CloudAccountDomainService] validateAccountDeletion - accountId={}", account.getId());
    }

    private void evictCachedSession(CloudAccount account) {
        if (account == null || account.getTenant() == null || account.getProvider() == null) {
            return;
        }

        String tenantKey = account.getTenant().getTenantKey();
        String accountScope = account.getAccountScope();
        ProviderType providerType = account.getProvider().getProviderType();

        if (tenantKey == null || accountScope == null || providerType == null) {
            return;
        }

        sessionCacheService.evictSession(tenantKey, accountScope, providerType);
        log.debug("[CloudAccountDomainService] evicted cached session - tenantKey={}, providerType={}, accountScope={}",
                tenantKey, providerType, maskingService.maskAccountScope(accountScope));
    }
}

