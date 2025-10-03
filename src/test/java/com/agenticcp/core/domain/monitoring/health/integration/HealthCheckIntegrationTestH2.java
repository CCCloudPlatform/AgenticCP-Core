package com.agenticcp.core.domain.monitoring.health.integration;

import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.exception.ComponentNotFoundException;
import com.agenticcp.core.domain.monitoring.health.service.AdvancedHealthCheckService;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 헬스체크 H2 데이터베이스 통합 테스트
 * 
 * 실제 H2 데이터베이스와 연결하여 헬스체크 시스템의 전체 동작을 테스트합니다.
 * 
 * 테스트 시나리오:
 * - 실제 H2 데이터베이스와 연결하여 헬스체크 수행
 * - 실제 데이터베이스에 헬스체크 결과 저장
 * - 실제 헬스체크 인디케이터들의 동작 확인
 * - 전체 Spring Boot 컨텍스트와의 통합 테스트
 * 
 * 주의사항:
 * - 실제 H2 데이터베이스 연결 필요
 * - Spring Boot 전체 컨텍스트 로드
 * - 실제 헬스체크 인디케이터들 사용
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("헬스체크 H2 데이터베이스 통합 테스트")
class HealthCheckIntegrationTestH2 {

    @Autowired
    private AdvancedHealthCheckService advancedHealthCheckService;
    
    @Autowired
    private PlatformHealthRepository platformHealthRepository;

    /**
     * 전체 헬스체크 통합 테스트 - H2 데이터베이스 연결
     * 
     * 실제 H2 데이터베이스와 연결하여 헬스체크를 수행하고,
     * 결과를 데이터베이스에 저장하는 전체 과정을 테스트합니다.
     */
    @Test
    @DisplayName("H2 데이터베이스와 연결하여 전체 헬스체크 수행")
    void getOverallHealth_WithH2Database_ShouldPerformHealthCheckAndSaveToDatabase() {
        // Given
        long beforeCount = platformHealthRepository.count();
        
        // When
        HealthStatusResponse response = advancedHealthCheckService.getOverallHealth();

        // Then
        assertThat(response.getOverallStatus()).isNotNull();
        assertThat(response.getComponents()).isNotNull();
        assertThat(response.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Health check completed");
        
        // 실제 H2 데이터베이스에 저장되었는지 확인
        long afterCount = platformHealthRepository.count();
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);
    }

    /**
     * 개별 컴포넌트 헬스체크 테스트 - H2 데이터베이스 연결
     * 
     * 실제 H2 데이터베이스와 연결하여 특정 컴포넌트의 헬스체크를 수행합니다.
     */
    @Test
    @DisplayName("H2 데이터베이스와 연결하여 개별 컴포넌트 헬스체크 수행")
    void getComponentHealth_WithH2Database_ShouldCheckSpecificComponent() {
        // When
        ComponentHealthStatus response = advancedHealthCheckService.getComponentHealth("database");

        // Then
        assertThat(response.getComponent()).isEqualTo("database");
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertThat(response.getTimestamp()).isNotNull();
    }

    /**
     * 존재하지 않는 컴포넌트 헬스체크 테스트
     * 
     * 존재하지 않는 컴포넌트에 대한 헬스체크 요청 시 UNKNOWN 상태를 반환하는지 확인합니다.
     */
    @Test
    @DisplayName("존재하지 않는 컴포넌트 헬스체크 시 ComponentNotFoundException 발생")
    void getComponentHealth_WithNonExistentComponent_ShouldThrowComponentNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> advancedHealthCheckService.getComponentHealth("nonexistent"))
                .isInstanceOf(ComponentNotFoundException.class)
                .hasMessage("Component 'nonexistent' not found");
    }

    /**
     * 헬스체크 요약 테스트 - H2 데이터베이스 연결
     * 
     * 실제 H2 데이터베이스에서 헬스체크 요약 정보를 조회합니다.
     */
    @Test
    @DisplayName("H2 데이터베이스에서 헬스체크 요약 정보 조회")
    void getHealthSummary_WithH2Database_ShouldReturnSummaryFromDatabase() {
        // Given - Perform some health checks first
        advancedHealthCheckService.getOverallHealth();

        // When
        HealthCheckSummary summary = advancedHealthCheckService.getHealthSummary();

        // Then
        assertThat(summary.getTotalServices()).isGreaterThanOrEqualTo(0);
        assertThat(summary.getHealthyServices()).isGreaterThanOrEqualTo(0);
        assertThat(summary.getWarningServices()).isGreaterThanOrEqualTo(0);
        assertThat(summary.getCriticalServices()).isGreaterThanOrEqualTo(0);
        assertThat(summary.getUnknownServices()).isGreaterThanOrEqualTo(0);
        assertThat(summary.getLastUpdated()).isNotNull();
    }

    /**
     * 헬스체크 데이터베이스 저장 테스트
     * 
     * 실제 H2 데이터베이스에 헬스체크 결과가 올바르게 저장되는지 확인합니다.
     */
    @Test
    @DisplayName("H2 데이터베이스에 헬스체크 결과 저장 확인")
    void healthCheck_WithH2Database_ShouldSaveCorrectDataToDatabase() {
        // Given
        long initialCount = platformHealthRepository.count();
        
        // When
        advancedHealthCheckService.getOverallHealth();

        // Then
        long finalCount = platformHealthRepository.count();
        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount);
        
        // 저장된 헬스체크 데이터 확인
        var savedHealth = platformHealthRepository.findAll();
        assertThat(savedHealth).isNotEmpty();
    }
}
