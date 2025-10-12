package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
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
 * QuotaRequestDto 검증 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("QuotaRequestDto 검증 테스트")
class QuotaRequestDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 할당량 요청 - 모든 검증 통과")
    void validateQuotaRequest_ValidData_ShouldPass() {
        // 테스트 목적: 모든 필드가 유효한 값으로 설정된 QuotaRequestDto가 검증을 통과하는지 확인
        // 검증 대상: @NotNull, @Min, @Max 어노테이션이 올바르게 작동하는지
        
        // Given - 유효한 할당량 데이터 생성
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000L)  // 1~10,000,000 범위 내
                .storageQuotaMb(100L)     // 1~1,048,576 범위 내
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)  // null이 아닌 enum 값
                .build();

        // When - DTO 검증 실행
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then - 검증 오류가 없음을 확인
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("일일 메트릭 수집량 제한 검증 실패 - null")
    void validateQuotaRequest_NullDailyMetricLimit_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(null)  // null
                .storageQuotaMb(100L)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("일일 메트릭 수집량 제한은 필수입니다");
    }

    @Test
    @DisplayName("일일 메트릭 수집량 제한 검증 실패 - 최소값 미만")
    void validateQuotaRequest_DailyMetricLimitTooSmall_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(0L)  // 0 (최소값 1 미만)
                .storageQuotaMb(100L)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("일일 메트릭 수집량 제한은 최소 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("일일 메트릭 수집량 제한 검증 실패 - 최대값 초과")
    void validateQuotaRequest_DailyMetricLimitTooLarge_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(20000000L)  // 20,000,000 (최대값 10,000,000 초과)
                .storageQuotaMb(100L)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("일일 메트릭 수집량 제한은 최대 10,000,000 이하여야 합니다");
    }

    @Test
    @DisplayName("저장 공간 할당량 검증 실패 - null")
    void validateQuotaRequest_NullStorageQuotaMb_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000L)
                .storageQuotaMb(null)  // null
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("메트릭 저장 공간 할당량은 필수입니다");
    }

    @Test
    @DisplayName("저장 공간 할당량 검증 실패 - 최소값 미만")
    void validateQuotaRequest_StorageQuotaMbTooSmall_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000L)
                .storageQuotaMb(0L)  // 0MB (최소값 1MB 미만)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("저장 공간 할당량은 최소 1MB 이상이어야 합니다");
    }

    @Test
    @DisplayName("저장 공간 할당량 검증 실패 - 최대값 초과")
    void validateQuotaRequest_StorageQuotaMbTooLarge_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000L)
                .storageQuotaMb(2000000L)  // 2TB (최대값 1TB 초과)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("저장 공간 할당량은 최대 1TB(1048576MB) 이하여야 합니다");
    }

    @Test
    @DisplayName("할당량 초과 시 동작 검증 실패 - null")
    void validateQuotaRequest_NullQuotaExceededAction_ShouldFail() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000L)
                .storageQuotaMb(100L)
                .quotaExceededAction(null)  // null
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("할당량 초과 시 동작은 필수입니다");
    }

    @Test
    @DisplayName("경계값 테스트 - 최소값")
    void validateQuotaRequest_MinimumValues_ShouldPass() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1L)  // 최소값
                .storageQuotaMb(1L)    // 최소값
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 테스트 - 최대값")
    void validateQuotaRequest_MaximumValues_ShouldPass() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(10000000L)  // 최대값
                .storageQuotaMb(1048576L)     // 최대값 (1TB)
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("모든 QuotaExceededAction 값 테스트")
    void validateQuotaRequest_AllQuotaExceededActions_ShouldPass() {
        // Given & When & Then
        for (QuotaExceededAction action : QuotaExceededAction.values()) {
            QuotaRequestDto dto = QuotaRequestDto.builder()
                    .dailyMetricLimit(1000L)
                    .storageQuotaMb(100L)
                    .quotaExceededAction(action)
                    .build();

            Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("여러 검증 실패 - 복합 오류")
    void validateQuotaRequest_MultipleValidationFailures_ShouldReturnAllErrors() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(null)  // null
                .storageQuotaMb(-1L)     // 음수
                .quotaExceededAction(null)  // null
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(3);
        
        // 각 오류 메시지 확인
        Set<String> violationMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
        
        assertThat(violationMessages).contains("일일 메트릭 수집량 제한은 필수입니다");
        assertThat(violationMessages).contains("저장 공간 할당량은 최소 1MB 이상이어야 합니다");
        assertThat(violationMessages).contains("할당량 초과 시 동작은 필수입니다");
    }

    @Test
    @DisplayName("실제 사용 시나리오 테스트 - 소규모 환경")
    void validateQuotaRequest_SmallEnvironmentScenario_ShouldPass() {
        // Given - 소규모 환경 (일일 10,000 메트릭, 1GB 저장공간)
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(10000L)
                .storageQuotaMb(1024L)  // 1GB
                .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("실제 사용 시나리오 테스트 - 대규모 환경")
    void validateQuotaRequest_LargeEnvironmentScenario_ShouldPass() {
        // Given - 대규모 환경 (일일 1,000,000 메트릭, 100GB 저장공간)
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(1000000L)
                .storageQuotaMb(102400L)  // 100GB
                .quotaExceededAction(QuotaExceededAction.BLOCK_COLLECTION)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Builder 패턴 동작 확인")
    void validateQuotaRequest_BuilderPattern_ShouldWorkCorrectly() {
        // Given
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(5000L)
                .storageQuotaMb(500L)
                .quotaExceededAction(QuotaExceededAction.THROTTLE_COLLECTION)
                .build();

        // When & Then
        assertThat(dto.getDailyMetricLimit()).isEqualTo(5000L);
        assertThat(dto.getStorageQuotaMb()).isEqualTo(500L);
        assertThat(dto.getQuotaExceededAction()).isEqualTo(QuotaExceededAction.THROTTLE_COLLECTION);
        
        // 검증도 통과하는지 확인
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("최대 성능 환경 테스트")
    void validateQuotaRequest_MaxPerformanceEnvironment_ShouldPass() {
        // Given - 최대 성능 환경 (최대 허용값)
        QuotaRequestDto dto = QuotaRequestDto.builder()
                .dailyMetricLimit(10000000L)  // 최대 일일 메트릭
                .storageQuotaMb(1048576L)     // 최대 저장공간 (1TB)
                .quotaExceededAction(QuotaExceededAction.BLOCK_COLLECTION)
                .build();

        // When
        Set<ConstraintViolation<QuotaRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
        assertThat(dto.getDailyMetricLimit()).isEqualTo(10000000L);
        assertThat(dto.getStorageQuotaMb()).isEqualTo(1048576L);
    }
}
