package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.dto.auth.TwoFactorEnableRequest;
import com.agenticcp.core.common.dto.auth.TwoFactorSetupResponse;
import com.agenticcp.core.common.dto.auth.UserStatusResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.service.TwoFactorService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 2FA (Two-Factor Authentication) 관련 REST API 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "2FA 관련 API")
public class TwoFactorController {
    
    private final TwoFactorService twoFactorService;
    private final UserService userService;
    
    /**
     * 2FA 설정 (QR 코드 생성)
     */
    @PostMapping("/setup")
    @Operation(summary = "2FA 설정", description = "TOTP 시크릿 키와 QR 코드를 생성합니다.")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor(
            HttpServletRequest request) {
        log.info("[TwoFactorController] setupTwoFactor");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication auth = (Authentication) request.getUserPrincipal();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(AuthErrorCode.UNAUTHORIZED));
            }
            
            String username = auth.getName();
            User user = userService.getUserByUsernameOrThrow(username);
            
            // 이미 2FA가 활성화된 경우
            if (user.isTwoFactorEnabled()) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_ALREADY_ENABLED));
            }
            
            // 시크릿 키 생성
            String secretKey = twoFactorService.generateSecretKey();
            
            // QR 코드 URL 생성
            String qrCodeUrl = twoFactorService.generateQrCodeUrl(username, secretKey);
            
            // QR 코드 이미지 생성
            String qrCodeImage = twoFactorService.generateQrCodeImage(qrCodeUrl);
            
            // 임시로 시크릿 키 저장 (10분 후 만료)
            twoFactorService.storeTemporarySecretKey(username, secretKey);
            
            TwoFactorSetupResponse response = TwoFactorSetupResponse.builder()
                    .secretKey(secretKey)
                    .qrCodeUrl(qrCodeUrl)
                    .qrCodeImage(qrCodeImage)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "2FA 설정 정보가 생성되었습니다."));
            
        } catch (Exception e) {
            log.error("[TwoFactorController] setupTwoFactor - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_SETUP_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 2FA 활성화
     */
    @PostMapping("/enable")
    @Operation(summary = "2FA 활성화", description = "TOTP 코드를 검증하여 2FA를 활성화합니다.")
    public ResponseEntity<ApiResponse<Void>> enableTwoFactor(
            @Valid @RequestBody TwoFactorEnableRequest request,
            HttpServletRequest httpRequest) {
        log.info("[TwoFactorController] enableTwoFactor");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication auth = (Authentication) httpRequest.getUserPrincipal();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(AuthErrorCode.UNAUTHORIZED));
            }
            
            String username = auth.getName();
            User user = userService.getUserByUsernameOrThrow(username);
            
            // 이미 2FA가 활성화된 경우
            if (user.isTwoFactorEnabled()) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_ALREADY_ENABLED));
            }
            
            // 임시 저장된 시크릿 키 조회
            String secretKey = twoFactorService.getTemporarySecretKey(username);
            if (secretKey == null) {
                log.warn("[TwoFactorController] enableTwoFactor - no temporary secret key found");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_SETUP_FAILED, 
                                "2FA 설정이 만료되었습니다. /setup을 다시 호출해주세요."));
            }
            
            // TOTP 코드 검증
            if (!twoFactorService.verifyCode(secretKey, request.getTotpCode())) {
                log.warn("[TwoFactorController] enableTwoFactor - invalid TOTP code");
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(AuthErrorCode.INVALID_TOTP_CODE));
            }
            
            // 2FA 활성화 및 사용자 상태를 ACTIVE로 전환
            userService.enableTwoFactor(username, secretKey);
            
            // 임시 시크릿 키 삭제
            twoFactorService.deleteTemporarySecretKey(username);
            
            return ResponseEntity.ok(ApiResponse.success(null, "2FA가 성공적으로 활성화되었습니다."));
            
        } catch (Exception e) {
            log.error("[TwoFactorController] enableTwoFactor - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_SETUP_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 2FA 비활성화 (관리자용)
     */
    @PostMapping("/disable")
    @Operation(summary = "2FA 비활성화", description = "관리자가 사용자의 2FA를 비활성화합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @RequestParam String username) {
        log.info("[TwoFactorController] disableTwoFactor - username={}", username);
        
        try {
            User user = userService.getUserByUsernameOrThrow(username);
            
            if (!user.isTwoFactorEnabled()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_NOT_ENABLED));
            }
            
            userService.disableTwoFactor(username);
            
            return ResponseEntity.ok(ApiResponse.success(null, "2FA가 비활성화되었습니다."));
            
        } catch (Exception e) {
            log.error("[TwoFactorController] disableTwoFactor - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.TWO_FACTOR_DISABLE_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 사용자 인증 상태 조회
     */
    @GetMapping("/status")
    @Operation(summary = "인증 상태 조회", description = "현재 사용자의 인증 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(
            HttpServletRequest request) {
        log.info("[TwoFactorController] getUserStatus");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication auth = (Authentication) request.getUserPrincipal();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(AuthErrorCode.UNAUTHORIZED));
            }
            
            String username = auth.getName();
            User user = userService.getUserByUsernameOrThrow(username);
            
            UserStatusResponse response = UserStatusResponse.builder()
                    .username(user.getUsername())
                    .status(user.getStatus())
                    .twoFactorEnabled(user.isTwoFactorEnabled())
                    .lastLogin(user.getLastLogin())
                    .requiresTwoFactorSetup(user.isPendingTwoFactorSetup())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "사용자 상태 조회에 성공했습니다."));
            
        } catch (Exception e) {
            log.error("[TwoFactorController] getUserStatus - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.USER_INFO_FAILED, e.getMessage()));
        }
    }
}

