package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * DatabaseHealthIndicator 테스트 클래스
 * 
 * 데이터베이스 연결 상태를 확인하는 헬스체크 인디케이터의 기능을 테스트합니다.
 * 
 * 테스트 시나리오:
 * - 정상적인 데이터베이스 연결 시 HEALTHY 상태 반환
 * - 연결이 유효하지 않을 때 CRITICAL 상태 반환
 * - SQLException 발생 시 CRITICAL 상태 반환
 * - 연결 자원이 적절히 해제되는지 확인
 */
@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private DatabaseHealthIndicator databaseHealthIndicator;

    @BeforeEach
    void setUp() {
        databaseHealthIndicator = new DatabaseHealthIndicator(dataSource);
    }

    /**
     * getName() 메서드 테스트
     * 
     * 데이터베이스 헬스체크 인디케이터의 이름이 "database"로 반환되는지 확인합니다.
     * 이는 헬스체크 시스템에서 컴포넌트를 식별하는 데 사용됩니다.
     */
    @Test
    void getName_ShouldReturnDatabase() {
        // When
        String name = databaseHealthIndicator.getName();

        // Then
        assertThat(name).isEqualTo("database");
    }

    /**
     * 데이터베이스 연결 테스트 - 정상 연결
     * 
     * 데이터베이스 연결이 유효할 때 HEALTHY 상태를 반환하는지 확인합니다.
     */
    @Test
    void check_WhenConnectionIsValid_ShouldReturnHealthy() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        // When
        HealthIndicatorResult result = databaseHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.getMessage()).isEqualTo("Database connection is valid");
        assertThat(result.getTimestamp()).isNotNull();

        verify(connection).close();
    }

    /**
     * 데이터베이스 연결 테스트 - 연결 실패
     * 
     * 데이터베이스 연결이 유효하지 않을 때 CRITICAL 상태를 반환하는지 확인합니다.
     */
    @Test
    void check_WhenConnectionIsInvalid_ShouldReturnCritical() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        // When
        HealthIndicatorResult result = databaseHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(result.getMessage()).isEqualTo("Database connection is invalid");
        assertThat(result.getTimestamp()).isNotNull();

        verify(connection).close();
    }

    /**
     * 데이터베이스 연결 테스트 - SQL 예외 발생
     * 
     * SQLException이 발생했을 때 CRITICAL 상태를 반환하는지 확인합니다.
     */
    @Test
    void check_WhenSQLException_ShouldReturnCritical() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        HealthIndicatorResult result = databaseHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(result.getMessage()).contains("Database connection failed");
        assertThat(result.getTimestamp()).isNotNull();
    }
}
