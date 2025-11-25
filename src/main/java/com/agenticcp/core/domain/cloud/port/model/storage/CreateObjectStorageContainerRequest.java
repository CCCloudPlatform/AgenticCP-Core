package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * Object Storage Container 생성 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateObjectStorageContainerRequest {

    /**
     * 프로바이더 / 계정 스코프
     */
    CloudProvider.ProviderType providerType;
    String accountScope;

    /**
     * Container 이름 (필수)
     */
    @NotBlank(message = "Container 이름은 필수입니다")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
            message = "Container 이름은 소문자, 숫자, 하이픈만 허용되며, 시작과 끝은 문자나 숫자여야 합니다")
    @Size(min = 3, max = 63, message = "Container 이름은 3-63자 사이여야 합니다")
    @Masked(type = MaskingType.DEFAULT)
    private String containerName;

    /**
     * 클라우드 리전 (필수)
     * 예: us-east-1, ap-northeast-2
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 객체 소유권 (선택적, 권장)
     * - "BucketOwnerEnforced": ACL 비활성화, Container 소유자가 모든 객체 소유 (권장)
     * - "ObjectWriter": 업로더가 객체 소유
     */
    private String objectOwnership;

    /**
     * 객체 잠금 활성화 (선택적)
     * - 기본값 false. true로 설정 시 WORM 활성화 (생성 후 변경 불가)
     */
    private Boolean objectLockEnabled;

    /**
     * 태그 (선택적)
     * - Container 생성 후 별도 API로 적용됩니다.
     */
    private Map<String, String> tags;
}
