package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.aws.S3BucketQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * S3 버킷 발견 포트 - S3 버킷 조회 기능을 정의하는 계약
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface S3BucketDiscoveryPort {

    /**
     * S3 버킷 목록을 조회합니다.
     * 
     * @param query 조회 조건 (페이징, 필터링 포함)
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     * @throws com.agenticcp.core.common.exception.BusinessException 조회 권한 없음, 잘못된 쿼리 조건
     */
    Page<CloudResource> listBuckets(S3BucketQuery query);

    /**
     * 특정 S3 버킷을 조회합니다.
     * 
     * @param bucketName 버킷 이름 (null 불가)
     * @return CloudResource (존재하지 않으면 Optional.empty())
     * @throws com.agenticcp.core.common.exception.BusinessException 잘못된 버킷 이름 형식
     */
    Optional<CloudResource> getBucket(String bucketName);

    /**
     * 버킷 존재 여부를 확인합니다.
     * 
     * @param bucketName 버킷 이름
     * @return 존재 여부
     */
    boolean bucketExists(String bucketName);
}
