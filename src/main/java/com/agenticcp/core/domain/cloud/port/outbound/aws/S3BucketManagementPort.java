package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.aws.CreateS3BucketCommand;
import com.agenticcp.core.domain.cloud.port.model.aws.UpdateS3BucketCommand;

/**
 * S3 버킷 관리 포트 - S3 버킷 생성, 수정, 삭제 기능을 정의하는 계약
 * 
 * S3 버킷은 일반적인 컴퓨팅 리소스와 달리 start/stop/terminate 생명주기가 없으므로
 * 별도의 관리 포트로 분리하여 명확한 역할을 부여합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface S3BucketManagementPort {

    /**
     * S3 버킷을 생성합니다.
     *
     * @return 생성된 CloudResource
     * @throws com.agenticcp.core.common.exception.BusinessException 버킷 이름 중복, 권한 없음, 잘못된 이름 형식
     */
    CloudResource createBucket(CreateS3BucketCommand command);

    /**
     * S3 버킷 설정을 업데이트합니다.
     *
     * @return 업데이트된 CloudResource
     * @throws com.agenticcp.core.common.exception.BusinessException 버킷 없음, 권한 없음
     */
    CloudResource updateBucket(UpdateS3BucketCommand command);

    /**
     * S3 버킷을 삭제합니다.
     * 
     * @param bucketName 버킷 이름
     * @throws com.agenticcp.core.common.exception.BusinessException 버킷 없음, 권한 없음, 버킷이 비어있지 않음
     */
    void deleteBucket(String bucketName);

    /**
     * S3 버킷을 강제 삭제합니다 (내용물 포함).
     * 
     * @param bucketName 버킷 이름
     * @throws com.agenticcp.core.common.exception.BusinessException 버킷 없음, 권한 없음
     */
    void forceDeleteBucket(String bucketName);
}
