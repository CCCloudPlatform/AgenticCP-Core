package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.port.model.S3BucketQuery;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketDiscoveryPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS S3 버킷 발견 어댑터
 * AWS S3 API를 통해 버킷 조회 기능을 제공
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3BucketDiscoveryAdapter implements S3BucketDiscoveryPort, ProviderScoped {

    private final S3Client s3Client;
    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;

    @Override
    public Page<CloudResource> listBuckets(S3BucketQuery query) {
        log.debug("Listing S3 buckets with query: {}", query);
        
        try {
            // AWS S3 API 호출
            ListBucketsResponse response = s3Client.listBuckets();
            
            // AWS Provider 조회
            CloudProvider awsProvider = getAwsProvider();
            
            // 매퍼를 사용하여 CloudResource로 변환
            List<CloudResource> allBuckets = mapper.toCloudResources(response, awsProvider);
            
            // 필터링 적용
            List<CloudResource> filteredBuckets = applyFilters(allBuckets, query);
            
            // 페이징 적용
            PageRequest pageRequest = PageRequest.of(query.getPage(), query.getSize());
            int start = (int) pageRequest.getOffset();
            int end = Math.min(start + pageRequest.getPageSize(), filteredBuckets.size());
            
            List<CloudResource> pagedBuckets = filteredBuckets.subList(start, end);
            
            log.debug("Found {} S3 buckets (filtered from {})", pagedBuckets.size(), allBuckets.size());
            
            return new PageImpl<>(pagedBuckets, pageRequest, filteredBuckets.size());
            
        } catch (Exception e) {
            log.error("Failed to list S3 buckets", e);
            throw translateException(e);
        }
    }

    @Override
    public Optional<CloudResource> getBucket(String bucketName) {
        log.debug("Getting S3 bucket: {}", bucketName);
        
        try {
            // 버킷 존재 여부 확인
            if (!bucketExists(bucketName)) {
                log.debug("S3 bucket not found: {}", bucketName);
                return Optional.empty();
            }
            
            // 버킷 정보 조회
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headRequest);
            
            // 버킷 생성일 조회
            ListBucketsResponse listResponse = s3Client.listBuckets();
            Bucket bucket = listResponse.buckets().stream()
                    .filter(b -> b.name().equals(bucketName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Bucket not found: " + bucketName));
            
            // AWS Provider 조회
            CloudProvider awsProvider = getAwsProvider();
            
            // 매퍼를 사용하여 CloudResource로 변환
            CloudResource resource = mapper.toCloudResource(bucket, awsProvider);
            
            log.debug("Successfully retrieved S3 bucket: {}", bucketName);
            return Optional.of(resource);
            
        } catch (NoSuchBucketException e) {
            log.debug("S3 bucket not found: {}", bucketName);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get S3 bucket: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        log.debug("Checking if S3 bucket exists: {}", bucketName);
        
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(request);
            return true;
            
        } catch (NoSuchBucketException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check bucket existence: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     * 필터링 조건을 적용합니다.
     */
    private List<CloudResource> applyFilters(List<CloudResource> buckets, S3BucketQuery query) {
        return buckets.stream()
                .filter(bucket -> {
                    // 이름 포함 필터
                    if (query.getNameContains() != null && !query.getNameContains().isEmpty()) {
                        if (!bucket.getResourceName().toLowerCase().contains(query.getNameContains().toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // 태그 필터 (metadata에서 확인)
                    if (query.getTagsEquals() != null && !query.getTagsEquals().isEmpty()) {
                        // TODO: 태그 필터링 로직 구현
                        // 현재는 metadata에서 태그 정보를 확인해야 함
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * AWS Provider를 조회합니다.
     */
    private CloudProvider getAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new RuntimeException("AWS Provider not found"));
    }

    /**
     * AWS 예외를 비즈니스 예외로 변환합니다.
     */
    private RuntimeException translateException(Exception e) {
        // TODO: CloudErrorTranslator 구현 후 사용
        return new RuntimeException("AWS S3 operation failed: " + e.getMessage(), e);
    }
}
