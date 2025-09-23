package com.agenticcp.core.common.security;

import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private final String TEST_SECRET = "dGhpc2lzdGVzdGp3dHNlY3JldGtleWZvcmFnZW50aWNjcGNvcmVhcHBsaWNhdGlvbg=="; // Base64 encoded 256-bit key
    private final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days

    private User testUser;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        testTenant = Tenant.builder()
                .tenantKey("testTenant")
                .tenantName("Test Tenant")
                .build();
        ReflectionTestUtils.setField(testTenant, "id", 1L);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.SUPER_ADMIN)
                .tenant(testTenant)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 검증")
    void generateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("testuser", jwtService.getUsernameFromToken(token));

        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        assertEquals("testuser", claims.get("username"));
        assertEquals("test@example.com", claims.get("email"));
        assertEquals(UserRole.SUPER_ADMIN.name(), claims.get("role"));
        assertEquals(testTenant.getId().intValue(), ((Integer) claims.get("tenantId")).intValue());
        assertEquals(testTenant.getTenantKey(), claims.get("tenantKey"));
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 검증")
    void generateAndValidateRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("testuser", jwtService.getUsernameFromToken(token));

        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        assertEquals("refresh", claims.get("type"));
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateExpiredToken() throws InterruptedException {
        // Temporarily set a very short expiration for testing
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 1L); // 1ms
        String token = jwtService.generateAccessToken(testUser);
        Thread.sleep(50); // Wait for token to expire

        assertTrue(jwtService.isTokenExpired(token));
        assertFalse(jwtService.validateToken(token)); // validateToken should also fail for expired tokens
    }

    @Test
    @DisplayName("유효하지 않은 시크릿 키로 토큰 검증 실패")
    void validateTokenWithInvalidSecret() {
        String token = jwtService.generateAccessToken(testUser);
        assertNotNull(token);

        // Create a new JwtService with an invalid secret key
        JwtService invalidJwtService = new JwtService();
        ReflectionTestUtils.setField(invalidJwtService, "secretKey", "aW52YWxpZHNlY3JldGtleWZvcmFnZW50aWNjcGNvcmVhcHBsaWNhdGlvbg==");
        ReflectionTestUtils.setField(invalidJwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(invalidJwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        assertFalse(invalidJwtService.validateToken(token));
    }

    @Test
    @DisplayName("토큰에서 사용자명 추출")
    void getUsernameFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertEquals("testuser", jwtService.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("토큰 만료 시간 추출")
    void getExpirationDateFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        Date expiration = jwtService.getExpirationFromToken(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("토큰에서 테넌트 정보 추출")
    void getTenantInfoFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertEquals(testTenant.getId(), jwtService.getTenantIdFromToken(token));
        assertEquals(testTenant.getTenantKey(), jwtService.getTenantKeyFromToken(token));
    }

    @Test
    @DisplayName("토큰에서 역할 정보 추출")
    void getRoleFromToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertEquals(UserRole.SUPER_ADMIN.name(), jwtService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("리프레시 토큰 타입 확인")
    void isRefreshToken() {
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertFalse(jwtService.isRefreshToken(accessToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("토큰 만료 시간 계산")
    void getTokenExpirationTime() {
        String token = jwtService.generateAccessToken(testUser);
        long expirationTime = jwtService.getTokenExpirationTime(token);
        assertTrue(expirationTime > 0);
        assertTrue(expirationTime <= 3600); // 1시간 이내
    }
}
