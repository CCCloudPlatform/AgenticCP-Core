package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.auth.PasswordChangeRequest;
import com.agenticcp.core.common.dto.auth.PasswordResetConfirm;
import com.agenticcp.core.common.dto.auth.PasswordResetRequest;
import com.agenticcp.core.common.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PasswordControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private PasswordController passwordController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(passwordController).build();
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 API 성공 응답")
    void requestPasswordReset_ShouldReturnSuccess() throws Exception {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("tester@agenticcp.com")
                .build();

        mockMvc.perform(post("/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."));

        ArgumentCaptor<PasswordResetRequest> requestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(passwordResetService).requestPasswordReset(requestCaptor.capture(), any(HttpServletRequest.class));
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("tester@agenticcp.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 확인 API 성공 응답")
    void confirmPasswordReset_ShouldReturnSuccess() throws Exception {
        PasswordResetConfirm request = PasswordResetConfirm.builder()
                .token("reset-token")
                .newPassword("NewPassword1!")
                .build();

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 재설정되었습니다."));

        ArgumentCaptor<PasswordResetConfirm> confirmCaptor = ArgumentCaptor.forClass(PasswordResetConfirm.class);
        verify(passwordResetService).confirmPasswordReset(confirmCaptor.capture(), any(HttpServletRequest.class));
        assertThat(confirmCaptor.getValue().getToken()).isEqualTo("reset-token");
    }

    @Test
    @DisplayName("비밀번호 변경 API 성공 응답")
    void changePassword_ShouldReturnSuccess() throws Exception {
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .currentPassword("Current1!")
                .newPassword("NewPassword1!")
                .build();

        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken("tester", "password", Collections.emptyList());

        mockMvc.perform(post("/auth/password/change")
                        .principal(authenticationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));

        verify(passwordResetService).changePassword(eq("tester"), eq("Current1!"), eq("NewPassword1!"), any(HttpServletRequest.class));
    }
}
