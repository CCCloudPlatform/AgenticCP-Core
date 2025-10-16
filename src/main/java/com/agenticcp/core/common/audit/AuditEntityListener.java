package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.util.ChangeTracker;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * JPA 엔티티 생명주기 리스너
 * 
 * 엔티티가 UPDATE/DELETE 되기 직전에 자동으로 변경 전 값을 캡처합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AuditEntityListener {

    private static ChangeTracker changeTracker;

    @Autowired
    public void setChangeTracker(ChangeTracker tracker) {
        AuditEntityListener.changeTracker = tracker;
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof AuditLog) {
            return; // 감지된 엔티티가 AuditLog 자신이면, 아무것도 하지 않고 즉시 종료
        }
        try {
            if (changeTracker == null) {
                log.warn("ChangeTracker가 주입되지 않았습니다. 변경 추적을 건너뜁니다.");
                return;
            }

            Map<String, Object> oldValue = changeTracker.extractOldValue(entity);
            String entityId = extractEntityId(entity);
            
            if (oldValue != null && entityId != null) {
                AuditChangeContext.setChangeData(oldValue, entityId);
                log.debug("엔티티 변경 전 값 자동 캡처 [Entity: {}, ID: {}]", 
                         entity.getClass().getSimpleName(), entityId);
            }
            
        } catch (Exception e) {
            log.warn("엔티티 변경 추적 중 오류 발생 [Entity: {}]: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        try {
            if (changeTracker == null) {
                return;
            }

            Map<String, Object> oldValue = changeTracker.extractOldValue(entity);
            String entityId = extractEntityId(entity);
            
            if (oldValue != null && entityId != null) {
                AuditChangeContext.setChangeData(oldValue, entityId);
                log.debug("엔티티 삭제 전 값 자동 캡처 [Entity: {}, ID: {}]", 
                         entity.getClass().getSimpleName(), entityId);
            }
            
        } catch (Exception e) {
            log.warn("엔티티 삭제 추적 중 오류 발생 [Entity: {}]: {}", 
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String extractEntityId(Object entity) {
        try {
            try {
                Object id = entity.getClass().getMethod("getId").invoke(entity);
                return id != null ? id.toString() : null;
            } catch (NoSuchMethodException e) {
            }

            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    field.setAccessible(true);
                    Object id = field.get(entity);
                    return id != null ? id.toString() : null;
                }
            }

            Class<?> superClass = entity.getClass().getSuperclass();
            if (superClass != null) {
                for (Field field : superClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                        field.setAccessible(true);
                        Object id = field.get(entity);
                        return id != null ? id.toString() : null;
                    }
                }
            }

        } catch (Exception e) {
            log.debug("엔티티 ID 추출 실패: {}", e.getMessage());
        }
        
        return null;
    }
}

