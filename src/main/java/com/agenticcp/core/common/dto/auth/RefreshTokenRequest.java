package com.agenticcp.core.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다")
    @Size(min = 10, max = 1000, message = "리프레시 토큰은 10-1000자 사이여야 합니다")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "리프레시 토큰은 영문, 숫자, 점, 언더스코어, 하이픈만 사용 가능합니다")
    private String refreshToken;
}
