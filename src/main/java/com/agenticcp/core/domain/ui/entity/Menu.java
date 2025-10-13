package com.agenticcp.core.domain.ui.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴 엔티티
 * 트리 구조로 구성된 메뉴 시스템을 관리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Entity
@Table(name = "menus", 
       uniqueConstraints = @UniqueConstraint(name = "uk_menu_key_tenant", columnNames = {"menu_key", "tenant_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu extends BaseEntity {

    /**
     * 메뉴 키 (테넌트 내에서 유일)
     */
    @Column(name = "menu_key", nullable = false, length = 100)
    private String menuKey;

    /**
     * 메뉴명
     */
    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    /**
     * 메뉴 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 메뉴 URL 경로
     */
    @Column(name = "url", length = 200)
    private String url;

    /**
     * 메뉴 아이콘
     */
    @Column(name = "icon", length = 50)
    private String icon;

    /**
     * 부모 메뉴 ID (트리 구조)
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 정렬 순서
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 시스템 메뉴 여부 (삭제 불가)
     */
    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    /**
     * 테넌트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * 메뉴 권한 매핑 목록
     */
    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<MenuPermission> permissions = new ArrayList<>();

    /**
     * 하위 메뉴 목록 (트리 구조)
     */
    @OneToMany(mappedBy = "parentId", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<Menu> children = new ArrayList<>();

    /**
     * 메뉴 레벨 계산
     */
    @Transient
    public int getLevel() {
        if (parentId == null) {
            return 1;
        }
        // 실제 구현에서는 부모 메뉴를 조회하여 레벨을 계산
        // 여기서는 단순화를 위해 1로 반환
        return 1;
    }

    /**
     * 최상위 메뉴 여부
     */
    @Transient
    public boolean isRoot() {
        return parentId == null;
    }

    /**
     * 리프 메뉴 여부 (하위 메뉴가 없는 메뉴)
     */
    @Transient
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 메뉴 경로 생성 (예: 홈 > 대시보드 > 사용자관리)
     */
    @Transient
    public String getMenuPath() {
        if (isRoot()) {
            return menuName;
        }
        // 실제 구현에서는 부모 메뉴들을 조회하여 경로를 구성
        return menuName;
    }
}
