package com.agenticcp.core.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2FA 비활성화 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorDisableRequest {

    @NotBlank(message = "2FA 검증 코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "2FA 코드는 6자리 숫자여야 합니다")
    private String verificationCode;
}
