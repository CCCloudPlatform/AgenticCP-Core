package com.agenticcp.core.domain.organization.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * OrganizationMember 복합 PK 클래스
 * 
 * <p>설계 B 기준: (organization_id, user_id)가 복합 PK</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberId implements Serializable {
    
    // JPA @IdClass 사용 시 엔티티의 필드명과 일치해야 함 (organization, user)
    private Long organization;
    private Long user;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationMemberId that = (OrganizationMemberId) o;
        return Objects.equals(organization, that.organization) &&
               Objects.equals(user, that.user);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(organization, user);
    }
}

