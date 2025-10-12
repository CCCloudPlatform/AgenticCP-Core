package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IpConditions DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("IpConditions DTO 테스트")
class IpConditionsTest {
    
    @Nested
    @DisplayName("IP 주소 허용 확인 테스트")
    class IpAllowedTest {
        
        @Test
        @DisplayName("허용 목록에 있는 IP는 허용")
        void isIpAllowed_InAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedIps(List.of("192.168.1.100", "10.0.0.1"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 목록에 있는 IP는 거부")
        void isIpAllowed_InDeniedList_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .deniedIps(List.of("192.168.1.100", "10.0.0.1"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null IP는 거부")
        void isIpAllowed_NullIp_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedIps(List.of("192.168.1.100"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("빈 IP는 거부")
        void isIpAllowed_EmptyIp_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedIps(List.of("192.168.1.100"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isIpAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder().build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("허용 목록이 있지만 포함되지 않으면 거부")
        void isIpAllowed_NotInAllowedList_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedIps(List.of("192.168.1.100"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.101");
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("CIDR 블록 테스트")
    class CidrTest {
        
        @Test
        @DisplayName("허용 CIDR 블록에 포함된 IP는 허용")
        void isIpAllowed_InAllowedCidr_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCidrs(List.of("192.168.1.0/24"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 CIDR 블록에 포함된 IP는 거부")
        void isIpAllowed_InDeniedCidr_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .deniedCidrs(List.of("192.168.1.0/24"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("CIDR 블록에 포함되지 않은 IP")
        void isIpAllowed_NotInCidr_WithAllowedList_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCidrs(List.of("192.168.1.0/24"))
                    .build();
            
            // When
            boolean result = conditions.isIpAllowed("192.168.2.100");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("여러 CIDR 블록 중 하나에 포함되면 허용")
        void isIpAllowed_InOneOfMultipleCidrs_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCidrs(List.of("192.168.1.0/24", "10.0.0.0/8"))
                    .build();
            
            // When
            boolean result1 = conditions.isIpAllowed("192.168.1.100");
            boolean result2 = conditions.isIpAllowed("10.10.10.10");
            
            // Then
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
        }
    }
    
    @Nested
    @DisplayName("국가 코드 허용 확인 테스트")
    class CountryAllowedTest {
        
        @Test
        @DisplayName("허용 국가 목록에 있는 국가는 허용")
        void isCountryAllowed_InAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCountries(List.of("KR", "US", "JP"))
                    .build();
            
            // When
            boolean result = conditions.isCountryAllowed("KR");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("소문자 국가 코드도 처리")
        void isCountryAllowed_LowerCase_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCountries(List.of("KR", "US"))
                    .build();
            
            // When
            boolean result = conditions.isCountryAllowed("kr");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 국가 목록에 있는 국가는 거부")
        void isCountryAllowed_InDeniedList_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .deniedCountries(List.of("CN", "RU"))
                    .build();
            
            // When
            boolean result = conditions.isCountryAllowed("CN");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 국가 코드는 거부")
        void isCountryAllowed_NullCode_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCountries(List.of("KR"))
                    .build();
            
            // When
            boolean result = conditions.isCountryAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("빈 국가 코드는 거부")
        void isCountryAllowed_EmptyCode_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedCountries(List.of("KR"))
                    .build();
            
            // When
            boolean result = conditions.isCountryAllowed("");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 국가 목록이 없으면 기본적으로 허용")
        void isCountryAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder().build();
            
            // When
            boolean result = conditions.isCountryAllowed("KR");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("지역 허용 확인 테스트")
    class RegionAllowedTest {
        
        @Test
        @DisplayName("허용 지역 목록에 있는 지역은 허용")
        void isRegionAllowed_InAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedRegions(List.of("us-east-1", "ap-northeast-2"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed("us-east-1");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 지역 목록에 있는 지역은 거부")
        void isRegionAllowed_InDeniedList_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .deniedRegions(List.of("us-west-1", "eu-west-1"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed("us-west-1");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 지역은 거부")
        void isRegionAllowed_NullRegion_ReturnsFalse() {
            // Given
            IpConditions conditions = IpConditions.builder()
                    .allowedRegions(List.of("us-east-1"))
                    .build();
            
            // When
            boolean result = conditions.isRegionAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 지역 목록이 없으면 기본적으로 허용")
        void isRegionAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            IpConditions conditions = IpConditions.builder().build();
            
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
            List<String> allowedIps = List.of("192.168.1.100", "10.0.0.1");
            List<String> deniedIps = List.of("192.168.1.200");
            List<String> allowedCidrs = List.of("192.168.0.0/16");
            List<String> deniedCidrs = List.of("10.10.0.0/16");
            List<String> allowedCountries = List.of("KR", "US");
            List<String> deniedCountries = List.of("CN");
            List<String> allowedRegions = List.of("us-east-1");
            List<String> deniedRegions = List.of("us-west-1");
            
            // When
            IpConditions conditions = IpConditions.builder()
                    .allowedIps(allowedIps)
                    .deniedIps(deniedIps)
                    .allowedCidrs(allowedCidrs)
                    .deniedCidrs(deniedCidrs)
                    .allowedCountries(allowedCountries)
                    .deniedCountries(deniedCountries)
                    .allowedRegions(allowedRegions)
                    .deniedRegions(deniedRegions)
                    .build();
            
            // Then
            assertThat(conditions.getAllowedIps()).isEqualTo(allowedIps);
            assertThat(conditions.getDeniedIps()).isEqualTo(deniedIps);
            assertThat(conditions.getAllowedCidrs()).isEqualTo(allowedCidrs);
            assertThat(conditions.getDeniedCidrs()).isEqualTo(deniedCidrs);
            assertThat(conditions.getAllowedCountries()).isEqualTo(allowedCountries);
            assertThat(conditions.getDeniedCountries()).isEqualTo(deniedCountries);
            assertThat(conditions.getAllowedRegions()).isEqualTo(allowedRegions);
            assertThat(conditions.getDeniedRegions()).isEqualTo(deniedRegions);
        }
    }
}

