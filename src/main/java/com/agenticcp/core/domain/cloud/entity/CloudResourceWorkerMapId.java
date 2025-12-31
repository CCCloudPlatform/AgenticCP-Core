package com.agenticcp.core.domain.cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * CloudResourceWorkerMap 복합 PK 클래스
 * 
 * <p>설계 C 기준: (cloud_resource_id, worker_id) 복합 PK</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudResourceWorkerMapId implements Serializable {
    
    // JPA @IdClass 사용 시 엔티티의 필드명과 일치해야 함 (cloudResource, worker)
    private Long cloudResource;
    private Long worker;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudResourceWorkerMapId that = (CloudResourceWorkerMapId) o;
        return Objects.equals(cloudResource, that.cloudResource) &&
               Objects.equals(worker, that.worker);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cloudResource, worker);
    }
}

