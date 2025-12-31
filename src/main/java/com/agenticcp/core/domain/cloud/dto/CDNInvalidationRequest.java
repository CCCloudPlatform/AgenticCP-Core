package com.agenticcp.core.domain.cloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CDN 캐시 무효화 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CDNInvalidationRequest {

    /**
     * 무효화할 경로 목록 (필수)
     * 예: ["/images/*", "/css/*", "/index.html"]
     * 모든 경로를 무효화하려면 ["/*"] 사용
     * 최대 3000개까지 가능
     */
    @NotEmpty(message = "무효화할 경로 목록은 필수입니다")
    @jakarta.validation.constraints.Size(max = 3000, message = "무효화 경로는 최대 3000개까지 가능합니다")
    private List<@NotBlank(message = "경로는 비어있을 수 없습니다")
            @jakarta.validation.constraints.Pattern(
                    regexp = "^/.*|^\\*$",
                    message = "경로는 '/'로 시작하거나 '*'이어야 합니다"
            ) String> paths;

    /**
     * Caller Reference (고유 식별자, 중복 방지용)
     * 미제공 시 자동 생성됩니다.
     */
    private String callerReference;
}

