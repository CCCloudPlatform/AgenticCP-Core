package com.agenticcp.core.domain.cloud.port.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * S3 버킷 업데이트 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateS3BucketRequest {
    
    /**
     * 버전 관리 활성화 여부 (선택적)
     * - 객체의 여러 버전을 유지할지 여부
     */
    private Boolean versioningEnabled;
    
    /**
     * 태그 (선택적)
     * - 버킷 분류 및 관리를 위한 키-값 쌍
     * - 기존 태그를 업데이트하거나 새로 추가
     */
    private Map<String, String> tags;

}
