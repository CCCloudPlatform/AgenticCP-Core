package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 테넌트 관리 서비스
 *
 * 테넌트 조회/생성/수정/상태변경 및 통계 관련 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    public List<Tenant> getAllTenants() {
        log.info("[TenantService] getAllTenants");
        List<Tenant> result = tenantRepository.findAll();
        log.info("[TenantService] getAllTenants - success count={}", result.size());
        return result;
    }

    public List<Tenant> getActiveTenants() {
        log.info("[TenantService] getActiveTenants");
        List<Tenant> result = tenantRepository.findActiveTenants(Status.ACTIVE);
        log.info("[TenantService] getActiveTenants - success count={}", result.size());
        return result;
    }

    public Optional<Tenant> getTenantByKey(String tenantKey) {
        log.info("[TenantService] getTenantByKey - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Optional<Tenant> result = tenantRepository.findByTenantKey(tenantKey);
        log.info("[TenantService] getTenantByKey - found={} tenantKey={}", result.isPresent(), LogMaskingUtils.maskTenantKey(tenantKey));
        return result;
    }

    public Tenant getTenantByKeyOrThrow(String tenantKey) {
        log.info("[TenantService] getTenantByKeyOrThrow - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "tenantKey", tenantKey));
        log.info("[TenantService] getTenantByKeyOrThrow - success tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        return tenant;
    }

    public List<Tenant> getTenantsByType(Tenant.TenantType tenantType) {
        log.info("[TenantService] getTenantsByType - type={}", tenantType);
        List<Tenant> result = tenantRepository.findByTenantType(tenantType);
        log.info("[TenantService] getTenantsByType - success count={} type={}", result.size(), tenantType);
        return result;
    }

    public List<Tenant> getActiveTrialTenants() {
        log.info("[TenantService] getActiveTrialTenants");
        List<Tenant> result = tenantRepository.findActiveTrialTenants(LocalDateTime.now());
        log.info("[TenantService] getActiveTrialTenants - success count={}", result.size());
        return result;
    }

    public List<Tenant> getExpiredTenants() {
        log.info("[TenantService] getExpiredTenants");
        List<Tenant> result = tenantRepository.findExpiredTenants(LocalDateTime.now(), Status.ACTIVE);
        log.info("[TenantService] getExpiredTenants - success count={}", result.size());
        return result;
    }

    public Long getActiveTenantCount() {
        log.info("[TenantService] getActiveTenantCount");
        Long count = tenantRepository.countActiveTenants(Status.ACTIVE);
        log.info("[TenantService] getActiveTenantCount - success count={}", count);
        return count;
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("[TenantService] createTenant - tenantKey={} name={}",
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()),
                tenant.getTenantName());
        Tenant saved = tenantRepository.save(tenant);
        log.info("[TenantService] createTenant - success tenantKey={}", LogMaskingUtils.maskTenantKey(saved.getTenantKey()));
        return saved;
    }

    @Transactional
    public Tenant updateTenant(String tenantKey, Tenant updatedTenant) {
        log.info("[TenantService] updateTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Tenant existingTenant = getTenantByKeyOrThrow(tenantKey);
        
        existingTenant.setTenantName(updatedTenant.getTenantName());
        existingTenant.setDescription(updatedTenant.getDescription());
        existingTenant.setStatus(updatedTenant.getStatus());
        existingTenant.setTenantType(updatedTenant.getTenantType());
        existingTenant.setMaxUsers(updatedTenant.getMaxUsers());
        existingTenant.setMaxResources(updatedTenant.getMaxResources());
        existingTenant.setStorageQuotaGb(updatedTenant.getStorageQuotaGb());
        existingTenant.setBandwidthQuotaGb(updatedTenant.getBandwidthQuotaGb());
        existingTenant.setContactEmail(updatedTenant.getContactEmail());
        existingTenant.setContactPhone(updatedTenant.getContactPhone());
        existingTenant.setBillingAddress(updatedTenant.getBillingAddress());
        existingTenant.setSettings(updatedTenant.getSettings());
        existingTenant.setSubscriptionStartDate(updatedTenant.getSubscriptionStartDate());
        existingTenant.setSubscriptionEndDate(updatedTenant.getSubscriptionEndDate());
        existingTenant.setIsTrial(updatedTenant.getIsTrial());
        existingTenant.setTrialEndDate(updatedTenant.getTrialEndDate());
        
        Tenant saved = tenantRepository.save(existingTenant);
        log.info("[TenantService] updateTenant - success tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        return saved;
    }

    @Transactional
    public Tenant suspendTenant(String tenantKey) {
        log.info("[TenantService] suspendTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setStatus(Status.SUSPENDED);
        Tenant saved = tenantRepository.save(tenant);
        log.info("[TenantService] suspendTenant - success tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        return saved;
    }

    @Transactional
    public Tenant activateTenant(String tenantKey) {
        log.info("[TenantService] activateTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setStatus(Status.ACTIVE);
        Tenant saved = tenantRepository.save(tenant);
        log.info("[TenantService] activateTenant - success tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        return saved;
    }

    @Transactional
    public void deleteTenant(String tenantKey) {
        log.info("[TenantService] deleteTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setIsDeleted(true);
        tenantRepository.save(tenant);
        log.info("[TenantService] deleteTenant - success tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
    }
}
