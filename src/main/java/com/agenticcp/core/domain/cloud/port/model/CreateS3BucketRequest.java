package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * S3 버킷 생성 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateS3BucketRequest {
    
    /**
     * 버킷 이름 (필수)
     * - 3-63자 사이
     * - 소문자, 숫자, 하이픈만 허용
     * - 전 세계적으로 고유해야 함
     */
    @NotBlank(message = "버킷 이름은 필수입니다")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", 
             message = "버킷 이름은 소문자, 숫자, 하이픈만 허용되며, 시작과 끝은 문자나 숫자여야 합니다")
    @Size(min = 3, max = 63, message = "버킷 이름은 3-63자 사이여야 합니다")
    @Masked(type = MaskingType.DEFAULT)
    private String bucketName;
    
    /**
     * AWS 리전 (필수)
     * - 버킷을 생성할 AWS 리전
     * - 예: us-east-1, ap-northeast-2, eu-west-1
     */
    @NotBlank(message = "리전은 필수입니다")
    @Size(max = 50, message = "리전은 50자 이하여야 합니다")
    private String region;
    
    /**
     * 태그 (선택적)
     * - 버킷 분류 및 관리를 위한 키-값 쌍
     * - 최대 50개 태그, 각 키와 값은 128자 이하
     */
    private Map<String, String> tags;
}
