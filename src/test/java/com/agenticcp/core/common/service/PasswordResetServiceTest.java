package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.PasswordResetConfirm;
import com.agenticcp.core.common.dto.auth.PasswordResetRequest;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserAuthHistoryService;
import com.agenticcp.core.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private UserAuthHistoryService authHistoryService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        passwordResetService = new PasswordResetService(userService, passwordEncoder, emailService, authHistoryService);
        ReflectionTestUtils.setField(passwordResetService, "redisTemplate", redisTemplate);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 시 토큰 저장 및 메일 발송")
    void requestPasswordReset_ShouldStoreTokenAndSendEmail() {
        User user = createUser();
        when(userService.getUserByEmail(user.getEmail())).thenReturn(Optional.of(user));

        PasswordResetRequest request = PasswordResetRequest.builder()
                .email(user.getEmail())
                .build();

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        passwordResetService.requestPasswordReset(request, httpRequest);

        verify(valueOperations).set(startsWith("password_reset_token:"), eq(user.getEmail()), eq(1L), eq(TimeUnit.HOURS));
        verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), eq(user.getUsername()), anyString());
    }

    @Test
    @DisplayName("유효한 토큰으로 비밀번호 재설정 성공")
    void confirmPasswordReset_WithValidToken_ShouldChangePassword() {
        User user = createUser();
        when(userService.getUserByEmail(user.getEmail())).thenReturn(Optional.of(user));

        String token = "reset-token";
        String redisKey = "password_reset_token:" + token;

        when(valueOperations.get(redisKey)).thenReturn(user.getEmail());

        PasswordResetConfirm confirm = PasswordResetConfirm.builder()
                .token(token)
                .newPassword("NewPassword1!")
                .build();

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        passwordResetService.confirmPasswordReset(confirm, httpRequest);

        verify(userService).changePassword(user.getUsername(), "NewPassword1!");
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("현재 비밀번호 검증에 성공하면 비밀번호 변경")
    void changePassword_WithValidCurrentPassword_ShouldChangePassword() {
        User user = createUser();
        when(userService.getUserByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("Current1!", user.getPasswordHash())).thenReturn(true);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        passwordResetService.changePassword(user.getUsername(), "Current1!", "NewPassword1!", httpRequest);

        verify(userService).changePassword(user.getUsername(), "NewPassword1!");
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 예외 발생")
    void changePassword_WithInvalidCurrentPassword_ShouldThrowException() {
        User user = createUser();
        when(userService.getUserByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("WrongPass1!", user.getPasswordHash())).thenReturn(false);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThatThrownBy(() ->
                passwordResetService.changePassword(user.getUsername(), "WrongPass1!", "NewPassword1!", httpRequest)
        ).isInstanceOf(BusinessException.class);
    }

    private User createUser() {
        return User.builder()
                .username("tester")
                .email("tester@agenticcp.com")
                .name("Tester")
                .passwordHash("encoded-password")
                .build();
    }
}

