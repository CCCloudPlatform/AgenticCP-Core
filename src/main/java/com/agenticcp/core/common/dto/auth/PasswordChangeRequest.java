package com.agenticcp.core.common.dto.auth;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 DTO (로그인한 사용자용)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    @Masked(type = MaskingType.PASSWORD)
    private String currentPassword;
    
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    @Masked(type = MaskingType.PASSWORD)
    private String newPassword;
}

