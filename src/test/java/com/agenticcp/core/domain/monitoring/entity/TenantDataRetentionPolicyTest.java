package com.agenticcp.core.domain.monitoring.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantDataRetentionPolicy 엔티티 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("TenantDataRetentionPolicy 엔티티 테스트")
class TenantDataRetentionPolicyTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("엔티티 생성 테스트")
    class EntityCreationTest {

        @Test
        @DisplayName("정상적인 보관 정책 엔티티 생성")
        void createRetentionPolicy_WithValidData_ReturnsCorrectEntity() {
            // Given - 유효한 보관 정책 데이터가 주어진 상황
            String tenantId = "tenant-001";
            Integer retentionDays = 30;
            String dataType = "metrics";
            Boolean isEnabled = true;

            // When - Builder 패턴으로 보관 정책 엔티티를 생성하는 경우
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId(tenantId)
                    .retentionDays(retentionDays)
                    .dataType(dataType)
                    .isEnabled(isEnabled)
                    .build();

            // Then - 모든 필드가 정확히 설정되어야 함
            assertThat(policy.getTenantId()).isEqualTo(tenantId);
            assertThat(policy.getRetentionDays()).isEqualTo(retentionDays);
            assertThat(policy.getDataType()).isEqualTo(dataType);
            assertThat(policy.getIsEnabled()).isEqualTo(isEnabled);
            assertThat(policy.getDeletionStrategy()).isEqualTo(TenantDataRetentionPolicy.DeletionStrategy.DELETE);
            assertThat(policy.getPriority()).isEqualTo(50);
            assertThat(policy.getLastDeletedCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("기본값으로 보관 정책 엔티티 생성")
        void createRetentionPolicy_WithMinimalData_UsesDefaultValues() {
            // Given - 최소한의 필수 데이터만 주어진 상황
            String tenantId = "tenant-002";
            String dataType = "logs";

            // When - 최소 필드만으로 보관 정책 엔티티를 생성하는 경우
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId(tenantId)
                    .dataType(dataType)
                    .build();

            // Then - 기본값들이 올바르게 설정되어야 함
            assertThat(policy.getTenantId()).isEqualTo(tenantId);
            assertThat(policy.getDataType()).isEqualTo(dataType);
            assertThat(policy.getRetentionDays()).isEqualTo(30); // 기본값
            assertThat(policy.getIsEnabled()).isEqualTo(true); // 기본값
            assertThat(policy.getDeletionStrategy()).isEqualTo(TenantDataRetentionPolicy.DeletionStrategy.DELETE); // 기본값
            assertThat(policy.getPriority()).isEqualTo(50); // 기본값
            assertThat(policy.getLastDeletedCount()).isEqualTo(0L); // 기본값
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("테넌트 ID가 null이면 유효성 검증 실패")
        void validateTenantIdNull() {
            // Given - 테넌트 ID가 null인 보관 정책을 생성하는 상황
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId(null)  // 필수 필드를 null로 설정
                    .dataType("metrics")
                    .build();

            // When - Jakarta Validation을 통해 유효성을 검증하는 경우
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then - @NotBlank 어노테이션에 의해 검증 오류가 발생해야 함
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("테넌트 ID는 필수입니다");
        }

        @Test
        @DisplayName("테넌트 ID가 공백이면 유효성 검증 실패")
        void validateTenantIdBlank() {
            // Given - 테넌트 ID가 공백 문자열인 보관 정책을 생성하는 상황
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("   ")  // 공백 문자열로 설정
                    .dataType("metrics")
                    .build();

            // When - Jakarta Validation을 통해 유효성을 검증하는 경우
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then - @NotBlank 어노테이션에 의해 공백 문자열도 검증 오류가 발생해야 함
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("테넌트 ID는 필수입니다");
        }

        @Test
        @DisplayName("테넌트 ID가 50자를 초과하면 유효성 검증 실패")
        void validateTenantIdTooLong() {
            // Given - 테넌트 ID가 최대 길이(50자)를 초과하는 보관 정책을 생성하는 상황
            String longTenantId = "a".repeat(51);  // 51자로 최대 길이 초과
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId(longTenantId)
                    .dataType("metrics")
                    .build();

            // When - Jakarta Validation을 통해 유효성을 검증하는 경우
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then - @Size 어노테이션에 의해 길이 제한 검증 오류가 발생해야 함
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("테넌트 ID는 50자를 초과할 수 없습니다");
        }

        @Test
        @DisplayName("데이터 타입이 null이면 유효성 검증 실패")
        void validateDataTypeNull() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType(null)
                    .build();

            // When
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("데이터 타입은 필수입니다");
        }

        @Test
        @DisplayName("보관 기간이 1일 미만이면 유효성 검증 실패")
        void validateRetentionDaysTooSmall() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(0)
                    .build();

            // When
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("보관 기간은 최소 1일 이상이어야 합니다");
        }

        @Test
        @DisplayName("보관 기간이 365일을 초과하면 유효성 검증 실패")
        void validateRetentionDaysTooLarge() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(366)
                    .build();

            // When
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("보관 기간은 최대 365일 이하여야 합니다");
        }

        @Test
        @DisplayName("우선순위가 1 미만이면 유효성 검증 실패")
        void validatePriorityTooSmall() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .priority(0)
                    .build();

            // When
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("우선순위는 1 이상이어야 합니다");
        }

        @Test
        @DisplayName("우선순위가 100을 초과하면 유효성 검증 실패")
        void validatePriorityTooLarge() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .priority(101)
                    .build();

            // When
            Set<ConstraintViolation<TenantDataRetentionPolicy>> violations = validator.validate(policy);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("우선순위는 100 이하여야 합니다");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("다음 정리 실행 예정 일시 계산")
        void calculateNextCleanupAt() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(30)
                    .lastCleanupAt(LocalDateTime.now().minusDays(1))
                    .build();

            // When
            LocalDateTime nextCleanupAt = policy.getNextCleanupAt();

            // Then
            assertThat(nextCleanupAt).isNotNull();
            assertThat(nextCleanupAt).isAfter(policy.getLastCleanupAt());
        }

        @Test
        @DisplayName("정리 필요 여부 확인 - 정리 필요함")
        void isCleanupNeededTrue() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(30)
                    .lastCleanupAt(LocalDateTime.now().minusDays(2)) // 2일 전에 마지막 정리
                    .build();

            // When
            boolean isNeeded = policy.isCleanupNeeded();

            // Then
            assertThat(isNeeded).isTrue();
        }

        @Test
        @DisplayName("정리 필요 여부 확인 - 정리 불필요함")
        void isCleanupNeededFalse() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(30)
                    .lastCleanupAt(LocalDateTime.now().minusHours(12)) // 12시간 전에 마지막 정리
                    .build();

            // When
            boolean isNeeded = policy.isCleanupNeeded();

            // Then
            assertThat(isNeeded).isFalse();
        }

        @Test
        @DisplayName("정리 상태 업데이트")
        void updateCleanupStatus() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(30)
                    .build();

            Long deletedCount = 1000L;
            LocalDateTime beforeUpdate = LocalDateTime.now();

            // When
            policy.updateCleanupStatus(deletedCount);

            // Then
            assertThat(policy.getLastCleanupAt()).isAfterOrEqualTo(beforeUpdate);
            assertThat(policy.getLastDeletedCount()).isEqualTo(deletedCount);
        }

        @Test
        @DisplayName("보관 기간 설정")
        void setRetentionDays() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .retentionDays(30)
                    .build();

            // When
            policy.setRetentionDays(60);

            // Then
            assertThat(policy.getRetentionDays()).isEqualTo(60);
        }

        @Test
        @DisplayName("삭제 방식 설정")
        void setDeletionStrategy() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .build();

            // When
            policy.setDeletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE);

            // Then
            assertThat(policy.getDeletionStrategy()).isEqualTo(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE);
        }

        @Test
        @DisplayName("활성화 여부 설정")
        void setIsEnabled() {
            // Given
            TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                    .tenantId("tenant-001")
                    .dataType("metrics")
                    .isEnabled(true)
                    .build();

            // When
            policy.setIsEnabled(false);

            // Then
            assertThat(policy.getIsEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("DeletionStrategy 열거형 테스트")
    class DeletionStrategyTest {

        @Test
        @DisplayName("DELETE 전략 확인")
        void deleteStrategy() {
            // When & Then
            assertThat(TenantDataRetentionPolicy.DeletionStrategy.DELETE.name()).isEqualTo("DELETE");
        }

        @Test
        @DisplayName("ARCHIVE 전략 확인")
        void archiveStrategy() {
            // When & Then
            assertThat(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE.name()).isEqualTo("ARCHIVE");
        }

        @Test
        @DisplayName("COMPRESS 전략 확인")
        void compressStrategy() {
            // When & Then
            assertThat(TenantDataRetentionPolicy.DeletionStrategy.COMPRESS.name()).isEqualTo("COMPRESS");
        }
    }
}
