package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseHealthIndicator H2 데이터베이스 통합 테스트
 * 
 * 실제 H2 데이터베이스와 연결하여 DatabaseHealthIndicator의 동작을 검증합니다.
 * 
 * 테스트 시나리오:
 * - H2 데이터베이스와 실제 연결 테스트
 * - 데이터베이스 연결 상태 확인
 * - 실제 DataSource를 사용한 헬스체크
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DatabaseHealthIndicator H2 통합 테스트")
class DatabaseHealthIndicatorIntegrationTest {

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Test
    @DisplayName("H2 데이터베이스와 실제 연결하여 HEALTHY 상태를 반환해야 함")
    void check_WithRealH2Database_ShouldReturnHealthy() {
        // When
        HealthIndicatorResult result = databaseHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.getMessage()).isEqualTo("Database connection is valid");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("인디케이터 이름이 'database'로 반환되어야 함")
    void getName_ShouldReturnDatabase() {
        // When
        String name = databaseHealthIndicator.getName();

        // Then
        assertThat(name).isEqualTo("database");
    }
}
