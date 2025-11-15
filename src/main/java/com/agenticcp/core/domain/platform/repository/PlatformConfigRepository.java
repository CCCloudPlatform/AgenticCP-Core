package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 플랫폼 설정 리포지토리
 * <p>
 * 플랫폼 설정 엔티티에 대한 데이터베이스 접근을 제공하는 리포지토리 인터페이스입니다.
 * 설정 키, 타입, 시스템 설정 여부 등으로 조회할 수 있습니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {

    /**
     * 설정 키로 플랫폼 설정 조회
     * <p>
     * 지정된 설정 키로 플랫폼 설정을 조회합니다. 삭제되지 않은 설정과 삭제된 설정 모두 조회됩니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @return 플랫폼 설정 (존재하지 않으면 Optional.empty())
     */
    Optional<PlatformConfig> findByTenantIdAndConfigKey(String tenantKey, String configKey);

    /**
     * 설정 타입으로 플랫폼 설정 목록 조회
     * <p>
     * 지정된 설정 타입의 모든 플랫폼 설정을 조회합니다. 삭제되지 않은 설정과 삭제된 설정 모두 조회됩니다.
     * </p>
     *
     * @param configType 조회할 설정 타입
     * @return 해당 타입의 플랫폼 설정 목록
     */
    List<PlatformConfig> findByTenantIdAndConfigType(String tenantKey, PlatformConfig.ConfigType configType);

    /**
     * 시스템 설정 여부로 플랫폼 설정 목록 조회
     * <p>
     * 시스템 설정 또는 사용자 설정을 조회합니다. 삭제되지 않은 설정과 삭제된 설정 모두 조회됩니다.
     * </p>
     *
     * @param isSystem 시스템 설정 여부 (true: 시스템 설정, false: 사용자 설정)
     * @return 해당하는 플랫폼 설정 목록
     */
    List<PlatformConfig> findByTenantIdAndIsSystem(String tenantKey, Boolean isSystem);

    /**
     * 활성화된 플랫폼 설정 목록 조회
     * <p>
     * 삭제되지 않은(isDeleted=false) 모든 플랫폼 설정을 조회합니다.
     * </p>
     *
     * @return 활성화된 플랫폼 설정 목록
     */
    @Query("SELECT pc FROM PlatformConfig pc WHERE pc.tenantId = :tenantKey AND pc.isDeleted = false")
    List<PlatformConfig> findAllActiveByTenantId(@Param("tenantKey") String tenantKey);

    /**
     * 설정 키 패턴으로 활성화된 플랫폼 설정 목록 조회
     * <p>
     * 설정 키가 지정된 패턴과 일치하고 삭제되지 않은 플랫폼 설정을 조회합니다.
     * 패턴은 SQL LIKE 문법을 사용합니다 (예: "system.%", "user.%").
     * </p>
     *
     * @param pattern 설정 키 패턴 (SQL LIKE 문법)
     * @return 패턴과 일치하는 활성화된 플랫폼 설정 목록
     */
    @Query("SELECT pc FROM PlatformConfig pc WHERE pc.tenantId = :tenantKey AND pc.configKey LIKE :pattern AND pc.isDeleted = false")
    List<PlatformConfig> findByTenantIdAndConfigKeyPattern(@Param("tenantKey") String tenantKey, @Param("pattern") String pattern);
}
