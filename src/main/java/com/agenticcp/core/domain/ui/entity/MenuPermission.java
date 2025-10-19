package com.agenticcp.core.domain.ui.entity;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.user.entity.Permission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메뉴-권한 매핑 엔티티
 * 메뉴에 대한 접근 권한을 관리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Entity
@Table(name = "menu_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuPermission extends BaseEntity {

    /**
     * 메뉴
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /**
     * 권한
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * 접근 타입 (READ, WRITE, DELETE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    @Builder.Default
    private AccessType accessType = AccessType.READ;

    /**
     * 접근 타입 열거형
     */
    public enum AccessType {
        /**
         * 읽기 권한
         */
        READ,
        
        /**
         * 쓰기 권한
         */
        WRITE,
        
        /**
         * 삭제 권한
         */
        DELETE
    }

    /**
     * 접근 타입 설명
     */
    @Transient
    public String getAccessTypeDescription() {
        return switch (accessType) {
            case READ -> "조회";
            case WRITE -> "수정";
            case DELETE -> "삭제";
        };
    }
}
