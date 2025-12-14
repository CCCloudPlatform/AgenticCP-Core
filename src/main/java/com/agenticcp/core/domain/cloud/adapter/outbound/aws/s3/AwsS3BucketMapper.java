package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.ObjectStorageErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.Tag;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS S3 버킷을 CloudResource 엔티티로 변환하는 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3BucketMapper {

    private final ObjectMapper objectMapper;
    private final CloudRegionRepository cloudRegionRepository;

    /**
     * AWS S3 버킷을 CloudResource 엔티티로 변환
     * 
     * @param bucket AWS S3 버킷 정보
     * @param provider CloudProvider 엔티티 (S3 서비스용)
     * @param versioningStatus 버킷 버전 관리 상태 (선택적)
     * @param lifecycleConfig 버킷 라이프사이클 설정 (선택적)
     * @param tags 버킷 태그 (선택적)
     * @return CloudResource 엔티티
     * @throws BusinessException 매핑 과정에서 오류가 발생한 경우
     */
    public CloudResource toCloudResource(Bucket bucket, 
                                       CloudProvider provider,
                                       GetBucketVersioningResponse versioningStatus,
                                       BucketLifecycleConfiguration lifecycleConfig,
                                       List<Tag> tags) {
        
        log.debug("Converting AWS S3 bucket to CloudResource: {}", bucket.name());
        
        try {
            // 입력 검증
            validateBucketInput(bucket, provider);
            
            return CloudResource.builder()
                    .resourceId(bucket.name())
                    .resourceName(bucket.name())
                    .displayName(bucket.name())
                    .provider(provider)
                    .resourceType(CloudResource.ResourceType.BUCKET)
                    .lifecycleState(CloudResource.LifecycleState.RUNNING)
                    .instanceType("S3_BUCKET")
                    .instanceSize("STANDARD")
                    .storageGb(0L) // S3 버킷은 스토리지 용량이 동적이므로 0으로 설정
                    .tags(toTagMap(tags))
                    .configuration(buildConfigurationJson(bucket, versioningStatus, lifecycleConfig))
                    .createdInCloud(toLocalDateTime(bucket.creationDate()))
                    .lastModifiedInCloud(toLocalDateTime(bucket.creationDate()))
                    .lastSync(LocalDateTime.now())
                    .metadata(buildMetadata(bucket, versioningStatus, lifecycleConfig, tags))
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert AWS S3 bucket to CloudResource: {}", bucket.name(), e);
            throw new BusinessException(CloudErrorCode.MAPPING_FAILED, 
                    "S3 버킷을 CloudResource로 변환하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * AWS S3 버킷을 CloudResource 엔티티로 변환 (기본 정보만)
     * 
     * @param bucket AWS S3 버킷 정보
     * @param provider CloudProvider 엔티티
     * @return CloudResource 엔티티
     * @throws BusinessException 매핑 과정에서 오류가 발생한 경우
     */
    public CloudResource toCloudResource(Bucket bucket, CloudProvider provider) {
        return toCloudResource(bucket, provider, null, null, null);
    }

    /**
     * S3 리전 ID 문자열(예: "ap-northeast-2")을
     * DB에 저장된 CloudRegion 엔티티로 변환합니다.
     *
     * @param regionId AWS SDK가 반환한 리전 ID (us-east-1의 경우 null일 수 있음)
     * @return CloudRegion 엔티티
     * @throws ResourceNotFoundException DB에서 해당 리전을 찾을 수 없는 경우
     * @throws BusinessException 리전 ID가 유효하지 않은 경우
     */
    public CloudRegion toCloudRegion(String regionId) {
        log.debug("Converting region ID to CloudRegion: {}", regionId);
        
        String effectiveRegionId = regionId;

        if (effectiveRegionId == null || effectiveRegionId.isEmpty()) {
            effectiveRegionId = "us-east-1";
        }

        // 리전 ID 형식 검증
        if (!isValidRegionId(effectiveRegionId)) {
            log.error("Invalid region ID format: {}", effectiveRegionId);
            throw new BusinessException(CloudErrorCode.INVALID_REGION,
                    "유효하지 않은 AWS 리전 ID입니다: " + effectiveRegionId);
        }

        String finalRegionId = effectiveRegionId;
        return cloudRegionRepository.findByRegionKey(finalRegionId)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_REGION_NOT_FOUND));
    }

    /**
     * AWS S3 버킷 리스트를 CloudResource 리스트로 변환
     * 
     * @param listBucketsResponse AWS S3 버킷 리스트 응답
     * @param provider CloudProvider 엔티티
     * @return CloudResource 리스트
     * @throws BusinessException 변환 과정에서 오류가 발생한 경우
     */
    public List<CloudResource> toCloudResources(ListBucketsResponse listBucketsResponse, CloudProvider provider) {
        log.debug("Converting AWS S3 bucket list to CloudResource list, count: {}", 
                listBucketsResponse.buckets().size());
        
        try {
            if (listBucketsResponse == null || listBucketsResponse.buckets() == null) {
                log.warn("Empty or null bucket list response");
                return List.of();
            }
            
            return listBucketsResponse.buckets().stream()
                    .map(bucket -> {
                        try {
                            return toCloudResource(bucket, provider);
                        } catch (Exception e) {
                            log.warn("Failed to convert bucket to CloudResource: {}", bucket.name(), e);
                            throw new BusinessException(CloudErrorCode.MAPPING_FAILED, 
                                    "S3 버킷 변환 중 오류가 발생했습니다: " + bucket.name());
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to convert S3 bucket list to CloudResource list", e);
            throw new BusinessException(CloudErrorCode.MAPPING_FAILED, 
                    "S3 버킷 목록을 CloudResource로 변환하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 버킷 설정 정보를 JSON 문자열로 구성
     * 
     * @param bucket AWS S3 버킷
     * @param versioningStatus 버전 관리 상태
     * @param lifecycleConfig 라이프사이클 설정
     * @return 설정 JSON 문자열
     */
    private String buildConfigurationJson(Bucket bucket, 
                                        GetBucketVersioningResponse versioningStatus,
                                        BucketLifecycleConfiguration lifecycleConfig) {
        
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 기본 버킷 설정
            config.put("bucketName", bucket.name());
            config.put("creationDate", bucket.creationDate());
            
            // 버전 관리 설정
            if (versioningStatus != null) {
                config.put("versioning", Map.of(
                    "status", versioningStatus.status() != null ? versioningStatus.status().toString() : "Disabled",
                    "mfaDelete", versioningStatus.mfaDelete() != null ? versioningStatus.mfaDelete().toString() : "Disabled"
                ));
            }
            
            // 라이프사이클 설정
            if (lifecycleConfig != null && lifecycleConfig.rules() != null) {
                config.put("lifecycle", Map.of(
                    "rulesCount", lifecycleConfig.rules().size(),
                    "hasRules", !lifecycleConfig.rules().isEmpty()
                ));
            }
            
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.warn("Failed to serialize configuration for bucket: {}", bucket.name(), e);
            throw new BusinessException(CloudErrorCode.CLOUD_METADATA_SERIALIZATION_FAILED, 
                    "S3 버킷 설정 직렬화에 실패했습니다: " + bucket.name());
        }
    }

    /**
     * 버킷의 메타데이터를 JSON 문자열로 구성
     * 
     * @param bucket AWS S3 버킷
     * @param versioningStatus 버전 관리 상태
     * @param lifecycleConfig 라이프사이클 설정
     * @param tags 태그 정보
     * @return 메타데이터 JSON 문자열
     */
    private String buildMetadata(Bucket bucket, 
                               GetBucketVersioningResponse versioningStatus,
                               BucketLifecycleConfiguration lifecycleConfig,
                               List<Tag> tags) {
        
        try {
            Map<String, Object> metadata = new HashMap<>();
            
            // 기본 버킷 정보
            metadata.put("bucketName", bucket.name());
            metadata.put("creationDate", bucket.creationDate() != null ? 
                        bucket.creationDate().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
            
            // 버전 관리 정보
            if (versioningStatus != null) {
                metadata.put("versioningStatus", versioningStatus.status() != null ? 
                            versioningStatus.status().toString() : "Disabled");
                metadata.put("mfaDelete", versioningStatus.mfaDelete() != null ? 
                            versioningStatus.mfaDelete().toString() : "Disabled");
            }
            
            // 라이프사이클 설정 정보
            if (lifecycleConfig != null && lifecycleConfig.rules() != null) {
                metadata.put("lifecycleRules", lifecycleConfig.rules().size());
                metadata.put("hasLifecycleRules", !lifecycleConfig.rules().isEmpty());
            }
            
            // 태그 정보
            if (tags != null && !tags.isEmpty()) {
                Map<String, String> tagMap = tags.stream()
                        .collect(Collectors.toMap(
                                Tag::key,
                                Tag::value,
                                (existing, replacement) -> replacement
                        ));
                metadata.put("tags", tagMap);
                metadata.put("tagCount", tags.size());
            }
            
            // S3 특화 정보
            metadata.put("serviceType", "S3");
            metadata.put("storageClass", "STANDARD");
            metadata.put("encryptionEnabled", false); // 기본값, 실제로는 버킷 암호화 설정 확인 필요
            
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata for bucket: {}", bucket.name(), e);
            throw new BusinessException(CloudErrorCode.CLOUD_METADATA_SERIALIZATION_FAILED, 
                    "S3 버킷 메타데이터 직렬화에 실패했습니다: " + bucket.name());
        }
    }

    /**
     * 버킷 생성 날짜를 LocalDateTime으로 변환
     * 
     * @param instant AWS SDK의 Instant
     * @return LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 태그 리스트를 Map으로 변환
     * 
     * @param tags AWS S3 태그 리스트
     * @return 태그 Map
     */
    private Map<String, String> toTagMap(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return new HashMap<>();
        }
        
        return tags.stream()
                .collect(Collectors.toMap(
                        Tag::key,
                        Tag::value,
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * S3 버킷 입력 검증
     * 
     * @param bucket AWS S3 버킷
     * @param provider CloudProvider 엔티티
     * @throws BusinessException 입력이 유효하지 않은 경우
     */
    private void validateBucketInput(Bucket bucket, CloudProvider provider) {
        if (bucket == null) {
            log.error("Bucket is null");
            throw new BusinessException(ObjectStorageErrorCode.BUCKET_NOT_FOUND, "S3 버킷 정보가 없습니다.");
        }
        
        if (bucket.name() == null || bucket.name().trim().isEmpty()) {
            log.error("Bucket name is null or empty");
            throw new BusinessException(ObjectStorageErrorCode.INVALID_BUCKET_NAME, "S3 버킷 이름이 유효하지 않습니다.");
        }
        
        if (provider == null) {
            log.error("CloudProvider is null");
            throw new BusinessException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND, "클라우드 프로바이더 정보가 없습니다.");
        }
        
        // S3 버킷 이름 형식 검증
        if (!isValidBucketName(bucket.name())) {
            log.error("Invalid S3 bucket name format: {}", bucket.name());
            throw new BusinessException(ObjectStorageErrorCode.INVALID_BUCKET_NAME,
                    "유효하지 않은 S3 버킷 이름 형식입니다: " + bucket.name());
        }
    }

    /**
     * AWS 리전 ID 형식 검증
     * 
     * @param regionId 리전 ID
     * @return 유효한 형식인지 여부
     */
    private boolean isValidRegionId(String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return false;
        }
        
        // AWS 리전 ID 형식: [region]-[direction]-[number] (예: us-east-1, ap-northeast-2)
        return regionId.matches("^[a-z]+-[a-z]+-[0-9]+$");
    }

    /**
     * S3 버킷 이름 형식 검증
     * 
     * @param bucketName 버킷 이름
     * @return 유효한 형식인지 여부
     */
    private boolean isValidBucketName(String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            return false;
        }
        
        // S3 버킷 이름 규칙:
        // - 3-63자 길이
        // - 소문자, 숫자, 점(.), 하이픈(-)만 허용
        // - 점(.)으로 시작하거나 끝날 수 없음
        // - 연속된 점(.)은 허용되지 않음
        if (bucketName.length() < 3 || bucketName.length() > 63) {
            return false;
        }
        
        if (!bucketName.matches("^[a-z0-9.-]+$")) {
            return false;
        }
        
        if (bucketName.startsWith(".") || bucketName.endsWith(".")) {
            return false;
        }
        
        if (bucketName.contains("..")) {
            return false;
        }
        
        return true;
    }
}
