package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.S3ErrorCode;
import com.agenticcp.core.domain.cloud.exception.AwsErrorCode;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.time.Instant;
import java.util.ArrayList;
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
public class AwsS3BucketManagementAdapter implements ObjectStorageManagementPort, ProviderScoped {

    private final S3Client s3Client;
    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CredentialProviderPort credentialProviderPort;

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     * S3 버킷 생성, 태그 적용, 객체 소유권 및 잠금 설정을 수행합니다.
     */
    @Override
    public CloudResource createContainer(CreateObjectStorageContainerCommand command) {
        log.info("Attempting to create S3 bucket: {} in region: {}",
                command.getContainerName(), command.getRegion());

        // 자격증명 해결
        resolveCredentials();

        Instant creationTime = Instant.now();

        try {
            CreateBucketRequest.Builder requestBuilder = CreateBucketRequest.builder()
                    .bucket(command.getContainerName());

            // 리전 설정 (us-east-1은 기본 리전이므로 LocationConstraint 불필요)
            String region = command.getRegion();
            if (region != null && !region.isEmpty() && !region.equals("us-east-1")) {
                requestBuilder.createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .locationConstraint(BucketLocationConstraint.fromValue(region))
                                .build()
                );
            }

            // 객체 소유권 설정 (ACL 비활성화 및 보안 강화)
            if (command.getObjectOwnership() != null) {
                requestBuilder.objectOwnership(ObjectOwnership.fromValue(command.getObjectOwnership()));
            }

            // 객체 잠금 설정
            if (command.getObjectLockEnabled() != null) {
                requestBuilder.objectLockEnabledForBucket(command.getObjectLockEnabled());
            }

            // 버킷 생성 실행
            s3Client.createBucket(requestBuilder.build());
            log.info("Successfully initiated bucket creation: {}", command.getContainerName());

        } catch (BucketAlreadyOwnedByYouException e) {
            // 멱등성(Idempotency) 처리: 이미 내가 소유한 버킷이면 성공으로 간주
            log.warn("S3 bucket {} already owned by you. Proceeding...", command.getContainerName());

        } catch (BucketAlreadyExistsException e) {
            // 이름 충돌: 다른 계정이 소유한 버킷
            log.error("S3 bucket name {} already exists (owned by another account).", command.getContainerName(), e);
            throw new BusinessException(S3ErrorCode.S3_BUCKET_ALREADY_EXISTS);

        } catch (Exception e) {
            // 그 외 AWS SDK 오류
            log.error("Failed to create S3 bucket: {}", command.getContainerName(), e);
            throw translateException(e);
        }

        // --- 버킷 생성 성공 또는 'AlreadyOwnedByYou'인 경우 ---

