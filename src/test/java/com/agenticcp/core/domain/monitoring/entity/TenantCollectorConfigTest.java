package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantCollectorConfig 엔티티 단위 테스트
 * 테넌트별 메트릭 수집기 설정 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
@DisplayName("TenantCollectorConfig 엔티티 단위 테스트")
class TenantCollectorConfigTest {

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 TenantCollectorConfig 생성")
        void buildTenantCollectorConfig_WhenValidData_ShouldCreateConfig() {
            // Given
            String tenantId = "tenant-001";
            CollectorType collectorType = CollectorType.SYSTEM;
            Boolean isEnabled = true;
            Long collectionInterval = 60000L;
            Integer retryCount = 3;
            Long timeout = 30000L;
            String targetMetrics = "[\"cpu.usage\", \"memory.usage\"]";
            String collectorSettings = "{\"region\": \"us-east-1\"}";
            Integer priority = 100;

            // When
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId(tenantId)
                    .collectorType(collectorType)
                    .isEnabled(isEnabled)
                    .collectionInterval(collectionInterval)
                    .retryCount(retryCount)
                    .timeout(timeout)
                    .targetMetrics(targetMetrics)
                    .collectorSettings(collectorSettings)
                    .priority(priority)
                    .build();

            // Then
            assertThat(config.getTenantId()).isEqualTo(tenantId);
            assertThat(config.getCollectorType()).isEqualTo(collectorType);
            assertThat(config.getIsEnabled()).isEqualTo(isEnabled);
            assertThat(config.getCollectionInterval()).isEqualTo(collectionInterval);
            assertThat(config.getRetryCount()).isEqualTo(retryCount);
            assertThat(config.getTimeout()).isEqualTo(timeout);
            assertThat(config.getTargetMetrics()).isEqualTo(targetMetrics);
            assertThat(config.getCollectorSettings()).isEqualTo(collectorSettings);
            assertThat(config.getPriority()).isEqualTo(priority);
        }

        @Test
        @DisplayName("기본값이 적용된 TenantCollectorConfig 생성")
        void buildTenantCollectorConfig_WithDefaultValues_ShouldSetDefaults() {
            // When
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .build();

            // Then
            assertThat(config.getIsEnabled()).isTrue();
            assertThat(config.getCollectionInterval()).isEqualTo(60000L);
            assertThat(config.getRetryCount()).isEqualTo(3);
            assertThat(config.getTimeout()).isEqualTo(30000L);
            assertThat(config.getPriority()).isEqualTo(100);
            assertThat(config.getCurrentDailyUsage()).isEqualTo(0L);
            assertThat(config.getCurrentStorageUsageMb()).isEqualTo(0L);
            assertThat(config.getQuotaExceededAction()).isEqualTo(QuotaExceededAction.WARN_ONLY);
            assertThat(config.getMetadataList()).isEmpty();
        }

