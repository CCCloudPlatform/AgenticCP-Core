package com.agenticcp.core.common.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 엔티티 감사 리스너
 * 
 * <p>엔티티의 생성/수정 시 자동으로 감사 정보를 설정합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class AuditEntityListener {

    /**
     * 엔티티 생성 전 감사 정보 설정
     */
    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof com.agenticcp.core.common.entity.BaseEntity) {
            com.agenticcp.core.common.entity.BaseEntity baseEntity = (com.agenticcp.core.common.entity.BaseEntity) entity;
            
            LocalDateTime now = LocalDateTime.now();
            String currentUser = getCurrentUser();
            
            if (baseEntity.getCreatedAt() == null) {
                baseEntity.setCreatedAt(now);
            }
            if (baseEntity.getUpdatedAt() == null) {
                baseEntity.setUpdatedAt(now);
            }
            if (baseEntity.getCreatedBy() == null) {
                baseEntity.setCreatedBy(currentUser);
            }
            if (baseEntity.getUpdatedBy() == null) {
                baseEntity.setUpdatedBy(currentUser);
            }
            if (baseEntity.getIsDeleted() == null) {
                baseEntity.setIsDeleted(false);
            }
        }
    }

    /**
     * 엔티티 수정 전 감사 정보 설정
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof com.agenticcp.core.common.entity.BaseEntity) {
            com.agenticcp.core.common.entity.BaseEntity baseEntity = (com.agenticcp.core.common.entity.BaseEntity) entity;
            
            baseEntity.setUpdatedAt(LocalDateTime.now());
            baseEntity.setUpdatedBy(getCurrentUser());
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    private String getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // SecurityContext가 없는 경우 무시
        }
        return "system";
    }
}