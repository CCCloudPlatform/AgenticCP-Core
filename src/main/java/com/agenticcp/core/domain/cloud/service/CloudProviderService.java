package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
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
 * 클라우드 공급자 관리 서비스
 *
 * 클라우드 공급자의 조회/생성/수정/상태변경 및 동기화 시점 업데이트 등의 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudProviderService {

    private final CloudProviderRepository cloudProviderRepository;

    public List<CloudProvider> getAllProviders() {
        log.info("[CloudProviderService] getAllProviders");
        List<CloudProvider> result = cloudProviderRepository.findAll();
        log.info("[CloudProviderService] getAllProviders - success count={}", result.size());
        return result;
    }

    public List<CloudProvider> getActiveProviders() {
        log.info("[CloudProviderService] getActiveProviders");
        List<CloudProvider> result = cloudProviderRepository.findActiveProviders(Status.ACTIVE);
        log.info("[CloudProviderService] getActiveProviders - success count={}", result.size());
        return result;
    }

    public Optional<CloudProvider> getProviderByKey(String providerKey) {
        log.info("[CloudProviderService] getProviderByKey - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        Optional<CloudProvider> result = cloudProviderRepository.findByProviderKey(providerKey);
        log.info("[CloudProviderService] getProviderByKey - found={} providerKey={}", result.isPresent(), LogMaskingUtils.mask(providerKey, 2, 2));
        return result;
    }

    public CloudProvider getProviderByKeyOrThrow(String providerKey) {
        log.info("[CloudProviderService] getProviderByKeyOrThrow - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider provider = cloudProviderRepository.findByProviderKey(providerKey)
                .orElseThrow(() -> new ResourceNotFoundException("CloudProvider", "providerKey", providerKey));
        log.info("[CloudProviderService] getProviderByKeyOrThrow - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        return provider;
    }

    public List<CloudProvider> getProvidersByType(CloudProvider.ProviderType providerType) {
        log.info("[CloudProviderService] getProvidersByType - type={}", providerType);
        List<CloudProvider> result = cloudProviderRepository.findByProviderType(providerType);
        log.info("[CloudProviderService] getProvidersByType - success count={} type={}", result.size(), providerType);
        return result;
    }

    public List<CloudProvider> getGlobalProviders() {
        log.info("[CloudProviderService] getGlobalProviders");
        List<CloudProvider> result = cloudProviderRepository.findGlobalProviders(Status.ACTIVE);
        log.info("[CloudProviderService] getGlobalProviders - success count={}", result.size());
        return result;
    }

    public List<CloudProvider> getGovernmentProviders() {
        log.info("[CloudProviderService] getGovernmentProviders");
        List<CloudProvider> result = cloudProviderRepository.findGovernmentProviders(Status.ACTIVE);
        log.info("[CloudProviderService] getGovernmentProviders - success count={}", result.size());
        return result;
    }

    public List<CloudProvider> getProvidersNeedingSync(int hoursSinceLastSync) {
        LocalDateTime before = LocalDateTime.now().minusHours(hoursSinceLastSync);
        log.info("[CloudProviderService] getProvidersNeedingSync - hoursSinceLastSync={}", hoursSinceLastSync);
        List<CloudProvider> result = cloudProviderRepository.findProvidersNeedingSync(before);
        log.info("[CloudProviderService] getProvidersNeedingSync - success count={} hoursSinceLastSync={}", result.size(), hoursSinceLastSync);
        return result;
    }

    public Long getActiveProviderCount() {
        log.info("[CloudProviderService] getActiveProviderCount");
        Long count = cloudProviderRepository.countActiveProviders(Status.ACTIVE);
        log.info("[CloudProviderService] getActiveProviderCount - success count={}", count);
        return count;
    }

    @Transactional
    public CloudProvider createProvider(CloudProvider cloudProvider) {
        log.info("[CloudProviderService] createProvider - providerKey={} name={}",
                LogMaskingUtils.mask(cloudProvider.getProviderKey(), 2, 2),
                cloudProvider.getProviderName());
        CloudProvider saved = cloudProviderRepository.save(cloudProvider);
        log.info("[CloudProviderService] createProvider - success providerKey={}", LogMaskingUtils.mask(saved.getProviderKey(), 2, 2));
        return saved;
    }

    @Transactional
    public CloudProvider updateProvider(String providerKey, CloudProvider updatedProvider) {
        log.info("[CloudProviderService] updateProvider - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider existingProvider = getProviderByKeyOrThrow(providerKey);
        
        existingProvider.setProviderName(updatedProvider.getProviderName());
        existingProvider.setDescription(updatedProvider.getDescription());
        existingProvider.setProviderType(updatedProvider.getProviderType());
        existingProvider.setStatus(updatedProvider.getStatus());
        existingProvider.setApiEndpoint(updatedProvider.getApiEndpoint());
        existingProvider.setApiVersion(updatedProvider.getApiVersion());
        existingProvider.setAuthenticationType(updatedProvider.getAuthenticationType());
        existingProvider.setSupportedRegions(updatedProvider.getSupportedRegions());
        existingProvider.setSupportedServices(updatedProvider.getSupportedServices());
        existingProvider.setPricingModel(updatedProvider.getPricingModel());
        existingProvider.setIsGlobal(updatedProvider.getIsGlobal());
        existingProvider.setIsGovernment(updatedProvider.getIsGovernment());
        existingProvider.setComplianceCertifications(updatedProvider.getComplianceCertifications());
        existingProvider.setMetadata(updatedProvider.getMetadata());
        
        CloudProvider saved = cloudProviderRepository.save(existingProvider);
        log.info("[CloudProviderService] updateProvider - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        return saved;
    }

    @Transactional
    public CloudProvider updateLastSync(String providerKey) {
        log.info("[CloudProviderService] updateLastSync - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setLastSync(LocalDateTime.now());
        CloudProvider saved = cloudProviderRepository.save(provider);
        log.info("[CloudProviderService] updateLastSync - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        return saved;
    }

    @Transactional
    public CloudProvider activateProvider(String providerKey) {
        log.info("[CloudProviderService] activateProvider - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setStatus(Status.ACTIVE);
        CloudProvider saved = cloudProviderRepository.save(provider);
        log.info("[CloudProviderService] activateProvider - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        return saved;
    }

    @Transactional
    public CloudProvider deactivateProvider(String providerKey) {
        log.info("[CloudProviderService] deactivateProvider - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setStatus(Status.INACTIVE);
        CloudProvider saved = cloudProviderRepository.save(provider);
        log.info("[CloudProviderService] deactivateProvider - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        return saved;
    }

    @Transactional
    public void deleteProvider(String providerKey) {
        log.info("[CloudProviderService] deleteProvider - providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setIsDeleted(true);
        cloudProviderRepository.save(provider);
        log.info("[CloudProviderService] deleteProvider - success providerKey={}", LogMaskingUtils.mask(providerKey, 2, 2));
    }
}
