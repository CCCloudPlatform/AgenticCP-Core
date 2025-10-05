package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantCollectorConfigDto 검증 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("TenantCollectorConfigDto 검증 테스트")
class TenantCollectorConfigDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 DTO - 모든 검증 통과")
    void validateDto_ValidData_ShouldPass() {
        // Given - 모든 필드가 유효한 값으로 설정된 DTO를 생성하는 상황
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant-123")  // 유효한 테넌트 ID (50자 이하)
                .collectorType(CollectorType.SYSTEM)  // 유효한 수집기 타입
                .isEnabled(true)  // 활성화 상태
                .collectionInterval(60000L)  // 유효한 수집 주기 (1분, 최대 1분 이하)
                .retryCount(3)  // 유효한 재시도 횟수 (1-10 범위)
                .timeout(30000L)  // 유효한 타임아웃 (양수)
                .priority(1)  // 유효한 우선순위 (1-1000 범위)
                .dailyMetricLimit(10000L)  // 유효한 일일 메트릭 제한 (양수)
                .storageQuotaMb(1000L)  // 유효한 저장 공간 할당량 (양수)
                .build();

        // When - Jakarta Validation을 통해 DTO의 유효성을 검증하는 경우
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then - 모든 검증 규칙을 통과하므로 검증 오류가 없어야 함
        assertThat(violations).isEmpty();  // 검증 오류가 없는지 확인
    }

    @Test
    @DisplayName("테넌트 ID 검증 실패 - 빈 문자열")
    void validateDto_BlankTenantId_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("")  // 빈 문자열
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("테넌트 ID는 필수입니다");
    }

    @Test
    @DisplayName("테넌트 ID 검증 실패 - null")
    void validateDto_NullTenantId_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId(null)  // null
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("테넌트 ID는 필수입니다");
    }

    @Test
    @DisplayName("테넌트 ID 검증 실패 - 길이 초과")
    void validateDto_TenantIdTooLong_ShouldFail() {
        // Given
        String longTenantId = "a".repeat(51);  // 51자 (최대 50자 초과)
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId(longTenantId)
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("테넌트 ID는 50자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("수집기 타입 검증 실패 - null")
    void validateDto_NullCollectorType_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(null)  // null
                .collectionInterval(60000L)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("수집기 타입은 필수입니다");
    }

    @Test
    @DisplayName("수집 주기 검증 실패 - 최소값 미만")
    void validateDto_CollectionIntervalTooSmall_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(500L)  // 500ms (최소 1000ms 미만)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("수집 주기는 최소 1초(1000ms) 이상이어야 합니다");
    }

    @Test
    @DisplayName("수집 주기 검증 실패 - 최대값 초과")
    void validateDto_CollectionIntervalTooLarge_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(90000000L)  // 25시간 (최대 24시간 초과)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("메트릭 수집 주기는 1분(60000ms) 이하여야 합니다");
    }

    @Test
    @DisplayName("재시도 횟수 검증 실패 - 최소값 미만")
    void validateDto_RetryCountTooSmall_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .retryCount(-1)  // 음수
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("재시도 횟수는 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("재시도 횟수 검증 실패 - 최대값 초과")
    void validateDto_RetryCountTooLarge_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .retryCount(11)  // 최대값 10 초과
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("재시도 횟수는 10 이하여야 합니다");
    }

    @Test
    @DisplayName("타임아웃 검증 실패 - 최소값 미만")
    void validateDto_TimeoutTooSmall_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .timeout(500L)  // 500ms (최소 1000ms 미만)
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("타임아웃은 최소 1초(1000ms) 이상이어야 합니다");
    }

    @Test
    @DisplayName("우선순위 검증 실패 - 최소값 미만")
    void validateDto_PriorityTooSmall_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .priority(0)  // 최소값 1 미만
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("우선순위는 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("우선순위 검증 실패 - 최대값 초과")
    void validateDto_PriorityTooLarge_ShouldFail() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("test-tenant")
                .collectorType(CollectorType.SYSTEM)
                .collectionInterval(60000L)
                .priority(1001)  // 최대값 1000 초과
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("우선순위는 1000 이하여야 합니다");
    }

    @Test
    @DisplayName("여러 검증 실패 - 복합 오류")
    void validateDto_MultipleValidationFailures_ShouldReturnAllErrors() {
        // Given
        TenantCollectorConfigDto dto = TenantCollectorConfigDto.builder()
                .tenantId("")  // 빈 문자열
                .collectorType(null)  // null
                .collectionInterval(-1000L)  // 음수
                .retryCount(-1)  // 음수
                .priority(0)  // 최소값 미만
                .build();

        // When
        Set<ConstraintViolation<TenantCollectorConfigDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(5);
        
        // 각 오류 메시지 확인
        String[] expectedMessages = {
                "테넌트 ID는 필수입니다",
                "수집기 타입은 필수입니다", 
                "수집 주기는 최소 1초 이상이어야 합니다",
                "재시도 횟수는 0 이상이어야 합니다",
                "우선순위는 1 이상이어야 합니다"
        };
        
        Set<String> actualMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessages).anyMatch(msg -> msg.contains(expectedMessage.split("은|는")[0]));
        }
    }
}
