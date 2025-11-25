package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.ObjectStorageErrorCode;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class AwsS3BucketManagementAdapter implements ObjectStorageManagementPort {

    private final AwsClientConfig awsClientConfig;
    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final AwsS3ErrorTranslator errorTranslator;

    /**
     * S3 버킷 생성, 태그 적용, 객체 소유권 및 잠금 설정을 수행합니다.
     */
    @Override
    public CloudResource createContainer(CreateObjectStorageContainerCommand command) {
        String bucketName = command.getContainerName();
        Instant creationTime = Instant.now();

        try (S3Client s3Client = awsClientConfig.createS3Client(command.getSession(), command.getRegion())) {
            log.info("Creating S3 bucket: {} in region {}", bucketName, command.getRegion());

            performCreateBucket(
                    s3Client,
                    bucketName,
                    command.getRegion(),
                    command.getObjectOwnership(),
                    command.getObjectLockEnabled()
            );

            if (command.getTags() != null && !command.getTags().isEmpty()) {
                setBucketTags(s3Client, bucketName, command.getTags());
            }

            Bucket createdBucket = Bucket.builder()
                    .name(bucketName)
                    .creationDate(creationTime)
                    .build();

            return mapper.toCloudResource(createdBucket, findAwsProvider());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create S3 bucket: {}", bucketName, e);
            throw errorTranslator.translate(e);
        }
    }

    @Override
    public void deleteContainer(CloudSessionCredential session, String containerName) {
        log.info("Deleting S3 bucket: {}", containerName);

        try (S3Client s3Client = awsClientConfig.createS3Client(session, null)) {
            if (!isBucketEmpty(s3Client, containerName)) {
                throw new BusinessException(
                        ObjectStorageErrorCode.BUCKET_OPERATION_FAILED,
                        "Bucket is not empty. Use force delete instead."
                );
            }

            deleteBucket(s3Client, containerName);
            log.info("Successfully deleted S3 bucket: {}", containerName);
        } catch (BusinessException e) {
            throw e;
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket not found: {}", containerName);
            throw new ResourceNotFoundException(ObjectStorageErrorCode.BUCKET_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to delete S3 bucket: {}", containerName, e);
            throw errorTranslator.translate(e);
        }
    }

    @Override
    public CloudResource updateContainer(UpdateObjectStorageContainerCommand command) {
        String containerName = command.getContainerName();
        log.info("Updating S3 bucket: {} with command: {}", containerName, command);

        try (S3Client s3Client = awsClientConfig.createS3Client(command.getSession(), null)) {
            if (!checkBucketExists(s3Client, containerName)) {
                throw new ResourceNotFoundException(ObjectStorageErrorCode.BUCKET_NOT_FOUND);
            }

            if (command.getVersioningEnabled() != null) {
                setBucketVersioning(s3Client, containerName, command.getVersioningEnabled());
            }

            if (command.getTags() != null) {
                setBucketTags(s3Client, containerName, command.getTags());
            }

            Bucket updatedBucket = Bucket.builder()
                    .name(containerName)
                    .build();

            CloudResource resource = mapper.toCloudResource(updatedBucket, findAwsProvider());
            log.info("Successfully updated S3 bucket: {}", containerName);
            return resource;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update S3 bucket: {}", containerName, e);
            throw errorTranslator.translate(e);
        }
    }

    @Override
    public void forceDeleteContainer(String containerName, CloudSessionCredential session) {
        log.info("Force deleting S3 bucket: {}", containerName);

        try (S3Client s3Client = awsClientConfig.createS3Client(session, null)) {
            if (!checkBucketExists(s3Client, containerName)) {
                throw new ResourceNotFoundException(ObjectStorageErrorCode.BUCKET_NOT_FOUND);
            }

            emptyBucket(s3Client, containerName);
            deleteBucket(s3Client, containerName);
            log.info("Successfully force deleted S3 bucket: {}", containerName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to force delete S3 bucket: {}", containerName, e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * 내부 Helper 메서드들
     */

    private void performCreateBucket(S3Client s3Client,
                                     String bucketName,
                                     String region,
                                     String objectOwnership,
                                     Boolean objectLock) {
        CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                .bucket(bucketName);

        if (region != null && !region.isBlank() && !"us-east-1".equals(region)) {
            builder.createBucketConfiguration(
                    CreateBucketConfiguration.builder()
                            .locationConstraint(BucketLocationConstraint.fromValue(region))
                            .build()
            );
        }

        if (objectOwnership != null) {
            builder.objectOwnership(ObjectOwnership.fromValue(objectOwnership));
        }

        if (objectLock != null) {
            builder.objectLockEnabledForBucket(objectLock);
        }

        try {
            s3Client.createBucket(builder.build());
        } catch (BucketAlreadyOwnedByYouException e) {
            log.warn("S3 bucket {} already owned by requester. Treating as success.", bucketName);
        } catch (BucketAlreadyExistsException e) {
            throw new BusinessException(ObjectStorageErrorCode.BUCKET_ALREADY_EXISTS);
        }
    }

    private void deleteBucket(S3Client s3Client, String bucketName) {
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

    private boolean checkBucketExists(S3Client s3Client, String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    private boolean isBucketEmpty(S3Client s3Client, String bucketName) {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .maxKeys(1)
                            .build()
            );
            return response.contents().isEmpty();
        } catch (NoSuchBucketException e) {
            return true;
        } catch (Exception e) {
            log.error("Failed to check bucket content state: {}", bucketName, e);
            throw new BusinessException(ObjectStorageErrorCode.BUCKET_OPERATION_FAILED);
        }
    }

    private void emptyBucket(S3Client s3Client, String bucketName) {
        String keyMarker = null;
        String versionIdMarker = null;

        log.debug("Emptying S3 bucket: {}", bucketName);

        while (true) {
            ListObjectVersionsResponse response = s3Client.listObjectVersions(
                    ListObjectVersionsRequest.builder()
                            .bucket(bucketName)
                            .keyMarker(keyMarker)
                            .versionIdMarker(versionIdMarker)
                            .build()
            );

            List<ObjectIdentifier> objectsToDelete = new ArrayList<>();

            response.versions().forEach(version ->
                    objectsToDelete.add(
                            ObjectIdentifier.builder()
                                    .key(version.key())
                                    .versionId(version.versionId())
                                    .build()
                    )
            );

            response.deleteMarkers().forEach(marker ->
                    objectsToDelete.add(
                            ObjectIdentifier.builder()
                                    .key(marker.key())
                                    .versionId(marker.versionId())
                                    .build()
                    )
            );

            if (!objectsToDelete.isEmpty()) {
                s3Client.deleteObjects(
                        DeleteObjectsRequest.builder()
                                .bucket(bucketName)
                                .delete(Delete.builder().objects(objectsToDelete).build())
                                .build()
                );
            }

            if (response.isTruncated()) {
                keyMarker = response.nextKeyMarker();
                versionIdMarker = response.nextVersionIdMarker();
            } else {
                break;
            }
        }
    }

    private void setBucketVersioning(S3Client s3Client, String bucketName, boolean enabled) {
        try {
            s3Client.putBucketVersioning(
                    PutBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .versioningConfiguration(
                                    VersioningConfiguration.builder()
                                            .status(enabled ? BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED)
                                            .build()
                            )
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to update versioning for bucket {}", bucketName, e);
            throw new BusinessException(ObjectStorageErrorCode.BUCKET_OPERATION_FAILED);
        }
    }

    private void setBucketTags(S3Client s3Client, String bucketName, Map<String, String> tags) {
        try {
            List<Tag> tagList = tags.entrySet().stream()
                    .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            s3Client.putBucketTagging(
                    PutBucketTaggingRequest.builder()
                            .bucket(bucketName)
                            .tagging(Tagging.builder().tagSet(tagList).build())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to set tags for bucket {}", bucketName, e);
            throw new BusinessException(CloudErrorCode.CLOUD_TAG_OPERATION_FAILED);
        }
    }

    private CloudProvider findAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }
}