        try {
            // 태그 설정 (별도 API 호출)
            if (command.getTags() != null && !command.getTags().isEmpty()) {
                setBucketTags(command.getContainerName(), command.getTags());
            }

            Bucket createdBucketInfo = Bucket.builder()
                    .name(command.getContainerName())
                    .creationDate(creationTime)
                    .build();

            CloudProvider awsProvider = getAwsProvider();
            CloudResource resource = mapper.toCloudResource(createdBucketInfo, awsProvider);

            log.info("Successfully created/verified S3 bucket resource: {}", command.getContainerName());
            return resource;

        } catch (Exception e) {
            log.error("Failed during post-creation processing (tagging/mapping) for bucket: {}",
                    command.getContainerName(), e);
            throw new BusinessException(CloudErrorCode.CLOUD_TAG_OPERATION_FAILED);
        }
    }

    @Override
    public void deleteContainer(String containerName) {
        log.info("Deleting S3 bucket: {}", containerName);
        
        // 자격증명 해결
        resolveCredentials();
        
        try {
            // 버킷이 비어있는지 확인
            if (!isBucketEmpty(containerName)) {
                throw new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED, "Bucket is not empty. Use forceDeleteContainer to delete non-empty bucket: " + containerName);
            }
            
            // 버킷 삭제
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(containerName)
                    .build();
            
            s3Client.deleteBucket(request);
            log.info("Successfully deleted S3 bucket: {}", containerName);
            
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket not found: {}", containerName);
            throw new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to delete S3 bucket: {}", containerName, e);
            throw translateException(e);
        }
    }

    @Override
    public void forceDeleteContainer(String containerName) {
        log.info("Force deleting S3 bucket: {}", containerName);
        
        // 자격증명 해결
        resolveCredentials();
        
        try {
            // 버킷 내 모든 객체 삭제
            deleteAllObjects(containerName);
            
            // 버킷 삭제
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(containerName)
                    .build();
            
            s3Client.deleteBucket(request);
            
            log.info("Successfully force deleted S3 bucket: {}", containerName);
            
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket not found: {}", containerName);
            throw new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to force delete S3 bucket: {}", containerName, e);
            throw translateException(e);
        }
    }

    @Override
    public CloudResource updateContainer(UpdateObjectStorageContainerCommand command) {
        String containerName = command.getContainerName();
        log.info("Updating S3 bucket: {} with command: {}", containerName, command);

        // 자격증명 해결
        resolveCredentials();

        try {
            if (!bucketExists(containerName)) {
                throw new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
            }

            if (command.getVersioningEnabled() != null) {
                setBucketVersioning(containerName, command.getVersioningEnabled());
            }

            if (command.getTags() != null) {
                setBucketTags(containerName, command.getTags());
            }

            CloudProvider awsProvider = getAwsProvider();
            Bucket updatedBucketInfo = Bucket.builder()
                    .name(containerName)
                    .build();

            CloudResource resource = mapper.toCloudResource(updatedBucketInfo, awsProvider);
            log.info("Successfully updated S3 bucket: {}", containerName);
            return resource;
        } catch (Exception e) {
            log.error("Failed to update S3 bucket: {}", containerName, e);
            throw translateException(e);
        }
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
            
        } catch (NoSuchBucketException e) {
            log.warn("isBucketEmpty check failed, bucket {} not found.", bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to check if bucket is empty: {}", bucketName, e);
            throw new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED);
        }
    }

    /**
     * 버킷 내 모든 객체를 삭제합니다.
     */
    private void deleteAllObjects(String bucketName) {
        log.debug("Starting to empty all versions and delete markers from bucket: {}", bucketName);

        String keyMarker = null;
        String versionIdMarker = null;

        try {
            while (true) {
                ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
                        .bucket(bucketName)
                        .keyMarker(keyMarker)
                        .versionIdMarker(versionIdMarker)
                        .build();

                ListObjectVersionsResponse response = s3Client.listObjectVersions(request);

                List<ObjectIdentifier> objectsToDelete = new ArrayList<>();

                // 모든 버전 추가
                response.versions().stream()
                        .map(v -> ObjectIdentifier.builder().key(v.key()).versionId(v.versionId()).build())
                        .forEach(objectsToDelete::add);

                // 모든 삭제 마커 추가
                response.deleteMarkers().stream()
                        .map(m -> ObjectIdentifier.builder().key(m.key()).versionId(m.versionId()).build())
                        .forEach(objectsToDelete::add);

                // 수집된 객체가 있으면 DeleteObjects API 호출
                if (!objectsToDelete.isEmpty()) {
                    log.debug("Deleting {} versions/markers from bucket {}", objectsToDelete.size(), bucketName);
                    DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(objectsToDelete).build())
                            .build();
                    s3Client.deleteObjects(deleteRequest);
                }

                // 루프 종료 조건 확인
                if (response.isTruncated()) {
                    keyMarker = response.nextKeyMarker();
                    versionIdMarker = response.nextVersionIdMarker();
                } else {
                    break;
                }
            }
            log.debug("Successfully emptied bucket: {}", bucketName);

        } catch (NoSuchBucketException e) {
            // 이미 버킷이 없다면, 작업 완료로 간주
            log.warn("deleteAllObjects: Bucket {} not found. Assuming empty.", bucketName);
        } catch (Exception e) {
            log.error("Failed to delete all objects from bucket: {}", bucketName, e);
            throw new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED);
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
            throw new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED);
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
            throw new BusinessException(CloudErrorCode.CLOUD_TAG_OPERATION_FAILED);
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
     * 자격증명을 해결합니다.
     * CredentialProviderPort를 통해 테넌트별 자격증명을 조회하고 ThreadLocal 캐시에 저장합니다.
     */
    private void resolveCredentials() {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKey();
            if (tenantKey == null) {
                log.warn("테넌트 컨텍스트가 설정되지 않음 - 기본 자격증명 사용");
                return;
            }

            String accountScope = getAccountScopeFromContext();
            CloudProvider.ProviderType providerType = CloudProvider.ProviderType.AWS;

            log.debug("자격증명 해결 시작: tenantKey={}, providerType={}, accountScope={}", 
                    tenantKey, providerType, accountScope);

            // CredentialProviderPort를 통해 자격증명 해결
            AwsCredentials credentials = (AwsCredentials) credentialProviderPort.resolveCredentials(
                    tenantKey, providerType, accountScope);

            if (credentials != null) {
                log.debug("자격증명 해결 완료: tenantKey={}", tenantKey);
            } else {
                log.warn("자격증명 해결 결과가 null: tenantKey={}", tenantKey);
            }

        } catch (Exception e) {
            log.error("자격증명 해결 실패: {}", e.getMessage(), e);
            throw new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID, 
                    "자격증명 해결에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 컨텍스트에서 계정 스코프 정보를 추출합니다.
     * 현재는 기본값을 반환하지만, 향후 요청 헤더나 컨텍스트에서 추출할 수 있습니다.
     */
    private String getAccountScopeFromContext() {
        // TODO: 실제 요청 컨텍스트에서 계정 스코프 정보 추출
        // 예: HTTP 헤더, MDC, 또는 별도 컨텍스트 홀더에서 추출
        return "default"; // 기본 계정 스코프
    }

    /**
     * AWS Provider를 조회합니다.
     */
    private CloudProvider getAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }

    /**
     * AWS 예외를 비즈니스 예외로 변환합니다.
     */
    private RuntimeException translateException(Exception e) {
        log.error("AWS S3 operation failed", e);
        
        if (e instanceof software.amazon.awssdk.services.s3.model.NoSuchBucketException) {
            return new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
        }
        
        if (e instanceof software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException) {
            return new BusinessException(S3ErrorCode.S3_BUCKET_ALREADY_EXISTS);
        }
        
        if (e instanceof software.amazon.awssdk.services.s3.model.S3Exception) {
            software.amazon.awssdk.services.s3.model.S3Exception s3Exception = (software.amazon.awssdk.services.s3.model.S3Exception) e;
            String errorCode = s3Exception.awsErrorDetails().errorCode();
            
            switch (errorCode) {
                case "NoSuchBucket":
                    return new ResourceNotFoundException(S3ErrorCode.S3_BUCKET_NOT_FOUND);
                case "BucketAlreadyExists":
                    return new BusinessException(S3ErrorCode.S3_BUCKET_ALREADY_EXISTS);
                case "AccessDenied":
                    return new BusinessException(S3ErrorCode.S3_BUCKET_ACCESS_DENIED);
                case "InvalidBucketName":
                    return new BusinessException(S3ErrorCode.S3_BUCKET_INVALID_NAME);
                case "ServiceUnavailable":
                    return new BusinessException(AwsErrorCode.AWS_SERVICE_UNAVAILABLE);
                case "ThrottlingException":
                    return new BusinessException(AwsErrorCode.AWS_QUOTA_EXCEEDED);
                default:
                    return new BusinessException(S3ErrorCode.S3_BUCKET_OPERATION_FAILED);
            }
        }
        
        if (e instanceof software.amazon.awssdk.core.exception.SdkClientException) {
            return new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID);
        }
        
        if (e instanceof software.amazon.awssdk.core.exception.SdkServiceException) {
            return new BusinessException(AwsErrorCode.AWS_API_ERROR);
        }
        
        // 일반적인 예외
        return new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED);
    }
}
