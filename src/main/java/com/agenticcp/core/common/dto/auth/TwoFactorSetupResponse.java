package com.agenticcp.core.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2FA 설정 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    
    /**
     * TOTP 시크릿 키 (Base32 인코딩)
     */
    private String secretKey;
    
    /**
     * QR 코드 URL (otpauth:// 형식)
     */
    private String qrCodeUrl;
    
    /**
     * QR 코드 이미지 (Base64 인코딩된 PNG)
     */
    private String qrCodeImage;
    
    /**
     * 백업 코드 목록 (선택사항)
     */
    private String backupCodes;
}
