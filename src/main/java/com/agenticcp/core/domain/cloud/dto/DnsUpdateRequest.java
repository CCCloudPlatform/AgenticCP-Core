package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DNS 호스팅 존 수정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsUpdateRequest {

    /**
     * 호스팅 존 설명 변경
     */
    private String comment;

    /**
     * 추가할 태그
     */
    private Map<String, String> tagsToAdd;

    /**
     * 제거할 태그
     */
    private Map<String, String> tagsToRemove;

    /**
     * CSP별 특화 설정
     */
    private Map<String, Object> providerSpecificConfig;
}
