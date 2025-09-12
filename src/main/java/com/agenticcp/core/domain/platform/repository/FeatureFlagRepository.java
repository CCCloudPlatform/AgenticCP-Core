package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    Optional<FeatureFlag> findByFlagKey(String flagKey);

    List<FeatureFlag> findByIsEnabled(Boolean isEnabled);

    List<FeatureFlag> findByStatus(Status status);

    @Query("SELECT ff FROM FeatureFlag ff WHERE ff.isEnabled = true AND ff.status = :status " +
           "AND (ff.startDate IS NULL OR ff.startDate <= :now) " +
           "AND (ff.endDate IS NULL OR ff.endDate >= :now)")
    List<FeatureFlag> findActiveFlags(@Param("status") Status status, @Param("now") LocalDateTime now);

    @Query("SELECT ff FROM FeatureFlag ff WHERE ff.flagKey = :flagKey AND ff.isEnabled = true " +
           "AND ff.status = :status AND (ff.startDate IS NULL OR ff.startDate <= :now) " +
           "AND (ff.endDate IS NULL OR ff.endDate >= :now)")
    Optional<FeatureFlag> findActiveFlagByKey(@Param("flagKey") String flagKey, 
                                            @Param("status") Status status, 
                                            @Param("now") LocalDateTime now);
}
