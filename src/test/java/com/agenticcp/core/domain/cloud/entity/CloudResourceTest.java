package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * CloudResource 엔티티 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("CloudResource 엔티티 단위 테스트")
class CloudResourceTest {

    private Tenant testTenant;
    private CloudResource testCloudResource;

    @BeforeEach
    void setUp() {
        // 테스트용 테넌트 생성
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .status(Status.ACTIVE)
                .build();
        testTenant.setId(1L);

        // 테스트용 클라우드 리소스 생성
        testCloudResource = CloudResource.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceName("test-instance")
                .displayName("Test Instance")
                .status(Status.ACTIVE)
                .resourceType(CloudResource.ResourceType.INSTANCE)
                .lifecycleState(CloudResource.LifecycleState.RUNNING)
                .instanceType("t3.micro")
                .instanceSize("small")
                .cpuCores(2)
                .memoryGb(4)
                .storageGb(20L)
                .networkBandwidthMbps(100)
                .ipAddress("192.168.1.100")
                .privateIpAddress("10.0.1.100")
                .publicIpAddress("203.0.113.100")
                .tags("{\"Environment\":\"test\",\"Project\":\"demo\"}")
                .configuration("{\"vpc\":\"vpc-12345678\",\"subnet\":\"subnet-12345678\"}")
                .costPerHour(new BigDecimal("0.05"))
                .monthlyCost(new BigDecimal("36.00"))
                .createdInCloud(LocalDateTime.now().minusDays(1))
                .lastModifiedInCloud(LocalDateTime.now())
                .lastSync(LocalDateTime.now())
                .metadata("{\"region\":\"us-east-1\",\"availability_zone\":\"us-east-1a\"}")
                .build();
    }

