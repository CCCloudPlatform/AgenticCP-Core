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
 * @since 2025-11-05
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
     * S3 버킷을 생성합니다.
     * 버킷 생성 시 태그, 객체 소유권, 객체 잠금 설정을 함께 적용할 수 있습니다.
     *
     * @param command 버킷 생성 명령 (이름, 리전, 태그, 객체 소유권, 객체 잠금 등)
     * @return 생성된 버킷 정보
     * @throws BusinessException 버킷이 이미 존재하거나 생성 실패 시
     * @throws ResourceNotFoundException AWS 프로바이더를 찾을 수 없을 때
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

    /**
     * S3 버킷을 삭제합니다.
     * 버킷이 비어있지 않으면 삭제에 실패하며, forceDeleteContainer를 사용해야 합니다.
     *
     * @param session 클라우드 세션 인증 정보
     * @param containerName 삭제할 버킷 이름
     * @throws BusinessException 버킷이 비어있지 않거나 삭제 실패 시
     * @throws ResourceNotFoundException 버킷을 찾을 수 없을 때
     */
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

    /**
     * S3 버킷의 설정을 업데이트합니다.
     * 버전 관리 활성화/비활성화 및 태그 수정을 지원합니다.
     *
     * @param command 업데이트 명령 (버킷 이름, 버전 관리 설정, 태그 등)
     * @return 업데이트된 버킷 정보
     * @throws BusinessException 업데이트 실패 시
     * @throws ResourceNotFoundException 버킷을 찾을 수 없을 때
     */
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

    /**
     * S3 버킷을 강제 삭제합니다.
     * 버킷 내부의 모든 객체와 버전을 먼저 삭제한 후 버킷을 삭제합니다.
     *
     * @param containerName 삭제할 버킷 이름
     * @param session 클라우드 세션 인증 정보
     * @throws BusinessException 삭제 실패 시
     * @throws ResourceNotFoundException 버킷을 찾을 수 없을 때
     */
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
