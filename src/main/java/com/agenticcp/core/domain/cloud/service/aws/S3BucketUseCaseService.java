package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.port.model.S3BucketQuery;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * S3 버킷 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 S3 버킷 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션, 감사, 추적을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3BucketUseCaseService {

    private final S3BucketPortRouter router;
    private final CapabilityGuard capabilityGuard;
    private final CredentialProviderPort credentialProviderPort;
    private final TracingPort tracingPort;

    /**
     * S3 버킷 목록을 조회합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param query 조회 조건
     * @return S3 버킷 페이지
     */
    public Page<CloudResource> listBuckets(CloudProvider.ProviderType providerType, S3BucketQuery query) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.listBuckets", 
                Map.of("provider", providerType.name()))) {
            
            log.debug("S3 버킷 목록 조회 시작: provider={}, query={}", providerType, query);
            
            // 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // S3 버킷 목록 조회
            Page<CloudResource> page = router.discovery(providerType).listBuckets(query);
            log.info("S3 버킷 목록 조회 완료: provider={}, totalElements={}",
                    providerType, page.getTotalElements());
            
            return page;
        }
    }

    /**
     * 특정 S3 버킷을 조회합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @return S3 버킷 정보
     */
    public CloudResource getBucket(CloudProvider.ProviderType providerType, String bucketName) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.getBucket", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 조회 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // S3 버킷 조회
            CloudResource bucket = router.discovery(providerType).getBucket(bucketName)
                    .orElseThrow(() -> new IllegalArgumentException("S3 버킷을 찾을 수 없습니다: " + bucketName));
            
            log.info("S3 버킷 조회 완료: provider={}, bucketName={}", providerType, bucketName);
            
            return bucket;
        }
    }

    /**
     * S3 버킷 존재 여부를 확인합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @return 존재 여부
     */
    public boolean bucketExists(CloudProvider.ProviderType providerType, String bucketName) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.bucketExists", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 존재 확인 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // S3 버킷 존재 확인
            boolean exists = router.discovery(providerType).bucketExists(bucketName);
            
            log.info("S3 버킷 존재 확인 완료: provider={}, bucketName={}, exists={}", 
                    providerType, bucketName, exists);
            
            return exists;
        }
    }

    /**
     * S3 버킷을 생성합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @param region 리전 (선택적)
     * @param tags 태그 (선택적)
     * @return 생성된 S3 버킷 정보
     */
    @Transactional
    public CloudResource createBucket(CloudProvider.ProviderType providerType, String bucketName, 
                                    String region, Map<String, String> tags) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.createBucket", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 생성 시작: provider={}, bucketName={}, region={}", 
                    providerType, bucketName, region);
            
            // 1. Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            
            // 2. 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // 3. S3 버킷 생성
            CloudResource bucket = router.management(providerType).createBucket(bucketName, region, tags);
            
            log.info("S3 버킷 생성 완료: provider={}, bucketName={}, region={}", 
                    providerType, bucketName, region);
            
            return bucket;
        }
    }

    /**
     * S3 버킷을 삭제합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     */
    @Transactional
    public void deleteBucket(CloudProvider.ProviderType providerType, String bucketName) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.deleteBucket", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 삭제 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 1. Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            
            // 2. 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // 3. S3 버킷 삭제
            router.management(providerType).deleteBucket(bucketName);
            
            log.info("S3 버킷 삭제 완료: provider={}, bucketName={}", providerType, bucketName);
        }
    }

    /**
     * S3 버킷을 강제 삭제합니다 (내용물 포함).
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     */
    @Transactional
    public void forceDeleteBucket(CloudProvider.ProviderType providerType, String bucketName) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.forceDeleteBucket", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 강제 삭제 시작: provider={}, bucketName={}", providerType, bucketName);
            
            // 1. Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            
            // 2. 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // 3. S3 버킷 강제 삭제
            router.management(providerType).forceDeleteBucket(bucketName);
            
            log.info("S3 버킷 강제 삭제 완료: provider={}, bucketName={}", providerType, bucketName);
        }
    }

    /**
     * S3 버킷 설정을 업데이트합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @param bucketName 버킷 이름
     * @param versioningEnabled 버전 관리 활성화 여부
     * @param tags 태그 (선택적)
     * @return 업데이트된 S3 버킷 정보
     */
    @Transactional
    public CloudResource updateBucket(CloudProvider.ProviderType providerType, String bucketName, 
                                    Boolean versioningEnabled, Map<String, String> tags) throws Exception {
        try (AutoCloseable span = tracingPort.startSpan("s3.updateBucket", 
                Map.of("provider", providerType.name(), "bucketName", bucketName))) {
            
            log.debug("S3 버킷 업데이트 시작: provider={}, bucketName={}, versioningEnabled={}", 
                    providerType, bucketName, versioningEnabled);
            
            // 1. Capability 검증
            capabilityGuard.ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            
            // 2. 자격증명 확인
            credentialProviderPort.resolveCredentials(TenantContextHolder.getCurrentTenantKey(), providerType, null);
            
            // 3. S3 버킷 업데이트
            CloudResource bucket = router.management(providerType).updateBucket(bucketName, versioningEnabled, tags);
            
            log.info("S3 버킷 업데이트 완료: provider={}, bucketName={}, versioningEnabled={}", 
                    providerType, bucketName, versioningEnabled);
            
            return bucket;
        }
    }
}
