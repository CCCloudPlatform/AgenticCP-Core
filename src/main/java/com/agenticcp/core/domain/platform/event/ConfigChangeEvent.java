package com.agenticcp.core.domain.platform.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;

import java.time.LocalDateTime;

/**
 * 플랫폼 설정 변경 이벤트
 * 
 * 설정이 생성/수정/삭제될 때 발행되는 이벤트입니다.
 * 보안을 위해 민감한 값은 마스킹되어 전달됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
@ToString
public class ConfigChangeEvent extends ApplicationEvent {
    
    /**
     * 변경된 설정 키
     */
    private final String configKey;
    
    /**
     * 이전 값 (마스킹됨)
     */
    @Masked(type = MaskingType.SECRET_KEY)
    private final String oldValueMasked;
    
    /**
     * 새로운 값 (마스킹됨)
     */
    @Masked(type = MaskingType.SECRET_KEY)
    private final String newValueMasked;
    
    /**
     * 변경 시각
     */
    private final LocalDateTime changedAt;
    
    /**
     * 변경 타입 (CREATE, UPDATE, DELETE)
     */
    private final ChangeType changeType;
    
    /**
     * 변경한 사용자 ID
     */
    private final String userId;
    
    /**
     * 변경 사유
     */
    private final String reason;
    
    /**
     * 설정 변경 타입
     */
    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE
    }
    
    /**
     * 생성자
     */
    public ConfigChangeEvent(Object source, String configKey, String oldValueMasked, String newValueMasked, 
                           LocalDateTime changedAt, ChangeType changeType, String userId, String reason) {
        super(source);
        this.configKey = configKey;
        this.oldValueMasked = oldValueMasked;
        this.newValueMasked = newValueMasked;
        this.changedAt = changedAt;
        this.changeType = changeType;
        this.userId = userId;
        this.reason = reason;
    }
    
    /**
     * 설정 생성 이벤트 생성
     */
    public static ConfigChangeEvent create(String configKey, String newValueMasked, String userId, String reason) {
        return new ConfigChangeEvent("PlatformConfigService", configKey, null, newValueMasked, 
                                   LocalDateTime.now(), ChangeType.CREATE, userId, reason);
    }
    
    /**
     * 설정 수정 이벤트 생성
     */
    public static ConfigChangeEvent update(String configKey, String oldValueMasked, String newValueMasked, String userId, String reason) {
        return new ConfigChangeEvent("PlatformConfigService", configKey, oldValueMasked, newValueMasked, 
                                   LocalDateTime.now(), ChangeType.UPDATE, userId, reason);
    }
    
    /**
     * 설정 삭제 이벤트 생성
     */
    public static ConfigChangeEvent delete(String configKey, String oldValueMasked, String userId, String reason) {
        return new ConfigChangeEvent("PlatformConfigService", configKey, oldValueMasked, null, 
                                   LocalDateTime.now(), ChangeType.DELETE, userId, reason);
    }
}
