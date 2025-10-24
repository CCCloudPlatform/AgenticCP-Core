package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AwsS3BucketMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AWS S3 버킷을 CloudResource 엔티티로 변환
     * 
     * @param bucket AWS S3 버킷 정보
     * @param provider CloudProvider 엔티티 (S3 서비스용)
     * @param versioningStatus 버킷 버전 관리 상태 (선택적)
     * @param lifecycleConfig 버킷 라이프사이클 설정 (선택적)
     * @param tags 버킷 태그 (선택적)
     * @return CloudResource 엔티티
     */
    public CloudResource toCloudResource(Bucket bucket, 
                                       CloudProvider provider,
                                       GetBucketVersioningResponse versioningStatus,
                                       BucketLifecycleConfiguration lifecycleConfig,
                                       List<Tag> tags) {
        
        log.debug("Converting AWS S3 bucket to CloudResource: {}", bucket.name());
        
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
                .tags(buildTagsJson(tags))
                .configuration(buildConfigurationJson(bucket, versioningStatus, lifecycleConfig))
                .createdInCloud(toLocalDateTime(bucket.creationDate()))
                .lastModifiedInCloud(toLocalDateTime(bucket.creationDate()))
                .lastSync(LocalDateTime.now())
                .metadata(buildMetadata(bucket, versioningStatus, lifecycleConfig, tags))
                .build();
    }

    /**
     * AWS S3 버킷을 CloudResource 엔티티로 변환 (기본 정보만)
     * 
     * @param bucket AWS S3 버킷 정보
     * @param provider CloudProvider 엔티티
     * @return CloudResource 엔티티
     */
    public CloudResource toCloudResource(Bucket bucket, CloudProvider provider) {
        return toCloudResource(bucket, provider, null, null, null);
    }

    /**
     * AWS S3 버킷 리스트를 CloudResource 리스트로 변환
     * 
     * @param listBucketsResponse AWS S3 버킷 리스트 응답
     * @param provider CloudProvider 엔티티
     * @return CloudResource 리스트
     */
    public List<CloudResource> toCloudResources(ListBucketsResponse listBucketsResponse, CloudProvider provider) {
        return listBucketsResponse.buckets().stream()
                .map(bucket -> toCloudResource(bucket, provider))
                .collect(Collectors.toList());
    }

    /**
     * 태그 리스트를 JSON 문자열로 변환
     * 
     * @param tags AWS S3 태그 리스트
     * @return 태그 JSON 문자열
     */
    private String buildTagsJson(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "{}";
        }
        
        Map<String, String> tagMap = toTagMap(tags);
        
        try {
            return objectMapper.writeValueAsString(tagMap);
        } catch (Exception e) {
            log.warn("Failed to serialize tags for bucket", e);
            return "{}";
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
        
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.warn("Failed to serialize configuration for bucket: {}", bucket.name(), e);
            return "{}";
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
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata for bucket: {}", bucket.name(), e);
            return "{}";
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
}
