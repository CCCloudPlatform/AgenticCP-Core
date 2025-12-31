package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * OrganizationMember 엔티티
 * 
 * <p>조직과 사용자 간의 관계를 관리하는 엔티티입니다.
 * 설계 B 기준: (organization_id, user_id)가 복합 PK</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "organization_member", indexes = {
    @Index(name = "idx_org_member_org", columnList = "organization_id"),
    @Index(name = "idx_org_member_user", columnList = "user_id")
})
@IdClass(OrganizationMemberId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrganizationMember {

    /**
     * 조직 (복합 PK의 일부)
     */
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * 사용자 (복합 PK의 일부)
     */
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 조직 내 역할 (선택적)
     */
    @Column(name = "role", length = 50)
    private String role;

    /**
     * 가입일시
     */
    @Column(name = "joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /**
     * 생성일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

