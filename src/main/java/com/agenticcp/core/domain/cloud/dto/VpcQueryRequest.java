package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * VPC 조회 쿼리 모델
 * 
 * VPC 목록 조회를 위한 표준화된 쿼리 모델
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpcQueryRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     */
    private String accountScope;

    /**
     * 리전
     * 예: us-east-1, ap-northeast-2
     */
    private String region;

    /**
     * VPC 이름 필터
     */
    private String vpcName;

    /**
     * CIDR 블록 필터
     */
    private String cidrBlock;

    /**
     * 태그 필터
     */
    private Map<String, String> tags;

    /**
     * 테넌트 키
     */
    private String tenantKey;

    /**
     * 페이지 번호 (0부터 시작)
     */
    private Integer page;

    /**
     * 페이지 크기
     */
    private Integer size;

    /**
     * 정렬 기준 필드
     */
    private String sortBy;

    /**
     * 정렬 방향 (ASC, DESC)
     */
    private String sortDirection;
}
