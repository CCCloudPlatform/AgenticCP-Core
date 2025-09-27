package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // application-test.yml 또는 테스트용 프로파일 사용
@Transactional // 각 테스트 후 롤백
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        testTenant = Tenant.builder()
                .tenantKey("testTenant")
                .tenantName("Test Tenant")
                .status(Status.ACTIVE)
                .build();
        tenantRepository.save(testTenant);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.SUPER_ADMIN)
                .status(Status.ACTIVE)
                .tenant(testTenant)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .twoFactorEnabled(false)
                .build();
        userRepository.save(testUser);
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("정상적인 로그인")
        void login_Success_ReturnsTokens() throws Exception {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.user.username").value("testuser"));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void login_Failure_WrongPassword() throws Exception {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("wrongpassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("잘못된 사용자명 또는 비밀번호입니다"));

            // 실패 횟수 증가 확인
            User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
            assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("잠금된 계정으로 로그인 실패")
        void login_Failure_AccountLocked() throws Exception {
            testUser.setFailedLoginAttempts(5);
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            userRepository.save(testUser);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists()); // 메시지 내용 검증은 유동적일 수 있음
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("정상적인 토큰 갱신")
        void refreshToken_Success() throws Exception {
        // 1. 로그인하여 초기 토큰 발급
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String loginResponseContent = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<TokenResponse> loginApiResponse = objectMapper.readValue(loginResponseContent, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<TokenResponse>>() {});
        String refreshToken = loginApiResponse.getData().getRefreshToken();

        // 2. 리프레시 토큰으로 갱신 요청
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 실패")
        void refreshToken_Failure_InvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalidRefreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다"));
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("정상적인 로그아웃")
        void logout_Success() throws Exception {
        // 1. 로그인하여 토큰 발급
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String loginResponseContent = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<TokenResponse> loginApiResponse = objectMapper.readValue(loginResponseContent, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<TokenResponse>>() {});
        String accessToken = loginApiResponse.getData().getAccessToken();

        // 2. 로그아웃 요청
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다"));
        }
    }

    @Nested
    @DisplayName("사용자 정보 조회 테스트")
    class UserInfoTest {

        @Test
        @DisplayName("정상적인 사용자 정보 조회")
        void getCurrentUser_Success() throws Exception {
        // 1. 로그인하여 토큰 발급
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String loginResponseContent = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApiResponse<TokenResponse> loginApiResponse = objectMapper.readValue(loginResponseContent, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<TokenResponse>>() {});
        String accessToken = loginApiResponse.getData().getAccessToken();

        // 2. 현재 사용자 정보 조회 요청
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

        @Test
        @DisplayName("인증되지 않은 상태에서 사용자 정보 조회 시 401 Unauthorized")
        void getCurrentUser_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
