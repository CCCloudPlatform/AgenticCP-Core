package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RegisterRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.service.AuthenticationService;
import com.agenticcp.core.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 * 
 * 인증 컨트롤러의 REST API 엔드포인트를 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;
    
    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authenticationService, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("정상적인 회원가입 요청 성공")
    void register_WhenValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authenticationService.register(any(RegisterRequest.class), any()))
                .thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("정상적인 로그인 요청 성공")
    void login_WhenValidCredentials_ShouldReturnOk() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authenticationService.login(any(LoginRequest.class), any()))
                .thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token"));
    }
}
