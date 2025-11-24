package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.repository.TenantIsolationRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 테넌트 격리 수준 관리 서비스
 * 
 * <p>테넌트의 격리 수준(SHARED/DEDICATED)을 조회하고 설정합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantIsolationService {

    private final TenantIsolationRepository tenantIsolationRepository;
    private final TenantService tenantService;

    /**
     * 테넌트의 격리 수준 조회
     * 
     * @param tenant 테넌트
     * @return 격리 수준 (없으면 null)
     */
    public TenantIsolation.IsolationLevel getIsolationLevel(Tenant tenant) {
        log.debug("[TenantIsolationService] getIsolationLevel - tenantId={}", tenant.getId());
        
        Optional<TenantIsolation> isolation = tenantIsolationRepository.findByTenantAndIsDeletedFalse(tenant);
        
        if (isolation.isPresent()) {
            TenantIsolation.IsolationLevel level = isolation.get().getIsolationLevel();
            log.debug("[TenantIsolationService] getIsolationLevel - success level={}, tenantId={}", 
                    level, tenant.getId());
            return level;
        }
        
        log.debug("[TenantIsolationService] getIsolationLevel - not found tenantId={}", tenant.getId());
        return null;
    }

    /**
     * 테넌트의 격리 정보 조회
     * 
     * @param tenant 테넌트
     * @return 격리 정보
     */
    public Optional<TenantIsolation> getTenantIsolation(Tenant tenant) {
        log.info("[TenantIsolationService] getTenantIsolation - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        
        Optional<TenantIsolation> isolation = tenantIsolationRepository.findByTenantAndIsDeletedFalse(tenant);
        
        log.info("[TenantIsolationService] getTenantIsolation - found={}, tenantKey={}", 
                isolation.isPresent(), LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return isolation;
    }

    /**
     * 테넌트의 격리 수준 설정
     * 
     * @param tenant 테넌트
     * @param isolationLevel 격리 수준
     * @return 저장된 격리 정보
     */
    @Transactional
    public TenantIsolation setIsolationLevel(Tenant tenant, TenantIsolation.IsolationLevel isolationLevel) {
        log.info("[TenantIsolationService] setIsolationLevel - tenantKey={}, level={}", 
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), isolationLevel);

        Optional<TenantIsolation> existing = tenantIsolationRepository.findByTenantAndIsDeletedFalse(tenant);

        TenantIsolation isolation;
        if (existing.isPresent()) {
            isolation = existing.get();
            isolation.setIsolationLevel(isolationLevel);
            log.info("[TenantIsolationService] setIsolationLevel - updated tenantKey={}, level={}", 
                    LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), isolationLevel);
        } else {
            isolation = TenantIsolation.builder()
                    .tenant(tenant)
                    .isolationLevel(isolationLevel)
                    .build();
            log.info("[TenantIsolationService] setIsolationLevel - created tenantKey={}, level={}", 
                    LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), isolationLevel);
        }

        TenantIsolation saved = tenantIsolationRepository.save(isolation);
        log.info("[TenantIsolationService] setIsolationLevel - success tenantKey={}, level={}", 
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()), isolationLevel);
        return saved;
    }

    /**
     * 테넌트의 격리 정보 저장
     * 
     * @param tenantIsolation 격리 정보
     * @return 저장된 격리 정보
     */
    @Transactional
    public TenantIsolation saveTenantIsolation(TenantIsolation tenantIsolation) {
        log.info("[TenantIsolationService] saveTenantIsolation - tenantKey={}, level={}", 
                LogMaskingUtils.maskTenantKey(tenantIsolation.getTenant().getTenantKey()), 
                tenantIsolation.getIsolationLevel());

        TenantIsolation saved = tenantIsolationRepository.save(tenantIsolation);
        
        log.info("[TenantIsolationService] saveTenantIsolation - success tenantKey={}, level={}", 
                LogMaskingUtils.maskTenantKey(saved.getTenant().getTenantKey()), 
                saved.getIsolationLevel());
        return saved;
    }
}
