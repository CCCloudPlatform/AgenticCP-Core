package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NetworkConditions DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("NetworkConditions DTO 테스트")
class NetworkConditionsTest {
    
    @Nested
    @DisplayName("프로토콜 허용 확인 테스트")
    class ProtocolAllowedTest {
        
        @Test
        @DisplayName("허용 프로토콜 목록에 있는 프로토콜은 허용")
        void isProtocolAllowed_InAllowedList_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedProtocols(List.of("HTTPS", "SSH"))
                    .build();
            
            // When
            boolean result = conditions.isProtocolAllowed("HTTPS");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("소문자 프로토콜도 허용")
        void isProtocolAllowed_LowerCase_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedProtocols(List.of("HTTPS"))
                    .build();
            
            // When
            boolean result = conditions.isProtocolAllowed("https");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 프로토콜은 거부")
        void isProtocolAllowed_InDeniedList_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .deniedProtocols(List.of("HTTP", "FTP"))
                    .build();
            
            // When
            boolean result = conditions.isProtocolAllowed("HTTP");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 프로토콜은 거부")
        void isProtocolAllowed_NullProtocol_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedProtocols(List.of("HTTPS"))
                    .build();
            
            // When
            boolean result = conditions.isProtocolAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("빈 프로토콜은 거부")
        void isProtocolAllowed_EmptyProtocol_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedProtocols(List.of("HTTPS"))
                    .build();
            
            // When
            boolean result = conditions.isProtocolAllowed("");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isProtocolAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isProtocolAllowed("HTTP");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("포트 허용 확인 테스트")
    class PortAllowedTest {
        
        @Test
        @DisplayName("허용 포트 목록에 있는 포트는 허용")
        void isPortAllowed_InAllowedList_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedPorts(List.of(443, 22, 80))
                    .build();
            
            // When
            boolean result = conditions.isPortAllowed(443);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 포트는 거부")
        void isPortAllowed_InDeniedList_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .deniedPorts(List.of(23, 21))
                    .build();
            
            // When
            boolean result = conditions.isPortAllowed(23);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 포트는 거부")
        void isPortAllowed_NullPort_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedPorts(List.of(443))
                    .build();
            
            // When
            boolean result = conditions.isPortAllowed(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("음수 포트는 거부")
        void isPortAllowed_NegativePort_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isPortAllowed(-1);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("65535를 초과하는 포트는 거부")
        void isPortAllowed_PortTooLarge_ReturnsFalse() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isPortAllowed(65536);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 목록이 없으면 기본적으로 허용")
        void isPortAllowed_NoAllowedList_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isPortAllowed(8080);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("0번 포트는 허용")
        void isPortAllowed_PortZero_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isPortAllowed(0);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("65535번 포트는 허용")
        void isPortAllowed_PortMaxValue_ReturnsTrue() {
            // Given
            NetworkConditions conditions = NetworkConditions.builder().build();
            
            // When
            boolean result = conditions.isPortAllowed(65535);
            
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
            List<String> allowedProtocols = List.of("HTTPS", "SSH");
            List<String> deniedProtocols = List.of("HTTP", "TELNET");
            List<Integer> allowedPorts = List.of(443, 22);
            List<Integer> deniedPorts = List.of(23, 21);
            List<String> allowedUserAgents = List.of("Mozilla/5.0");
            List<String> deniedUserAgents = List.of("BadBot");
            
            // When
            NetworkConditions conditions = NetworkConditions.builder()
                    .allowedProtocols(allowedProtocols)
                    .deniedProtocols(deniedProtocols)
                    .allowedPorts(allowedPorts)
                    .deniedPorts(deniedPorts)
                    .allowedUserAgents(allowedUserAgents)
                    .deniedUserAgents(deniedUserAgents)
                    .build();
            
            // Then
            assertThat(conditions.getAllowedProtocols()).isEqualTo(allowedProtocols);
            assertThat(conditions.getDeniedProtocols()).isEqualTo(deniedProtocols);
            assertThat(conditions.getAllowedPorts()).isEqualTo(allowedPorts);
            assertThat(conditions.getDeniedPorts()).isEqualTo(deniedPorts);
            assertThat(conditions.getAllowedUserAgents()).isEqualTo(allowedUserAgents);
            assertThat(conditions.getDeniedUserAgents()).isEqualTo(deniedUserAgents);
        }
    }
}

