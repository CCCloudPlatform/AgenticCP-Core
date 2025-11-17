package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantCollectorMetadata 엔티티 단위 테스트
 * 테넌트 수집기 설정 메타데이터 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
@DisplayName("TenantCollectorMetadata 엔티티 단위 테스트")
class TenantCollectorMetadataTest {

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 TenantCollectorMetadata 생성")
        void buildTenantCollectorMetadata_WhenValidData_ShouldCreateMetadata() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            String metadataKey = "test-key";
            String metadataValue = "test-value";
            String metadataType = "STRING";
            String description = "test description";

            // When
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey(metadataKey)
                    .metadataValue(metadataValue)
                    .metadataType(metadataType)
                    .description(description)
                    .build();

            // Then
            assertThat(metadata.getTenantCollectorConfig()).isEqualTo(config);
            assertThat(metadata.getMetadataKey()).isEqualTo(metadataKey);
            assertThat(metadata.getMetadataValue()).isEqualTo(metadataValue);
            assertThat(metadata.getMetadataType()).isEqualTo(metadataType);
            assertThat(metadata.getDescription()).isEqualTo(description);
        }

        @Test
        @DisplayName("기본값이 적용된 TenantCollectorMetadata 생성")
        void buildTenantCollectorMetadata_WithDefaultValues_ShouldSetDefaults() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();

            // When
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .build();

            // Then
            assertThat(metadata.getMetadataType()).isEqualTo("STRING");
        }

        @Test
        @DisplayName("최소 필수 필드만으로 TenantCollectorMetadata 생성")
        void buildTenantCollectorMetadata_WithMinimalFields_ShouldCreateMetadata() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            String metadataKey = "minimal-key";

            // When
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey(metadataKey)
                    .build();

            // Then
            assertThat(metadata.getTenantCollectorConfig()).isEqualTo(config);
            assertThat(metadata.getMetadataKey()).isEqualTo(metadataKey);
            assertThat(metadata.getMetadataValue()).isNull();
            assertThat(metadata.getDescription()).isNull();
            assertThat(metadata.getMetadataType()).isEqualTo("STRING");
        }
    }

    @Nested
    @DisplayName("메타데이터 업데이트 테스트")
    class MetadataUpdateTest {

        @Test
        @DisplayName("메타데이터 값 업데이트")
        void updateValue_WhenCalled_ShouldUpdateValue() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .metadataValue("old-value")
                    .build();
            String newValue = "new-value";

            // When
            metadata.updateValue(newValue);

            // Then
            assertThat(metadata.getMetadataValue()).isEqualTo(newValue);
        }

        @Test
        @DisplayName("메타데이터 타입 업데이트")
        void updateType_WhenCalled_ShouldUpdateType() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .metadataType("STRING")
                    .build();
            String newType = "NUMBER";

            // When
            metadata.updateType(newType);

            // Then
            assertThat(metadata.getMetadataType()).isEqualTo(newType);
        }

        @Test
        @DisplayName("설명 업데이트")
        void updateDescription_WhenCalled_ShouldUpdateDescription() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .description("old description")
                    .build();
            String newDescription = "new description";

            // When
            metadata.updateDescription(newDescription);

            // Then
            assertThat(metadata.getDescription()).isEqualTo(newDescription);
        }

        @Test
        @DisplayName("테넌트 수집기 설정 설정")
        void setTenantCollectorConfig_WhenCalled_ShouldUpdateConfig() {
            // Given
            TenantCollectorConfig config1 = TestDataBuilder.tenantCollectorConfigBuilder()
                    .tenantId("tenant-001")
                    .build();
            TenantCollectorConfig config2 = TestDataBuilder.tenantCollectorConfigBuilder()
                    .tenantId("tenant-002")
                    .build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config1)
                    .metadataKey("test-key")
                    .build();

            // When
            metadata.setTenantCollectorConfig(config2);

            // Then
            assertThat(metadata.getTenantCollectorConfig()).isEqualTo(config2);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터로 생성된 TenantCollectorMetadata는 equals true")
        void equals_WhenSameData_ShouldReturnTrue() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata1 = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .metadataValue("test-value")
                    .build();
            TenantCollectorMetadata metadata2 = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .metadataValue("test-value")
                    .build();

            // When & Then
            assertThat(metadata1).isEqualTo(metadata2);
            assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터로 생성된 TenantCollectorMetadata는 equals false")
        void equals_WhenDifferentData_ShouldReturnFalse() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata1 = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("key-1")
                    .metadataValue("value-1")
                    .build();
            TenantCollectorMetadata metadata2 = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("key-2")
                    .metadataValue("value-2")
                    .build();

            // When & Then
            assertThat(metadata1).isNotEqualTo(metadata2);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString 메서드가 모든 필드를 포함")
        void toString_ShouldContainAllFields() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .metadataValue("test-value")
                    .metadataType("STRING")
                    .description("test description")
                    .build();

            // When
            String toString = metadata.toString();

            // Then
            assertThat(toString).contains("metadataKey=test-key");
            assertThat(toString).contains("metadataValue=test-value");
            assertThat(toString).contains("metadataType=STRING");
            assertThat(toString).contains("description=test description");
        }

        @Test
        @DisplayName("null 값이 포함된 경우 toString 동작")
        void toString_WithNullValues_ShouldHandleGracefully() {
            // Given
            TenantCollectorConfig config = TestDataBuilder.tenantCollectorConfigBuilder().build();
            TenantCollectorMetadata metadata = TenantCollectorMetadata.builder()
                    .tenantCollectorConfig(config)
                    .metadataKey("test-key")
                    .build();

            // When
            String toString = metadata.toString();

            // Then
            assertThat(toString).contains("metadataKey=test-key");
            assertThat(toString).contains("metadataValue=null");
            assertThat(toString).contains("description=null");
        }
    }
}

