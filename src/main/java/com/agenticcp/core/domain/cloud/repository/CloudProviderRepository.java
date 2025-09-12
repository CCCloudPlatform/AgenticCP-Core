package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CloudProviderRepository extends JpaRepository<CloudProvider, Long> {

    Optional<CloudProvider> findByProviderKey(String providerKey);

    List<CloudProvider> findByStatus(Status status);

    List<CloudProvider> findByProviderType(CloudProvider.ProviderType providerType);

    @Query("SELECT cp FROM CloudProvider cp WHERE cp.status = :status AND cp.isDeleted = false")
    List<CloudProvider> findActiveProviders(@Param("status") Status status);

    @Query("SELECT cp FROM CloudProvider cp WHERE cp.isGlobal = true AND cp.status = :status")
    List<CloudProvider> findGlobalProviders(@Param("status") Status status);

    @Query("SELECT cp FROM CloudProvider cp WHERE cp.isGovernment = true AND cp.status = :status")
    List<CloudProvider> findGovernmentProviders(@Param("status") Status status);

    @Query("SELECT cp FROM CloudProvider cp WHERE cp.lastSync < :before")
    List<CloudProvider> findProvidersNeedingSync(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(cp) FROM CloudProvider cp WHERE cp.status = :status AND cp.isDeleted = false")
    Long countActiveProviders(@Param("status") Status status);
}
