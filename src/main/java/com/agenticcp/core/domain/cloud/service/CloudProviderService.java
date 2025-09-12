package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
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
public class CloudProviderService {

    private final CloudProviderRepository cloudProviderRepository;

    public List<CloudProvider> getAllProviders() {
        return cloudProviderRepository.findAll();
    }

    public List<CloudProvider> getActiveProviders() {
        return cloudProviderRepository.findActiveProviders(Status.ACTIVE);
    }

    public Optional<CloudProvider> getProviderByKey(String providerKey) {
        return cloudProviderRepository.findByProviderKey(providerKey);
    }

    public CloudProvider getProviderByKeyOrThrow(String providerKey) {
        return cloudProviderRepository.findByProviderKey(providerKey)
                .orElseThrow(() -> new ResourceNotFoundException("CloudProvider", "providerKey", providerKey));
    }

    public List<CloudProvider> getProvidersByType(CloudProvider.ProviderType providerType) {
        return cloudProviderRepository.findByProviderType(providerType);
    }

    public List<CloudProvider> getGlobalProviders() {
        return cloudProviderRepository.findGlobalProviders(Status.ACTIVE);
    }

    public List<CloudProvider> getGovernmentProviders() {
        return cloudProviderRepository.findGovernmentProviders(Status.ACTIVE);
    }

    public List<CloudProvider> getProvidersNeedingSync(int hoursSinceLastSync) {
        LocalDateTime before = LocalDateTime.now().minusHours(hoursSinceLastSync);
        return cloudProviderRepository.findProvidersNeedingSync(before);
    }

    public Long getActiveProviderCount() {
        return cloudProviderRepository.countActiveProviders(Status.ACTIVE);
    }

    @Transactional
    public CloudProvider createProvider(CloudProvider cloudProvider) {
        log.info("Creating cloud provider: {}", cloudProvider.getProviderKey());
        return cloudProviderRepository.save(cloudProvider);
    }

    @Transactional
    public CloudProvider updateProvider(String providerKey, CloudProvider updatedProvider) {
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
        
        log.info("Updating cloud provider: {}", providerKey);
        return cloudProviderRepository.save(existingProvider);
    }

    @Transactional
    public CloudProvider updateLastSync(String providerKey) {
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setLastSync(LocalDateTime.now());
        log.info("Updated last sync for provider: {}", providerKey);
        return cloudProviderRepository.save(provider);
    }

    @Transactional
    public CloudProvider activateProvider(String providerKey) {
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setStatus(Status.ACTIVE);
        log.info("Activated provider: {}", providerKey);
        return cloudProviderRepository.save(provider);
    }

    @Transactional
    public CloudProvider deactivateProvider(String providerKey) {
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setStatus(Status.INACTIVE);
        log.info("Deactivated provider: {}", providerKey);
        return cloudProviderRepository.save(provider);
    }

    @Transactional
    public void deleteProvider(String providerKey) {
        CloudProvider provider = getProviderByKeyOrThrow(providerKey);
        provider.setIsDeleted(true);
        cloudProviderRepository.save(provider);
        log.info("Soft deleted provider: {}", providerKey);
    }
}
