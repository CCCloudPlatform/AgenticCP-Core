package com.agenticcp.core.common.security;

import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtService 단위 테스트
 * 
 * JWT 토큰 서비스의 핵심 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService 단위 테스트")
class JwtServiceTest {

    private JwtService jwtService;
    private String testSecretKey;

    @BeforeEach
    void setUp() {
        testSecretKey = "testSecretKeyForJwtServiceTestingPurposesOnly123456789";
        jwtService = new JwtService(testSecretKey, 3600000L, 604800000L);
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void generateAccessToken_WhenValidUser_ShouldReturnValidToken() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.VIEWER)
                .build();

        // When
        String token = jwtService.generateAccessToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // 토큰이 유효한 JWT 형식인지 확인
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void generateRefreshToken_WhenValidUser_ShouldReturnValidToken() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.VIEWER)
                .build();

        // When
        String token = jwtService.generateRefreshToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // 토큰이 유효한 JWT 형식인지 확인
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("토큰에서 사용자명 추출 성공")
    void extractUsername_WhenValidToken_ShouldReturnUsername() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.VIEWER)
                .build();

        String token = jwtService.generateAccessToken(user);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    void isTokenValid_WhenValidToken_ShouldReturnTrue() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.VIEWER)
                .build();

        String token = jwtService.generateAccessToken(user);

        // When
        boolean isValid = jwtService.isTokenValid(token, "testuser");

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 유효성 검증 실패")
    void isTokenValid_WhenInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then - 예외가 발생해야 함
        try {
            boolean isValid = jwtService.isTokenValid(invalidToken, "testuser");
            assertThat(isValid).isFalse();
        } catch (Exception e) {
            // JWT 파싱 오류는 예상된 동작
            assertThat(e).isInstanceOf(Exception.class);
        }
    }
}
