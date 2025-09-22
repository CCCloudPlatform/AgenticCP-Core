package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<PlatformConfig> findAllByIsDeletedFalse(Pageable pageable);

    Page<PlatformConfig> findAllByIsSystemAndIsDeletedFalse(Boolean isSystem, Pageable pageable);

    Page<PlatformConfig> findAllByConfigTypeAndIsDeletedFalse(PlatformConfig.ConfigType type, Pageable pageable);

    Page<PlatformConfig> findAllByConfigTypeAndIsSystemAndIsDeletedFalse(PlatformConfig.ConfigType type, Boolean isSystem, Pageable pageable);

    // Service에서 사용하는 메서드들 추가
    Page<PlatformConfig> findAll(Pageable pageable);

    Page<PlatformConfig> findByConfigType(PlatformConfig.ConfigType configType, Pageable pageable);

    Page<PlatformConfig> findByIsSystem(Boolean isSystem, Pageable pageable);

    @Query("SELECT pc FROM PlatformConfig pc WHERE " +
           "(:configKeyPattern IS NULL OR pc.configKey LIKE :configKeyPattern) AND " +
           "(:configType IS NULL OR pc.configType = :configType) AND " +
           "(:isSystem IS NULL OR pc.isSystem = :isSystem) AND " +
           "pc.isDeleted = false")
    Page<PlatformConfig> findFilteredConfigs(
            @Param("configKeyPattern") String configKeyPattern,
            @Param("configType") PlatformConfig.ConfigType configType,
            @Param("isSystem") Boolean isSystem,
            Pageable pageable);
}
