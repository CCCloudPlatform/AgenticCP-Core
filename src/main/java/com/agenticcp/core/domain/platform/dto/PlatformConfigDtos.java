package com.agenticcp.core.domain.platform.dto;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * PlatformConfig API 요청/응답 DTO 묶음.
 * - CreateRequest: 생성 시 필수 필드(key/type/value/isSystem/description)
 * - UpdateRequest: 수정 시 필드(type/value/description)
 * - Response: 조회 응답(ENCRYPTED 값은 기본 마스킹, showSecret=true일 때만 평문)
 */
@SuppressWarnings("unused")
public class PlatformConfigDtos {

    @Data
    /** 생성 요청 DTO */
    public static class CreateRequest {
        @NotBlank
        private String key;
        @NotNull
        private PlatformConfig.ConfigType type;
        @NotNull
        private String value;
        private Boolean isSystem = false;
        private String description;
    }

    @Data
    /** 수정 요청 DTO */
    public static class UpdateRequest {
        @NotNull
        private PlatformConfig.ConfigType type;
        @NotNull
        private String value;
        private String description;
    }

    @Data
    @Builder
    /** 조회 응답 DTO */
    public static class Response {
        private String key;
        private String value;
        private PlatformConfig.ConfigType type;
        private Boolean isEncrypted;
        private Boolean isSystem;
        private String description;

        /** 엔티티를 응답 DTO로 변환. maskSecret=true면 ENCRYPTED 값을 마스킹 */
        public static Response of(PlatformConfig pc, boolean maskSecret) {
            String resolvedValue = pc.getIsEncrypted() == Boolean.TRUE && maskSecret && pc.getConfigValue() != null
                    ? "******"
                    : pc.getConfigValue();
            return Response.builder()
                    .key(pc.getConfigKey())
                    .value(resolvedValue)
                    .type(pc.getConfigType())
                    .isEncrypted(pc.getIsEncrypted())
                    .isSystem(pc.getIsSystem())
                    .description(pc.getDescription())
                    .build();
        }
    }
}


