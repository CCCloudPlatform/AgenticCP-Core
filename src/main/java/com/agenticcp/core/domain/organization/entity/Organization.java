package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * 조직 엔티티
 * 
 * <p>조직 정보를 관리하는 엔티티입니다.
 * ERD 기준: id, name, created_at</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_name", columnList = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Organization extends BaseEntity {

    /** 조직명 (ERD: name) */
    @NotBlank(message = "조직명은 필수입니다")
    @Size(max = 255, message = "조직명은 255자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
