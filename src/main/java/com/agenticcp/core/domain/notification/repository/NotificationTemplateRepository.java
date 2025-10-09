package com.agenticcp.core.domain.notification.repository;

import com.agenticcp.core.domain.notification.entity.NotificationTemplate;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 템플릿 Repository
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 테넌트별 활성 템플릿 조회
     */
    List<NotificationTemplate> findByTenantIdAndIsActiveTrueAndIsDeletedFalse(String tenantId);

    /**
     * 템플릿명으로 조회
     */
    Optional<NotificationTemplate> findByTenantIdAndTemplateNameAndIsDeletedFalse(String tenantId, String templateName);

    /**
     * 타입별 템플릿 조회
     */
    List<NotificationTemplate> findByTenantIdAndTemplateTypeAndIsActiveTrueAndIsDeletedFalse(String tenantId, NotificationType templateType);

    /**
     * 활성화된 템플릿 조회
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId AND nt.isActive = true AND nt.isDeleted = false")
    List<NotificationTemplate> findActiveTemplatesByTenant(@Param("tenantId") String tenantId);
}
