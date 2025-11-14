package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.auth.PasswordChangeRequest;
import com.agenticcp.core.common.dto.auth.PasswordResetConfirm;
import com.agenticcp.core.common.dto.auth.PasswordResetRequest;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 비밀번호 관련 REST API 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
@Tag(name = "Password", description = "비밀번호 재설정 및 변경 API")
public class PasswordController {

    private final PasswordResetService passwordResetService;

    /**
     * 비밀번호 재설정 요청
     * 이메일로 재설정 링크 발송
     */
    @PostMapping("/reset-request")
    @Operation(summary = "비밀번호 재설정 요청", 
               description = "이메일로 비밀번호 재설정 링크를 발송합니다. 보안을 위해 사용자가 존재하지 않아도 성공 메시지를 반환합니다.")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        log.info("[PasswordController] requestPasswordReset - email={}", request.getEmail());
        
        try {
            passwordResetService.requestPasswordReset(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(null, 
                    "비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."));
            
        } catch (Exception e) {
            log.error("[PasswordController] requestPasswordReset - error", e);
            // 보안을 위해 실제 오류를 숨기고 일반적인 메시지 반환
            return ResponseEntity.ok(ApiResponse.success(null, 
                    "비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."));
        }
    }

    /**
     * 비밀번호 재설정 확인 및 처리
     * 재설정 토큰과 새 비밀번호로 비밀번호 재설정
     */
    @PostMapping("/reset")
    @Operation(summary = "비밀번호 재설정", 
               description = "재설정 토큰과 새 비밀번호를 사용하여 비밀번호를 재설정합니다.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirm confirm,
            HttpServletRequest httpRequest) {
        log.info("[PasswordController] resetPassword - token={}", maskToken(confirm.getToken()));
        
        try {
            passwordResetService.confirmPasswordReset(confirm, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 재설정되었습니다."));
            
        } catch (Exception e) {
            log.error("[PasswordController] resetPassword - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.PASSWORD_RESET_FAILED, e.getMessage()));
        }
    }

    /**
     * 비밀번호 변경 (로그인한 사용자용)
     * 현재 비밀번호 확인 후 새 비밀번호로 변경
     */
    @PostMapping("/change")
    @Operation(summary = "비밀번호 변경", 
               description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. 로그인이 필요합니다.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        log.info("[PasswordController] changePassword");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = (Authentication) httpRequest.getUserPrincipal();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(AuthErrorCode.UNAUTHORIZED));
            }
            
            String username = authentication.getName();
            passwordResetService.changePassword(username, request.getCurrentPassword(), 
                    request.getNewPassword(), httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다."));
            
        } catch (Exception e) {
            log.error("[PasswordController] changePassword - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.PASSWORD_CHANGE_FAILED, e.getMessage()));
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

