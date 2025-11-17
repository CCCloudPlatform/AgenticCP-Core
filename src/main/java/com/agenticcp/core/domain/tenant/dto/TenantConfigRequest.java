package com.agenticcp.core.domain.tenant.dto;

import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 테넌트 설정 생성/수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigRequest {

    @NotBlank(message = "설정 키는 필수입니다")
    private String configKey;

    @NotBlank(message = "설정 값은 필수입니다")
    private String configValue;

    @NotNull(message = "설정 타입은 필수입니다")
    private TenantConfig.ConfigType configType;

    private String description;

    @Builder.Default
    private Boolean isEncrypted = false;
}
