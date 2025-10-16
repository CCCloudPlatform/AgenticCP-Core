package com.agenticcp.core.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2FA 활성화 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorEnableRequest {
    
    /**
     * TOTP 코드 (6자리 숫자)
     */
    @NotBlank(message = "TOTP 코드는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "TOTP 코드는 6자리 숫자여야 합니다.")
    private String totpCode;
}
