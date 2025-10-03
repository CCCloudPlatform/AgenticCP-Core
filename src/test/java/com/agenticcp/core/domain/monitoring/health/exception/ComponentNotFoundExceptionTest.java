package com.agenticcp.core.domain.monitoring.health.exception;

import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ComponentNotFoundException 단위 테스트
 *
 * ComponentNotFoundException의 생성자와 에러 코드 처리를 검증합니다.
 * 
 * 테스트 시나리오:
 * - 컴포넌트 이름을 포함한 기본 생성자 테스트
 * - 커스텀 메시지 생성자 테스트
 * - 에러 코드 및 HTTP 상태 코드 검증
 */
@DisplayName("ComponentNotFoundException 단위 테스트")
class ComponentNotFoundExceptionTest {

    @Test
    @DisplayName("컴포넌트 이름으로 예외를 생성하면 적절한 메시지를 반환해야 함")
    void constructor_WithComponentName_ShouldSetCorrectMessage() {
        // Given
        String componentName = "database";

        // When
        ComponentNotFoundException exception = new ComponentNotFoundException(componentName);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(MonitoringErrorCode.COMPONENT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Component 'database' not found");
        assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("MONITORING_8082");
    }

    @Test
    @DisplayName("다양한 컴포넌트 이름으로 예외를 생성할 수 있어야 함")
    void constructor_WithDifferentComponentNames_ShouldFormatMessageCorrectly() {
        // Test with "system" component
        ComponentNotFoundException systemException = new ComponentNotFoundException("system");
        assertThat(systemException.getMessage()).isEqualTo("Component 'system' not found");

        // Test with "application" component
        ComponentNotFoundException appException = new ComponentNotFoundException("application");
        assertThat(appException.getMessage()).isEqualTo("Component 'application' not found");

        // Test with "nonexistent" component
        ComponentNotFoundException unknownException = new ComponentNotFoundException("nonexistent");
        assertThat(unknownException.getMessage()).isEqualTo("Component 'nonexistent' not found");
    }

    @Test
    @DisplayName("커스텀 메시지 생성자로 예외를 생성하면 커스텀 메시지를 반환해야 함")
    void constructor_WithCustomMessage_ShouldSetCustomMessage() {
        // Given
        String componentName = "database";
        String customMessage = "데이터베이스 컴포넌트를 찾을 수 없습니다.";

        // When
        ComponentNotFoundException exception = new ComponentNotFoundException(componentName, customMessage);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(MonitoringErrorCode.COMPONENT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
        assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("MONITORING_8082");
    }

    @Test
    @DisplayName("예외는 RuntimeException을 상속받아야 함")
    void exception_ShouldExtendRuntimeException() {
        // When
        ComponentNotFoundException exception = new ComponentNotFoundException("test");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("예외는 BusinessException을 상속받아야 함")
    void exception_ShouldExtendBusinessException() {
        // When
        ComponentNotFoundException exception = new ComponentNotFoundException("test");

        // Then
        assertThat(exception).isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class);
    }

    @Test
    @DisplayName("빈 문자열 컴포넌트 이름으로도 예외를 생성할 수 있어야 함")
    void constructor_WithEmptyComponentName_ShouldWork() {
        // When
        ComponentNotFoundException exception = new ComponentNotFoundException("");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Component '' not found");
        assertThat(exception.getErrorCode()).isEqualTo(MonitoringErrorCode.COMPONENT_NOT_FOUND);
    }
}
