package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cloud Resource 엔티티 (쿠버네티스 스타일)
 * 
 * <p>리소스를 메타데이터가 있는 일반적인 컨테이너로 취급하고,
 * 구체적인 설정은 구조화된 JSON으로 저장합니다.</p>
 * 
 * <p>기본 필드: resourceId, name, provider, region, type, labels (모든 리소스 공통)
 * 확장 필드: properties (Spec), status (Status) - JSON 형태</p>
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 * @since 2025-01-XX
 */
@Entity
@Table(name = "cloud_resources", indexes = {
    @Index(name = "idx_resource_id", columnList = "resource_id"),
    @Index(name = "idx_resource_tenant", columnList = "tenant_id"),
    @Index(name = "idx_resource_type", columnList = "type"),
    @Index(name = "idx_resource_tenant_type", columnList = "tenant_id, type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudResource extends BaseEntity {

    // ==================== 공통 식별자 ====================
    
    /**
     * 리소스 ID (CSP에서 부여한 고유 ID)
     */
    @Column(name = "resource_id", nullable = false, unique = true, length = 255)
    private String resourceId;

    /**
     * 리소스 이름
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 클라우드 프로바이더 (AWS, GCP, Azure 등)
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * 리전
     */
    @Column(name = "region", nullable = false, length = 50)
    private String region;

    /**
     * 리소스 타입 (INSTANCE, CLUSTER, BUCKET 등)
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /**
     * 테넌트 (리소스 소유 테넌트)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // ==================== 확장 필드 (JSON) ====================
    
    /**
     * 쿠버네티스 Spec - 선언된 상태/설정 (리소스 타입별 설정 정보)
     * JSON 형태로 저장: { "cpuCores": 4, "memoryGb": 8, "instanceType": "t3.medium" }
     */
    @Column(name = "properties", columnDefinition = "JSON")
    private String properties;

    /**
     * 쿠버네티스 Status - 관측된 상태/런타임 정보
     * JSON 형태로 저장: { "state": "running", "ipAddress": "10.0.0.1", "costPerHour": 0.05 }
     */
    @Column(name = "status", columnDefinition = "JSON")
    private String status;

    /**
     * 태그/라벨 (JSON Map)
     * JSON 형태로 저장: { "environment": "production", "team": "backend" }
     */
    @Column(name = "labels", columnDefinition = "JSON")
    private String labels;

    // ==================== 내부 Enum ====================

    /**
     * 리소스 타입
     */
    public enum ResourceType {
        INSTANCE,
        NETWORK,
        BUCKET,
        CLUSTER,
        DATABASE,
        LOAD_BALANCER,
        SECURITY_GROUP,
        SUBNET,
        ROUTE_TABLE,
        INTERNET_GATEWAY,
        NAT_GATEWAY,
        VPC_ENDPOINT,
        OTHER
    }

    /**
     * 리소스 생명주기 상태
     */
    public enum LifecycleState {
        UNKNOWN,
        PENDING,
        RUNNING,
        STOPPED,
        TERMINATED;

        /**
         * LifecycleState를 소문자 문자열로 변환합니다.
         * 
         * @return 소문자 상태 문자열 (예: "running", "stopped")
         */
        public String toLowerCase() {
            return name().toLowerCase();
        }
    }
}