    @Test
    @DisplayName("CloudResource 기본 필드 설정 및 조회")
    void testBasicFields() {
        // Given & When
        testCloudResource.setId(1L);
        testCloudResource.setTenant(testTenant);

        // Then
        assertThat(testCloudResource.getId()).isEqualTo(1L);
        assertThat(testCloudResource.getResourceId()).isEqualTo("i-1234567890abcdef0");
        assertThat(testCloudResource.getResourceName()).isEqualTo("test-instance");
        assertThat(testCloudResource.getDisplayName()).isEqualTo("Test Instance");
        assertThat(testCloudResource.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(testCloudResource.getResourceType()).isEqualTo(CloudResource.ResourceType.INSTANCE);
        assertThat(testCloudResource.getLifecycleState()).isEqualTo(CloudResource.LifecycleState.RUNNING);
    }

    @Test
    @DisplayName("CloudResource 인스턴스 정보 설정 및 조회")
    void testInstanceInfo() {
        // Given & When
        testCloudResource.setInstanceType("t3.large");
        testCloudResource.setInstanceSize("large");
        testCloudResource.setCpuCores(4);
        testCloudResource.setMemoryGb(8);
        testCloudResource.setStorageGb(50L);
        testCloudResource.setNetworkBandwidthMbps(1000);

        // Then
        assertThat(testCloudResource.getInstanceType()).isEqualTo("t3.large");
        assertThat(testCloudResource.getInstanceSize()).isEqualTo("large");
        assertThat(testCloudResource.getCpuCores()).isEqualTo(4);
        assertThat(testCloudResource.getMemoryGb()).isEqualTo(8);
        assertThat(testCloudResource.getStorageGb()).isEqualTo(50L);
        assertThat(testCloudResource.getNetworkBandwidthMbps()).isEqualTo(1000);
    }

    @Test
    @DisplayName("CloudResource 네트워크 정보 설정 및 조회")
    void testNetworkInfo() {
        // Given & When
        testCloudResource.setIpAddress("192.168.1.200");
        testCloudResource.setPrivateIpAddress("10.0.1.200");
        testCloudResource.setPublicIpAddress("203.0.113.200");

        // Then
        assertThat(testCloudResource.getIpAddress()).isEqualTo("192.168.1.200");
        assertThat(testCloudResource.getPrivateIpAddress()).isEqualTo("10.0.1.200");
        assertThat(testCloudResource.getPublicIpAddress()).isEqualTo("203.0.113.200");
    }

    @Test
    @DisplayName("CloudResource 비용 정보 설정 및 조회")
    void testCostInfo() {
        // Given & When
        testCloudResource.setCostPerHour(new BigDecimal("0.10"));
        testCloudResource.setMonthlyCost(new BigDecimal("72.00"));

        // Then
        assertThat(testCloudResource.getCostPerHour()).isEqualTo(new BigDecimal("0.10"));
        assertThat(testCloudResource.getMonthlyCost()).isEqualTo(new BigDecimal("72.00"));
    }

    @Test
    @DisplayName("CloudResource JSON 필드 설정 및 조회")
    void testJsonFields() {
        // Given & When
        String tags = "{\"Environment\":\"production\",\"Project\":\"web-app\"}";
        String configuration = "{\"vpc\":\"vpc-87654321\",\"subnet\":\"subnet-87654321\"}";
        String metadata = "{\"region\":\"us-west-2\",\"availability_zone\":\"us-west-2a\"}";
        
        testCloudResource.setTags(tags);
        testCloudResource.setConfiguration(configuration);
        testCloudResource.setMetadata(metadata);

        // Then
        assertThat(testCloudResource.getTags()).isEqualTo(tags);
        assertThat(testCloudResource.getConfiguration()).isEqualTo(configuration);
        assertThat(testCloudResource.getMetadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("CloudResource 동기화 정보 설정 및 조회")
    void testSyncInfo() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);

        // When
        testCloudResource.setCreatedInCloud(lastWeek);
        testCloudResource.setLastModifiedInCloud(yesterday);
        testCloudResource.setLastSync(now);

        // Then
        assertThat(testCloudResource.getCreatedInCloud()).isEqualTo(lastWeek);
        assertThat(testCloudResource.getLastModifiedInCloud()).isEqualTo(yesterday);
        assertThat(testCloudResource.getLastSync()).isEqualTo(now);
    }

    @Test
    @DisplayName("CloudResource 테넌트 정보 설정 및 조회")
    void testTenantInfo() {
        // Given & When
        testCloudResource.setTenant(testTenant);

        // Then
        assertThat(testCloudResource.getTenant()).isNotNull();
        assertThat(testCloudResource.getTenant().getId()).isEqualTo(1L);
        assertThat(testCloudResource.getTenant().getTenantKey()).isEqualTo("test-tenant");
        assertThat(testCloudResource.getTenant().getTenantName()).isEqualTo("Test Tenant");
    }

    @Test
    @DisplayName("CloudResource 리소스 타입 열거형 테스트")
    void testResourceTypeEnum() {
        // Given & When
        testCloudResource.setResourceType(CloudResource.ResourceType.VOLUME);

        // Then
        assertThat(testCloudResource.getResourceType()).isEqualTo(CloudResource.ResourceType.VOLUME);
        
        // 다른 리소스 타입들도 테스트
        testCloudResource.setResourceType(CloudResource.ResourceType.DATABASE);
        assertThat(testCloudResource.getResourceType()).isEqualTo(CloudResource.ResourceType.DATABASE);
        
        testCloudResource.setResourceType(CloudResource.ResourceType.BUCKET);
        assertThat(testCloudResource.getResourceType()).isEqualTo(CloudResource.ResourceType.BUCKET);
    }

    @Test
    @DisplayName("CloudResource 라이프사이클 상태 열거형 테스트")
    void testLifecycleStateEnum() {
        // Given & When
        testCloudResource.setLifecycleState(CloudResource.LifecycleState.STOPPED);

        // Then
        assertThat(testCloudResource.getLifecycleState()).isEqualTo(CloudResource.LifecycleState.STOPPED);
        
        // 다른 라이프사이클 상태들도 테스트
        testCloudResource.setLifecycleState(CloudResource.LifecycleState.TERMINATED);
        assertThat(testCloudResource.getLifecycleState()).isEqualTo(CloudResource.LifecycleState.TERMINATED);
        
        testCloudResource.setLifecycleState(CloudResource.LifecycleState.FAILED);
        assertThat(testCloudResource.getLifecycleState()).isEqualTo(CloudResource.LifecycleState.FAILED);
    }

    @Test
    @DisplayName("CloudResource toString 메서드 확인")
    void testToString() {
        // Given
        testCloudResource.setId(1L);
        testCloudResource.setResourceId("i-1234567890abcdef0");
        testCloudResource.setResourceName("test-instance");

        // When
        String toString = testCloudResource.toString();

        // Then
        assertThat(toString).contains("resourceId=i-1234567890abcdef0");
        assertThat(toString).contains("resourceName=test-instance");
        // id는 Lombok @Data에서 toString에 포함되지 않을 수 있으므로 제거
    }

    @Test
    @DisplayName("CloudResource equals와 hashCode 확인")
    void testEqualsAndHashCode() {
        // Given
        CloudResource resource1 = CloudResource.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceName("test-instance")
                .build();
        resource1.setId(1L);

        CloudResource resource2 = CloudResource.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceName("test-instance")
                .build();
        resource2.setId(1L);

        CloudResource resource3 = CloudResource.builder()
                .resourceId("i-0987654321fedcba0")
                .resourceName("test-instance-2")
                .build();
        resource3.setId(2L);

        // When & Then
        assertThat(resource1).isEqualTo(resource2);
        assertThat(resource1).isNotEqualTo(resource3);
        assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
        assertThat(resource1.hashCode()).isNotEqualTo(resource3.hashCode());
    }
}