        @Test
        @DisplayName("TestDataBuilder를 사용한 TenantCollectorConfig 생성")
        void buildTenantCollectorConfig_UsingTestDataBuilder_ShouldCreateConfig() {
            // When
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();

            // Then
            assertThat(config.getTenantId()).isEqualTo("tenant-001");
            assertThat(config.getCollectorType()).isEqualTo(CollectorType.SYSTEM);
            assertThat(config.getIsEnabled()).isTrue();
            assertThat(config.getCollectionInterval()).isEqualTo(60000L);
        }
    }

    @Nested
    @DisplayName("수집기 설정 관리 테스트")
    class CollectorSettingsTest {

        @Test
        @DisplayName("수집기 활성화/비활성화")
        void setEnabled_WhenCalled_ShouldUpdateEnabled() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder()
                    .isEnabled(true)
                    .build();

            // When
            config.setEnabled(false);

            // Then
            assertThat(config.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("수집 주기 업데이트")
        void updateCollectionInterval_WhenCalled_ShouldUpdateInterval() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            Long newInterval = 30000L;

            // When
            config.updateCollectionInterval(newInterval);

            // Then
            assertThat(config.getCollectionInterval()).isEqualTo(newInterval);
        }

        @Test
        @DisplayName("타겟 메트릭 업데이트")
        void updateTargetMetrics_WhenCalled_ShouldUpdateMetrics() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            String newMetrics = "[\"disk.usage\", \"network.usage\"]";

            // When
            config.updateTargetMetrics(newMetrics);

            // Then
            assertThat(config.getTargetMetrics()).isEqualTo(newMetrics);
        }

        @Test
        @DisplayName("수집기 설정 업데이트")
        void updateCollectorSettings_WhenCalled_ShouldUpdateSettings() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            String newSettings = "{\"region\": \"us-west-2\", \"endpoint\": \"https://example.com\"}";

            // When
            config.updateCollectorSettings(newSettings);

            // Then
            assertThat(config.getCollectorSettings()).isEqualTo(newSettings);
        }

        @Test
        @DisplayName("재시도 횟수 업데이트")
        void updateRetryCount_WhenCalled_ShouldUpdateRetryCount() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            Integer newRetryCount = 5;

            // When
            config.updateRetryCount(newRetryCount);

            // Then
            assertThat(config.getRetryCount()).isEqualTo(newRetryCount);
        }

        @Test
        @DisplayName("타임아웃 업데이트")
        void updateTimeout_WhenCalled_ShouldUpdateTimeout() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            Long newTimeout = 60000L;

            // When
            config.updateTimeout(newTimeout);

            // Then
            assertThat(config.getTimeout()).isEqualTo(newTimeout);
        }

        @Test
        @DisplayName("우선순위 업데이트")
        void updatePriority_WhenCalled_ShouldUpdatePriority() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            Integer newPriority = 50;

            // When
            config.updatePriority(newPriority);

            // Then
            assertThat(config.getPriority()).isEqualTo(newPriority);
        }
    }

    @Nested
    @DisplayName("할당량 관리 테스트")
    class QuotaManagementTest {

        @Test
        @DisplayName("할당량 초과 여부 확인 - 초과하지 않은 경우")
        void isQuotaExceeded_WhenNotExceeded_ShouldReturnFalse() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .dailyMetricLimit(1000L)
                    .currentDailyUsage(500L)
                    .build();

            // When & Then
            assertThat(config.isQuotaExceeded()).isFalse();
        }

        @Test
        @DisplayName("할당량 초과 여부 확인 - 초과한 경우")
        void isQuotaExceeded_WhenExceeded_ShouldReturnTrue() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .dailyMetricLimit(1000L)
                    .currentDailyUsage(1000L)
                    .build();

            // When & Then
            assertThat(config.isQuotaExceeded()).isTrue();
        }

        @Test
        @DisplayName("할당량 초과 여부 확인 - 할당량이 설정되지 않은 경우")
        void isQuotaExceeded_WhenLimitNotSet_ShouldReturnFalse() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .currentDailyUsage(1000L)
                    .build();

            // When & Then
            assertThat(config.isQuotaExceeded()).isFalse();
        }

        @Test
        @DisplayName("일일 사용량 증가")
        void incrementDailyUsage_WhenCalled_ShouldIncreaseUsage() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .currentDailyUsage(100L)
                    .build();

            // When
            config.incrementDailyUsage(50L);

            // Then
            assertThat(config.getCurrentDailyUsage()).isEqualTo(150L);
        }

        @Test
        @DisplayName("일일 사용량 증가 - null인 경우")
        void incrementDailyUsage_WhenNull_ShouldInitializeAndIncrease() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .build();

            // When
            config.incrementDailyUsage(50L);

            // Then
            assertThat(config.getCurrentDailyUsage()).isEqualTo(50L);
        }

        @Test
        @DisplayName("저장 공간 사용량 증가")
        void incrementStorageUsage_WhenCalled_ShouldIncreaseStorageUsage() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .currentStorageUsageMb(100L)
                    .build();

            // When
            config.incrementStorageUsage(50L);

            // Then
            assertThat(config.getCurrentStorageUsageMb()).isEqualTo(150L);
        }

        @Test
        @DisplayName("저장 공간 사용량 증가 - null인 경우")
        void incrementStorageUsage_WhenNull_ShouldInitializeAndIncrease() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .build();

            // When
            config.incrementStorageUsage(50L);

            // Then
            assertThat(config.getCurrentStorageUsageMb()).isEqualTo(50L);
        }

        @Test
        @DisplayName("일일 사용량 리셋")
        void resetDailyUsage_WhenCalled_ShouldResetUsageAndSetResetTime() {
            // Given
            TenantCollectorConfig config = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .currentDailyUsage(1000L)
                    .build();

            // When
            config.resetDailyUsage();

            // Then
            assertThat(config.getCurrentDailyUsage()).isEqualTo(0L);
            assertThat(config.getLastResetAt()).isNotNull();
        }

        @Test
        @DisplayName("할당량 설정 업데이트")
        void updateQuota_WhenCalled_ShouldUpdateQuotaSettings() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            Long dailyMetricLimit = 5000L;
            Long storageQuotaMb = 1000L;
            QuotaExceededAction action = QuotaExceededAction.BLOCK_COLLECTION;

            // When
            config.updateQuota(dailyMetricLimit, storageQuotaMb, action);

            // Then
            assertThat(config.getDailyMetricLimit()).isEqualTo(dailyMetricLimit);
            assertThat(config.getStorageQuotaMb()).isEqualTo(storageQuotaMb);
            assertThat(config.getQuotaExceededAction()).isEqualTo(action);
        }

        @Test
        @DisplayName("할당량 초과 동작 설정")
        void setQuotaExceededAction_WhenCalled_ShouldUpdateAction() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            QuotaExceededAction newAction = QuotaExceededAction.THROTTLE_COLLECTION;

            // When
            config.setQuotaExceededAction(newAction);

            // Then
            assertThat(config.getQuotaExceededAction()).isEqualTo(newAction);
        }
    }

    @Nested
    @DisplayName("메타데이터 관리 테스트")
    class MetadataManagementTest {

        @Test
        @DisplayName("메타데이터 추가")
        void addMetadata_WhenCalled_ShouldAddMetadata() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = new TenantCollectorMetadata(
                    null, // tenantCollectorConfig는 addMetadata에서 설정됨
                    "test-key",
                    "test-value",
                    "STRING",
                    "test description"
            );

            // When
            config.addMetadata(metadata);

            // Then
            assertThat(config.getMetadataList()).hasSize(1);
            assertThat(config.getMetadataList()).contains(metadata);
            assertThat(metadata.getTenantCollectorConfig()).isEqualTo(config);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터로 생성된 TenantCollectorConfig는 equals true")
        void equals_WhenSameData_ShouldReturnTrue() {
            // Given
            TenantCollectorConfig config1 = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorConfig config2 = TestDataBuilder.tenantCollectorConfigBuilder().build();

            // When & Then
            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터로 생성된 TenantCollectorConfig는 equals false")
        void equals_WhenDifferentData_ShouldReturnFalse() {
            // Given
            TenantCollectorConfig config1 = TenantCollectorConfig.builder()
                    .tenantId("tenant-001")
                    .collectorType(CollectorType.SYSTEM)
                    .build();

            TenantCollectorConfig config2 = TenantCollectorConfig.builder()
                    .tenantId("tenant-002")
                    .collectorType(CollectorType.APPLICATION)
                    .build();

            // When & Then
            assertThat(config1).isNotEqualTo(config2);
        }
    }
}

