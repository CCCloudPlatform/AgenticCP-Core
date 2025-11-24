package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CloudServiceRepository extends JpaRepository<CloudService, Long> {
    
    /**
     * provider와 serviceKey로 CloudService 조회
     */
    Optional<CloudService> findByProviderAndServiceKey(CloudProvider provider, String serviceKey);
    
    /**
     * providerType과 serviceKey로 CloudService 조회
     */
    @Query("SELECT cs FROM CloudService cs WHERE cs.provider.providerType = :providerType AND cs.serviceKey = :serviceKey")
    Optional<CloudService> findByProviderTypeAndServiceKey(
        @Param("providerType") CloudProvider.ProviderType providerType,
        @Param("serviceKey") String serviceKey
    );
}

