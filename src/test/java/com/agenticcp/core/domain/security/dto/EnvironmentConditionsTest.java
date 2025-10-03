package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EnvironmentConditions DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("EnvironmentConditions DTO 테스트")
class EnvironmentConditionsTest {
    
    @Nested
    @DisplayName("환경 허용 확인 테스트")
    class EnvironmentAllowedTest {
        
        @Test
        @DisplayName("허용 환경 목록에 있는 환경은 허용")
        void isEnvironmentAllowed_InAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedEnvironments(List.of("production", "staging"))
                    .build();
            
            // When
            boolean result = conditions.isEnvironmentAllowed("production");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 환경은 거부")
        void isEnvironmentAllowed_InDeniedList_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .deniedEnvironments(List.of("development", "test"))
                    .build();
            
            // When
            boolean result = conditions.isEnvironmentAllowed("development");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 환경은 거부")
        void isEnvironmentAllowed_NullEnvironment_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedEnvironments(List.of("production"))
                    .build();
            
            // When
            boolean result = conditions.isEnvironmentAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("빈 환경은 거부")
        void isEnvironmentAllowed_EmptyEnvironment_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedEnvironments(List.of("production"))
                    .build();
            
            // When
            boolean result = conditions.isEnvironmentAllowed("");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isEnvironmentAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder().build();
            
            // When
            boolean result = conditions.isEnvironmentAllowed("production");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("테넌트 허용 확인 테스트")
    class TenantAllowedTest {
        
        @Test
        @DisplayName("허용 테넌트 목록에 있는 테넌트는 허용")
        void isTenantAllowed_InAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedTenants(List.of("tenant-A", "tenant-B"))
                    .build();
            
            // When
            boolean result = conditions.isTenantAllowed("tenant-A");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 테넌트는 거부")
        void isTenantAllowed_InDeniedList_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .deniedTenants(List.of("tenant-X", "tenant-Y"))
                    .build();
            
            // When
            boolean result = conditions.isTenantAllowed("tenant-X");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 테넌트는 거부")
        void isTenantAllowed_NullTenant_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedTenants(List.of("tenant-A"))
                    .build();
            
            // When
            boolean result = conditions.isTenantAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isTenantAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder().build();
            
            // When
            boolean result = conditions.isTenantAllowed("tenant-A");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("리전 허용 확인 테스트")
    class RegionAllowedTest {
        
        @Test
        @DisplayName("허용 리전 목록에 있는 리전은 허용")
        void isRegionAllowed_InAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedRegions(List.of("us-east-1", "ap-northeast-2"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed("us-east-1");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 리전은 거부")
        void isRegionAllowed_InDeniedList_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .deniedRegions(List.of("us-west-1"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed("us-west-1");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 리전은 거부")
        void isRegionAllowed_NullRegion_ReturnsFalse() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedRegions(List.of("us-east-1"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isRegionAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            EnvironmentConditions conditions = EnvironmentConditions.builder().build();
            
            // When
            boolean result = conditions.isRegionAllowed("us-east-1");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            List<String> allowedEnvironments = List.of("production", "staging");
            List<String> deniedEnvironments = List.of("development");
            List<String> allowedTenants = List.of("tenant-A", "tenant-B");
            List<String> deniedTenants = List.of("tenant-X");
            List<String> allowedRegions = List.of("us-east-1", "ap-northeast-2");
            List<String> deniedRegions = List.of("us-west-1");
            
            // When
            EnvironmentConditions conditions = EnvironmentConditions.builder()
                    .allowedEnvironments(allowedEnvironments)
                    .deniedEnvironments(deniedEnvironments)
                    .allowedTenants(allowedTenants)
                    .deniedTenants(deniedTenants)
                    .allowedRegions(allowedRegions)
                    .deniedRegions(deniedRegions)
                    .build();
            
            // Then
            assertThat(conditions.getAllowedEnvironments()).isEqualTo(allowedEnvironments);
            assertThat(conditions.getDeniedEnvironments()).isEqualTo(deniedEnvironments);
            assertThat(conditions.getAllowedTenants()).isEqualTo(allowedTenants);
            assertThat(conditions.getDeniedTenants()).isEqualTo(deniedTenants);
            assertThat(conditions.getAllowedRegions()).isEqualTo(allowedRegions);
            assertThat(conditions.getDeniedRegions()).isEqualTo(deniedRegions);
        }
    }
}

