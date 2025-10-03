package com.agenticcp.core.domain.monitoring.health.exception;

import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HealthCheckException 단위 테스트
 *
 * HealthCheckException의 생성자와 에러 코드 처리를 검증합니다.
 * 
 * 테스트 시나리오:
 * - 기본 생성자 테스트
 * - 커스텀 메시지 생성자 테스트
 * - 에러 코드 및 HTTP 상태 코드 검증
 */
@DisplayName("HealthCheckException 단위 테스트")
class HealthCheckExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외를 생성하면 에러 코드와 기본 메시지를 반환해야 함")
    void constructor_WithErrorCode_ShouldSetErrorCodeAndDefaultMessage() {
        // When
        HealthCheckException exception = new HealthCheckException(MonitoringErrorCode.HEALTH_CHECK_FAILED);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(MonitoringErrorCode.HEALTH_CHECK_FAILED);
        assertThat(exception.getMessage()).isEqualTo(MonitoringErrorCode.HEALTH_CHECK_FAILED.getMessage());
        assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("MONITORING_8081");
    }

    @Test
    @DisplayName("커스텀 메시지 생성자로 예외를 생성하면 에러 코드와 커스텀 메시지를 반환해야 함")
    void constructor_WithErrorCodeAndMessage_ShouldSetErrorCodeAndCustomMessage() {
        // Given
        String customMessage = "헬스체크 수행 중 오류가 발생했습니다.";

        // When
        HealthCheckException exception = new HealthCheckException(
            MonitoringErrorCode.HEALTH_INDICATOR_ERROR, 
            customMessage
        );

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(MonitoringErrorCode.HEALTH_INDICATOR_ERROR);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
        assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("MONITORING_8083");
    }

    @Test
    @DisplayName("다양한 MonitoringErrorCode로 예외를 생성할 수 있어야 함")
    void constructor_WithDifferentErrorCodes_ShouldWorkCorrectly() {
        // Test HEALTH_CHECK_FAILED
        HealthCheckException healthCheckException = new HealthCheckException(MonitoringErrorCode.HEALTH_CHECK_FAILED);
        assertThat(healthCheckException.getErrorCode()).isEqualTo(MonitoringErrorCode.HEALTH_CHECK_FAILED);
        assertThat(healthCheckException.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // Test HEALTH_INDICATOR_ERROR
        HealthCheckException indicatorException = new HealthCheckException(MonitoringErrorCode.HEALTH_INDICATOR_ERROR);
        assertThat(indicatorException.getErrorCode()).isEqualTo(MonitoringErrorCode.HEALTH_INDICATOR_ERROR);
        assertThat(indicatorException.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Test CACHE_EVICTION_FAILED
        HealthCheckException cacheException = new HealthCheckException(MonitoringErrorCode.CACHE_EVICTION_FAILED);
        assertThat(cacheException.getErrorCode()).isEqualTo(MonitoringErrorCode.CACHE_EVICTION_FAILED);
        assertThat(cacheException.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("예외는 RuntimeException을 상속받아야 함")
    void exception_ShouldExtendRuntimeException() {
        // When
        HealthCheckException exception = new HealthCheckException(MonitoringErrorCode.HEALTH_CHECK_FAILED);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
