package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CloudRegionRepository extends JpaRepository<CloudRegion, Long> {
    Optional<CloudRegion> findByRegionKey(String regionKey);
}
