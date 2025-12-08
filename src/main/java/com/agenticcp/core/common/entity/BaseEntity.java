package com.agenticcp.core.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기본 엔티티 - 테넌트 정보 없음
 * 
 * <p>
 * 전역 기능(플랫폼 설정, 클라우드 제공자 등)에서 사용하는 베이스 엔티티입니다.
 * 모든 엔티티의 공통 필드(ID, 생성/수정 정보, 삭제 플래그)를 제공합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 엔티티 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 엔티티 생성 시각
     * Spring Data JPA Auditing에 의해 자동으로 설정됩니다.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정 시각
     * Spring Data JPA Auditing에 의해 자동으로 설정됩니다.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 엔티티 생성자 식별자 (사용자명 또는 시스템 ID)
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * 엔티티 최종 수정자 식별자 (사용자명 또는 시스템 ID)
     */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * 논리 삭제 플래그
     * true인 경우 삭제된 것으로 간주합니다.
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
