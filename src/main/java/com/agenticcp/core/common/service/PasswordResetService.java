package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.PasswordResetConfirm;
import com.agenticcp.core.common.dto.auth.PasswordResetRequest;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.enums.AuthType;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.service.UserAuthHistoryService;
import com.agenticcp.core.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final long TOKEN_EXPIRATION_HOURS = 1;
    private static final String REDIS_KEY_PREFIX = "password_reset_token:";
    
    // 비밀번호 복잡성 검증: 8자 이상, 대소문자, 숫자, 특수문자 포함
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserAuthHistoryService authHistoryService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 비밀번호 재설정 요청 처리
     * 
     * @param request 재설정 요청 (이메일)
     * @param httpRequest HTTP 요청 (IP 주소 추출용)
     */
    public void requestPasswordReset(PasswordResetRequest request, HttpServletRequest httpRequest) {
        log.info("[PasswordResetService] requestPasswordReset - email={}", request.getEmail());
        
        try {
            // 1. 이메일로 사용자 조회
            User user = userService.getUserByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        log.warn("[PasswordResetService] requestPasswordReset - user not found email={}", request.getEmail());
                        // 보안을 위해 사용자가 존재하지 않아도 성공 메시지 반환
                        return new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND);
                    });
            
            // 2. 재설정 토큰 생성
            String resetToken = generateResetToken();
            
            // 3. Redis에 토큰 저장 (1시간 만료)
            String redisKey = REDIS_KEY_PREFIX + resetToken;
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(redisKey, user.getEmail(), TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);
                log.info("[PasswordResetService] requestPasswordReset - token stored in Redis key={}", redisKey);
            } else {
                log.warn("[PasswordResetService] requestPasswordReset - Redis not available, token not stored");
            }
            
            // 4. 이메일 발송
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
            
            // 5. 인증 이력 저장
            if (httpRequest != null) {
                authHistoryService.recordAuthHistoryFromRequest(
                        user, AuthType.PASSWORD_RESET, true, httpRequest, null);
            }
            
            log.info("[PasswordResetService] requestPasswordReset - success email={}", request.getEmail());
            
        } catch (ResourceNotFoundException e) {
            // 보안을 위해 사용자가 존재하지 않아도 성공 메시지 반환
            log.warn("[PasswordResetService] requestPasswordReset - user not found (silent fail) email={}", request.getEmail());
            // 실제로는 성공 메시지를 반환하지만, 로그에는 기록
        } catch (Exception e) {
            log.error("[PasswordResetService] requestPasswordReset - failed email={}", request.getEmail(), e);
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_REQUEST_FAILED);
        }
    }

    /**
     * 비밀번호 재설정 확인 및 처리
     * 
     * @param confirm 재설정 확인 정보 (토큰, 새 비밀번호)
     * @param httpRequest HTTP 요청 (IP 주소 추출용)
     */
    public void confirmPasswordReset(PasswordResetConfirm confirm, HttpServletRequest httpRequest) {
        log.info("[PasswordResetService] confirmPasswordReset - token={}", maskToken(confirm.getToken()));
        
        try {
            // 1. 토큰 검증
            String email = validateResetToken(confirm.getToken());
            
            // 2. 사용자 조회
            User user = userService.getUserByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("[PasswordResetService] confirmPasswordReset - user not found email={}", email);
                        return new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND);
                    });
            
            // 3. 비밀번호 복잡성 검증
            validatePasswordComplexity(confirm.getNewPassword());
            
            // 4. 비밀번호 변경
            userService.changePassword(user.getUsername(), confirm.getNewPassword());
            
            // 5. Redis에서 토큰 삭제
            String redisKey = REDIS_KEY_PREFIX + confirm.getToken();
            if (redisTemplate != null) {
                redisTemplate.delete(redisKey);
                log.info("[PasswordResetService] confirmPasswordReset - token deleted from Redis");
            }
            
            // 6. 인증 이력 저장
            if (httpRequest != null) {
                authHistoryService.recordAuthHistoryFromRequest(
                        user, AuthType.PASSWORD_RESET, true, httpRequest, null);
            }
            
            log.info("[PasswordResetService] confirmPasswordReset - success username={}", user.getUsername());
            
        } catch (ResourceNotFoundException e) {
            log.error("[PasswordResetService] confirmPasswordReset - user not found", e);
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_FAILED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PasswordResetService] confirmPasswordReset - failed", e);
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_FAILED);
        }
    }

    /**
     * 비밀번호 변경 (로그인한 사용자용)
     * 
     * @param username 사용자명
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @param httpRequest HTTP 요청 (IP 주소 추출용)
     */
    public void changePassword(String username, String currentPassword, String newPassword, HttpServletRequest httpRequest) {
        log.info("[PasswordResetService] changePassword - username={}", username);
        
        try {
            // 1. 사용자 조회
            User user = userService.getUserByUsernameOrThrow(username);
            
            // 2. 현재 비밀번호 확인
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                log.warn("[PasswordResetService] changePassword - invalid current password username={}", username);
                throw new BusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
            }
            
            // 3. 비밀번호 복잡성 검증
            validatePasswordComplexity(newPassword);
            
            // 4. 비밀번호 변경
            userService.changePassword(username, newPassword);
            
            // 5. 인증 이력 저장
            if (httpRequest != null) {
                authHistoryService.recordAuthHistoryFromRequest(
                        user, AuthType.PASSWORD_CHANGE, true, httpRequest, null);
            }
            
            log.info("[PasswordResetService] changePassword - success username={}", username);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PasswordResetService] changePassword - failed username={}", username, e);
            throw new BusinessException(AuthErrorCode.PASSWORD_CHANGE_FAILED);
        }
    }

    /**
     * 재설정 토큰 생성
     * 32바이트 랜덤 값을 Base64 URL 인코딩
     * 
     * @return 재설정 토큰
     */
    private String generateResetToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * 재설정 토큰 검증
     * 
     * @param token 재설정 토큰
     * @return 사용자 이메일
     */
    private String validateResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("[PasswordResetService] validateResetToken - token is null or empty");
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        
        String redisKey = REDIS_KEY_PREFIX + token;
        
        if (redisTemplate == null) {
            log.warn("[PasswordResetService] validateResetToken - Redis not available");
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        
        String email = (String) redisTemplate.opsForValue().get(redisKey);
        
        if (email == null) {
            log.warn("[PasswordResetService] validateResetToken - token not found or expired token={}", maskToken(token));
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
        
        return email;
    }

    /**
     * 비밀번호 복잡성 검증
     * 8자 이상, 대소문자, 숫자, 특수문자 포함
     * 
     * @param password 비밀번호
     */
    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            log.warn("[PasswordResetService] validatePasswordComplexity - password too short");
            throw new BusinessException(AuthErrorCode.PASSWORD_COMPLEXITY_FAILED);
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            log.warn("[PasswordResetService] validatePasswordComplexity - password does not meet complexity requirements");
            throw new BusinessException(AuthErrorCode.PASSWORD_COMPLEXITY_FAILED);
        }
    }

    /**
     * 토큰 마스킹 (로깅용)
     * 
     * @param token 토큰
     * @return 마스킹된 토큰
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}

