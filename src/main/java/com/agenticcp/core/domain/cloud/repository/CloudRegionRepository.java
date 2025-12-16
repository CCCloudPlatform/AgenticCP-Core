package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CloudRegionRepository extends JpaRepository<CloudRegion, Long> {
    
    /**
     * regionKey로 CloudRegion 조회
     */
    Optional<CloudRegion> findByRegionKey(String regionKey);
    
    /**
     * providerType과 regionKey로 CloudRegion 조회
     */
    @Query("SELECT cr FROM CloudRegion cr WHERE cr.provider.providerType = :providerType AND cr.regionKey = :regionKey")
    Optional<CloudRegion> findByProviderTypeAndRegionKey(
        @Param("providerType") CloudProvider.ProviderType providerType,
        @Param("regionKey") String regionKey
    );
}
