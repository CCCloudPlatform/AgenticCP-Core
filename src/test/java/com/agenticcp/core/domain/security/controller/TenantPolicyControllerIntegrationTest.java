package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.security.dto.EffectivePolicySetDTO;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantPolicyController 통합 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("TenantPolicyController 통합 테스트")
class TenantPolicyControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private SecurityPolicyRepository policyRepository;
    
    private Tenant testTenant;
    
    @BeforeEach
    void setUp() {
        // 데이터 초기화
        policyRepository.deleteAll();
        tenantRepository.deleteAll();
        
        // 테스트 테넌트 생성
        testTenant = Tenant.builder()
                .tenantKey("TEST_TENANT")
                .tenantName("테스트 테넌트")
                .status(Status.ACTIVE)
                .build();
        testTenant = tenantRepository.save(testTenant);
        
        // 글로벌 정책 생성
        SecurityPolicy globalPolicy1 = SecurityPolicy.builder()
                .policyKey("GLOBAL_POLICY_1")
                .policyName("글로벌 정책 1")
                .policyType(SecurityPolicy.PolicyType.ACCESS_CONTROL)
                .priority(100)
                .status(Status.ACTIVE)
                .isGlobal(true)
                .isEnabled(true)
                .rules("{\"defaultAction\": \"DENY\"}")
                .build();
        policyRepository.save(globalPolicy1);
        
        SecurityPolicy globalPolicy2 = SecurityPolicy.builder()
                .policyKey("GLOBAL_POLICY_2")
                .policyName("글로벌 정책 2")
                .policyType(SecurityPolicy.PolicyType.AUTHENTICATION)
                .priority(200)
                .status(Status.ACTIVE)
                .isGlobal(true)
                .isEnabled(true)
                .rules("{\"requireMFA\": false}")
                .build();
        policyRepository.save(globalPolicy2);
        
        // 테넌트 정책 생성
        SecurityPolicy tenantPolicy = SecurityPolicy.builder()
                .policyKey("TENANT_POLICY_1")
                .policyName("테넌트 정책 1")
                .policyType(SecurityPolicy.PolicyType.DATA_PROTECTION)
                .priority(150)
                .tenant(testTenant)
                .status(Status.ACTIVE)
                .isGlobal(false)
                .isEnabled(true)
                .rules("{\"encryptAtRest\": true}")
                .build();
        policyRepository.save(tenantPolicy);
    }
    
    @AfterEach
    void tearDown() {
        // 데이터 정리
        policyRepository.deleteAll();
        tenantRepository.deleteAll();
    }
    
    @Test
    @DisplayName("테넌트 정책 조회 API 테스트")
    void getTenantPolicies_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId();
        
        // When
        ResponseEntity<ApiResponse<List<EffectivePolicySetDTO.PolicySummaryDTO>>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.GET, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<List<EffectivePolicySetDTO.PolicySummaryDTO>>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);  // 테넌트 정책만 1개
    }
    
    @Test
    @DisplayName("유효한 정책 조회 API 테스트 (글로벌 + 테넌트)")
    void getEffectivePolicies_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId() + "/effective";
        
        // When
        ResponseEntity<ApiResponse<EffectivePolicySetDTO>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.GET, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<EffectivePolicySetDTO>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        
        EffectivePolicySetDTO data = response.getBody().getData();
        assertThat(data.getGlobalPolicyCount()).isEqualTo(2);  // 글로벌 2개
        assertThat(data.getTenantPolicyCount()).isEqualTo(1);  // 테넌트 1개
        assertThat(data.getTotalPolicyCount()).isEqualTo(3);   // 총 3개
    }
    
    @Test
    @DisplayName("정책 타입별 조회 API 테스트")
    void getPoliciesByType_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId() + "/by-type/ACCESS_CONTROL";
        
        // When
        ResponseEntity<ApiResponse<List<SecurityPolicy>>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.GET, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<List<SecurityPolicy>>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);  // ACCESS_CONTROL 타입 1개
    }
    
    @Test
    @DisplayName("정렬된 정책 조회 API 테스트")
    void getSortedEffectivePolicies_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId() + "/sorted?ascending=false";
        
        // When
        ResponseEntity<ApiResponse<List<SecurityPolicy>>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.GET, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<List<SecurityPolicy>>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        
        List<SecurityPolicy> policies = response.getBody().getData();
        assertThat(policies).hasSize(3);
        
        // 우선순위 내림차순 확인
        for (int i = 0; i < policies.size() - 1; i++) {
            assertThat(policies.get(i).getPriority())
                    .isGreaterThanOrEqualTo(policies.get(i + 1).getPriority());
        }
    }
    
    @Test
    @DisplayName("기본 정책 초기화 API 테스트")
    void initializeDefaultPolicies_Success() {
        // Given
        Tenant newTenant = Tenant.builder()
                .tenantKey("NEW_TENANT")
                .tenantName("새 테넌트")
                .status(Status.ACTIVE)
                .build();
        newTenant = tenantRepository.save(newTenant);
        
        String url = "/api/security/tenant-policies/" + newTenant.getId() + "/initialize";
        
        // When
        ResponseEntity<ApiResponse<List<SecurityPolicy>>> response = 
                restTemplate.postForEntity(
                        url, 
                        null, 
                        (Class<ApiResponse<List<SecurityPolicy>>>) (Class<?>) ApiResponse.class
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        
        // DB 검증
        List<SecurityPolicy> policies = policyRepository.findByTenantIdAndIsEnabledTrue(newTenant.getId());
        assertThat(policies).hasSize(5);  // 5개 기본 정책 생성
    }
    
    @Test
    @DisplayName("정책 통계 조회 API 테스트")
    void getPolicyStatistics_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId() + "/statistics";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"totalPolicies\":3");
        assertThat(response.getBody()).contains("\"globalPolicies\":2");
        assertThat(response.getBody()).contains("\"tenantPolicies\":1");
    }
    
    @Test
    @DisplayName("캐시 무효화 API 테스트")
    void evictTenantPolicyCache_Success() {
        // Given
        String url = "/api/security/tenant-policies/" + testTenant.getId() + "/cache";
        
        // When
        ResponseEntity<ApiResponse<Void>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.DELETE, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<Void>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
    
    @Test
    @DisplayName("전체 캐시 무효화 API 테스트")
    void evictAllTenantPolicyCache_Success() {
        // Given
        String url = "/api/security/tenant-policies/cache/all";
        
        // When
        ResponseEntity<ApiResponse<Void>> response = 
                restTemplate.exchange(
                        url, 
                        HttpMethod.DELETE, 
                        null, 
                        new ParameterizedTypeReference<ApiResponse<Void>>() {}
                );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
    
    @Test
    @DisplayName("존재하지 않는 테넌트 조회 시 에러 처리")
    void getEffectivePolicies_WithNonExistentTenant_ReturnsError() {
        // Given
        String url = "/api/security/tenant-policies/999999/effective";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);
    }
}

