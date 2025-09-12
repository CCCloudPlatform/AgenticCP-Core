package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {

    Optional<PlatformConfig> findByConfigKey(String configKey);

    List<PlatformConfig> findByConfigType(PlatformConfig.ConfigType configType);

    List<PlatformConfig> findByIsSystem(Boolean isSystem);

    @Query("SELECT pc FROM PlatformConfig pc WHERE pc.isDeleted = false")
    List<PlatformConfig> findAllActive();

    @Query("SELECT pc FROM PlatformConfig pc WHERE pc.configKey LIKE :pattern AND pc.isDeleted = false")
    List<PlatformConfig> findByConfigKeyPattern(@Param("pattern") String pattern);
}
