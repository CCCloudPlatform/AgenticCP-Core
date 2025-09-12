package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.common.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public List<Tenant> getActiveTenants() {
        return tenantRepository.findActiveTenants(Status.ACTIVE);
    }

    public Optional<Tenant> getTenantByKey(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey);
    }

    public Tenant getTenantByKeyOrThrow(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "tenantKey", tenantKey));
    }

    public List<Tenant> getTenantsByType(Tenant.TenantType tenantType) {
        return tenantRepository.findByTenantType(tenantType);
    }

    public List<Tenant> getActiveTrialTenants() {
        return tenantRepository.findActiveTrialTenants(LocalDateTime.now());
    }

    public List<Tenant> getExpiredTenants() {
        return tenantRepository.findExpiredTenants(LocalDateTime.now(), Status.ACTIVE);
    }

    public Long getActiveTenantCount() {
        return tenantRepository.countActiveTenants(Status.ACTIVE);
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("Creating tenant: {}", tenant.getTenantKey());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(String tenantKey, Tenant updatedTenant) {
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
        
        log.info("Updating tenant: {}", tenantKey);
        return tenantRepository.save(existingTenant);
    }

    @Transactional
    public Tenant suspendTenant(String tenantKey) {
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setStatus(Status.SUSPENDED);
        log.info("Suspending tenant: {}", tenantKey);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant activateTenant(String tenantKey) {
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setStatus(Status.ACTIVE);
        log.info("Activating tenant: {}", tenantKey);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(String tenantKey) {
        Tenant tenant = getTenantByKeyOrThrow(tenantKey);
        tenant.setIsDeleted(true);
        tenantRepository.save(tenant);
        log.info("Soft deleted tenant: {}", tenantKey);
    }
}
