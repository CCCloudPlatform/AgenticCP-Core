package com.agenticcp.core.domain.organization.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * WorkerRole 복합 PK 클래스
 * 
 * <p>설계 C 기준: (worker_id, role_id) 복합 PK
 * tenant_id는 제거되었습니다 (Role이 이미 tenant_id를 가짐)</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerRoleId implements Serializable {
    
    // JPA @IdClass 사용 시 엔티티의 필드명과 일치해야 함 (worker, role)
    private Long worker;
    private Long role;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerRoleId that = (WorkerRoleId) o;
        return Objects.equals(worker, that.worker) &&
               Objects.equals(role, that.role);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(worker, role);
    }
}

