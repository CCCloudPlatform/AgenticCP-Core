package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.aws.*;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * S3 버킷 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 S3 버킷 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션, 감사, 추적을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3BucketUseCaseService {

    private final S3BucketPortRouter router;
    private final CapabilityGuard capabilityGuard;
    private final CredentialProviderPort credentialProviderPort;
    
    /**
     * 테넌트 컨텍스트 설정 및 자격증명 해결
     */
    private void setupTenantContextAndCredentials(CloudProvider.ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKey();
        credentialProviderPort.resolveCredentials(tenantKey, providerType, accountScope);
    }

    /**
     * S3 버킷을 생성합니다.
     *
     * @return 생성된 S3 버킷 정보
     */
    @Transactional
    public CloudResource createBucket(CloudProvider.ProviderType providerType, CreateS3BucketRequest request) {
            log.debug("S3 버킷 생성 시작: provider={}, request={}", providerType, request);

            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, request.getRegion());

            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);

            CreateS3BucketCommand command = CreateS3BucketCommand.builder()
                    .bucketName(request.getBucketName())
                    .region(request.getRegion())
                    .tags(request.getTags())
                    .objectOwnership(request.getObjectOwnership())
                    .objectLockEnabled(request.getObjectLockEnabled())
                    .build();

            CloudResource bucket = router.management(providerType).createBucket(command);

            log.info("S3 버킷 생성 완료: provider={}, bucketName={}", providerType, request.getBucketName());

            return bucket;
    }

    /**
     * S3 버킷 설정을 업데이트합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @return 업데이트된 S3 버킷 정보
     */
    @Transactional
    public CloudResource updateBucket(CloudProvider.ProviderType providerType, String bucketName,
                                      UpdateS3BucketRequest request) {

        log.debug("S3 버킷 업데이트 시작: provider={}, bucketName={}, request={}", providerType, bucketName, request);

        // 테넌트 컨텍스트 설정 및 자격증명 해결
        setupTenantContextAndCredentials(providerType, null);

        // Capability 검증
        capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);

        // S3 버킷 업데이트
        UpdateS3BucketCommand command = UpdateS3BucketCommand.builder()
                .bucketName(bucketName)
                .versioningEnabled(request.getVersioningEnabled())
                .tags(request.getTags())
                .build();

        CloudResource bucket = router.management(providerType).updateBucket(command);

        log.info("S3 버킷 업데이트 완료: provider={}, bucketName={}", providerType, bucketName);

        return bucket;
    }

    /**
     * S3 버킷 목록을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param query 조회 조건
     * @return S3 버킷 페이지
     */
    public Page<CloudResource> listBuckets(CloudProvider.ProviderType providerType, S3BucketQuery query) {
            log.debug("S3 버킷 목록 조회 시작: provider={}, query={}", providerType, query);

            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);

            // S3 버킷 목록 조회
            Page<CloudResource> page = router.discovery(providerType).listBuckets(query);
            log.info("S3 버킷 목록 조회 완료: provider={}, totalElements={}",
                    providerType, page.getTotalElements());

            return page;
    }

    /**
     * 특정 S3 버킷을 조회합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @return S3 버킷 정보
     */
    public CloudResource getBucket(CloudProvider.ProviderType providerType, String bucketName) {
            log.debug("S3 버킷 조회 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // S3 버킷 조회
            CloudResource bucket = router.discovery(providerType).getBucket(bucketName)
                    .orElseThrow(() -> new IllegalArgumentException("S3 버킷을 찾을 수 없습니다: " + bucketName));
            
            log.info("S3 버킷 조회 완료: provider={}, bucketName={}", providerType, bucketName);

            return bucket;
    }

    /**
     * S3 버킷 존재 여부를 확인합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @return 존재 여부
     */
    public boolean bucketExists(CloudProvider.ProviderType providerType, String bucketName) {
            log.debug("S3 버킷 존재 확인 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // S3 버킷 존재 확인
            boolean exists = router.discovery(providerType).bucketExists(bucketName);
            
            log.info("S3 버킷 존재 확인 완료: provider={}, bucketName={}, exists={}", 
                    providerType, bucketName, exists);
            
            return exists;
    }

    /**
     * S3 버킷을 삭제합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     */
    @Transactional
    public void deleteBucket(CloudProvider.ProviderType providerType, String bucketName) {
            log.debug("S3 버킷 삭제 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // S3 버킷 삭제
            router.management(providerType).deleteBucket(bucketName);
            
            log.info("S3 버킷 삭제 완료: provider={}, bucketName={}", providerType, bucketName);
    }

    /**
     * S3 버킷을 강제 삭제합니다 (내용물 포함).
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     */
    @Transactional
    public void forceDeleteBucket(CloudProvider.ProviderType providerType, String bucketName) {
            log.debug("S3 버킷 강제 삭제 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            
            // 테넌트 컨텍스트 설정 및 자격증명 해결
            setupTenantContextAndCredentials(providerType, null);
            
            // S3 버킷 강제 삭제
            router.management(providerType).forceDeleteBucket(bucketName);
            
            log.info("S3 버킷 강제 삭제 완료: provider={}, bucketName={}", providerType, bucketName);
    }
}
