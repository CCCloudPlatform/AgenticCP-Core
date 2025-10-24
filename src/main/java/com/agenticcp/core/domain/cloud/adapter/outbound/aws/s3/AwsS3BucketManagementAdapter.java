package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Map;

/**
 * AWS S3 버킷 관리 어댑터
 * AWS S3 API를 통해 버킷 생성, 수정, 삭제 기능을 제공
 * 
 * S3 버킷은 일반적인 컴퓨팅 리소스와 달리 start/stop/terminate 생명주기가 없으므로
 * 별도의 관리 어댑터로 분리하여 명확한 역할을 부여합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3BucketManagementAdapter implements S3BucketManagementPort, ProviderScoped {

    private final S3Client s3Client;
    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;

    @Override
    public CloudResource createBucket(String bucketName, String region, Map<String, String> tags) {
        log.info("Creating S3 bucket: {} in region: {}", bucketName, region);
        
        try {
            // 버킷 생성 요청 구성
            CreateBucketRequest.Builder requestBuilder = CreateBucketRequest.builder()
                    .bucket(bucketName);
            
            // 리전이 지정된 경우 설정
            if (region != null && !region.isEmpty()) {
                requestBuilder.createBucketConfiguration(
                    CreateBucketConfiguration.builder()
                            .locationConstraint(BucketLocationConstraint.fromValue(region))
                            .build()
                );
            }
            
            // 버킷 생성
            s3Client.createBucket(requestBuilder.build());
            
            // 태그 설정 (선택적)
            if (tags != null && !tags.isEmpty()) {
                setBucketTags(bucketName, tags);
            }
            
            // 생성된 버킷 정보 조회
            ListBucketsResponse listResponse = s3Client.listBuckets();
            Bucket createdBucket = listResponse.buckets().stream()
                    .filter(bucket -> bucket.name().equals(bucketName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Created bucket not found: " + bucketName));
            
            // AWS Provider 조회
            CloudProvider awsProvider = getAwsProvider();
            
            // 매퍼를 사용하여 CloudResource로 변환
            CloudResource resource = mapper.toCloudResource(createdBucket, awsProvider);
            
            log.info("Successfully created S3 bucket: {}", bucketName);
            return resource;
            
        } catch (BucketAlreadyExistsException e) {
            log.error("S3 bucket already exists: {}", bucketName);
            throw new RuntimeException("Bucket already exists: " + bucketName, e);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.error("S3 bucket already owned by you: {}", bucketName);
            throw new RuntimeException("Bucket already owned by you: " + bucketName, e);
        } catch (Exception e) {
            log.error("Failed to create S3 bucket: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        log.info("Deleting S3 bucket: {}", bucketName);
        
        try {
            // 버킷이 비어있는지 확인
            if (!isBucketEmpty(bucketName)) {
                throw new RuntimeException("Bucket is not empty. Use forceDeleteBucket to delete non-empty bucket: " + bucketName);
            }
            
            // 버킷 삭제
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.deleteBucket(request);
            
            log.info("Successfully deleted S3 bucket: {}", bucketName);
            
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket not found: {}", bucketName);
            throw new RuntimeException("Bucket not found: " + bucketName, e);
        } catch (Exception e) {
            log.error("Failed to delete S3 bucket: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public void forceDeleteBucket(String bucketName) {
        log.info("Force deleting S3 bucket: {}", bucketName);
        
        try {
            // 버킷 내 모든 객체 삭제
            deleteAllObjects(bucketName);
            
            // 버킷 삭제
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.deleteBucket(request);
            
            log.info("Successfully force deleted S3 bucket: {}", bucketName);
            
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket not found: {}", bucketName);
            throw new RuntimeException("Bucket not found: " + bucketName, e);
        } catch (Exception e) {
            log.error("Failed to force delete S3 bucket: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public CloudResource updateBucket(String bucketName, Boolean versioningEnabled, Map<String, String> tags) {
        log.info("Updating S3 bucket: {} with versioning: {}, tags: {}", bucketName, versioningEnabled, tags);
        
        try {
            // 버킷 존재 여부 확인
            if (!bucketExists(bucketName)) {
                throw new RuntimeException("Bucket not found: " + bucketName);
            }
            
            // 버전 관리 설정 (선택적)
            if (versioningEnabled != null) {
                setBucketVersioning(bucketName, versioningEnabled);
            }
            
            // 태그 설정 (선택적)
            if (tags != null && !tags.isEmpty()) {
                setBucketTags(bucketName, tags);
            }
            
            // 업데이트된 버킷 정보 조회
            ListBucketsResponse listResponse = s3Client.listBuckets();
            Bucket updatedBucket = listResponse.buckets().stream()
                    .filter(bucket -> bucket.name().equals(bucketName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Updated bucket not found: " + bucketName));
            
            // AWS Provider 조회
            CloudProvider awsProvider = getAwsProvider();
            
            // 매퍼를 사용하여 CloudResource로 변환
            CloudResource resource = mapper.toCloudResource(updatedBucket, awsProvider);
            
            log.info("Successfully updated S3 bucket: {}", bucketName);
            return resource;
            
        } catch (Exception e) {
            log.error("Failed to update S3 bucket: {}", bucketName, e);
            throw translateException(e);
        }
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     * 버킷이 비어있는지 확인합니다.
     */
    private boolean isBucketEmpty(String bucketName) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().isEmpty();
            
        } catch (Exception e) {
            log.warn("Failed to check if bucket is empty: {}", bucketName, e);
            return false;
        }
    }

    /**
     * 버킷 내 모든 객체를 삭제합니다.
     */
    private void deleteAllObjects(String bucketName) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            if (!listResponse.contents().isEmpty()) {
                // 모든 객체 삭제
                List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                        .map(s3Object -> ObjectIdentifier.builder()
                                .key(s3Object.key())
                                .build())
                        .toList();
                
                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder()
                                .objects(objectsToDelete)
                                .build())
                        .build();
                
                s3Client.deleteObjects(deleteRequest);
                log.info("Deleted {} objects from bucket: {}", objectsToDelete.size(), bucketName);
            }
            
        } catch (Exception e) {
            log.error("Failed to delete objects from bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to delete objects from bucket: " + bucketName, e);
        }
    }

    /**
     * 버킷 버전 관리를 설정합니다.
     */
    private void setBucketVersioning(String bucketName, boolean enabled) {
        try {
            PutBucketVersioningRequest request = PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(enabled ? BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED)
                            .build())
                    .build();
            
            s3Client.putBucketVersioning(request);
            log.debug("Set versioning for bucket {} to: {}", bucketName, enabled);
            
        } catch (Exception e) {
            log.error("Failed to set versioning for bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to set versioning for bucket: " + bucketName, e);
        }
    }

    /**
     * 버킷 태그를 설정합니다.
     */
    private void setBucketTags(String bucketName, Map<String, String> tags) {
        try {
            List<Tag> tagList = tags.entrySet().stream()
                    .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                    .toList();
            
            PutBucketTaggingRequest request = PutBucketTaggingRequest.builder()
                    .bucket(bucketName)
                    .tagging(Tagging.builder()
                            .tagSet(tagList)
                            .build())
                    .build();
            
            s3Client.putBucketTagging(request);
            log.debug("Set tags for bucket {}: {}", bucketName, tags);
            
        } catch (Exception e) {
            log.error("Failed to set tags for bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to set tags for bucket: " + bucketName, e);
        }
    }

    /**
     * 버킷 존재 여부를 확인합니다.
     */
    private boolean bucketExists(String bucketName) {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(request);
            return true;
            
        } catch (NoSuchBucketException e) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to check bucket existence: {}", bucketName, e);
            return false;
        }
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
