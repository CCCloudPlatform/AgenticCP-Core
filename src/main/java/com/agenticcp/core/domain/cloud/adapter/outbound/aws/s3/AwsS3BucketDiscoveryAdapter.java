package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.port.model.storage.ObjectStorageContainerQuery;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.S3ErrorCode;
import com.agenticcp.core.domain.cloud.exception.AwsErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;
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
public class AwsS3BucketDiscoveryAdapter implements ObjectStorageDiscoveryPort, ProviderScoped {

    private final S3Client s3Client;
    private final ResourceGroupsTaggingApiClient taggingClient;
    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final ObjectMapper objectMapper;
    private final CredentialProviderPort credentialProviderPort;

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    @Override
    public Page<CloudResource> listContainers(ObjectStorageContainerQuery query) {
        log.debug("Listing S3 buckets using Resource Groups Tagging API with query: {}", query);

        try {
            // 자격증명 해결
            resolveCredentials();
            CloudProvider awsProvider = getAwsProvider();
            GetResourcesRequest.Builder requestBuilder = GetResourcesRequest.builder()
                    .resourceTypeFilters("s3:bucket");

            if (query.getTagsEquals() != null && !query.getTagsEquals().isEmpty()) {
                List<TagFilter> tagFilters = query.getTagsEquals().entrySet().stream()
                        .map(entry -> TagFilter.builder().key(entry.getKey()).values(entry.getValue()).build())
                        .collect(Collectors.toList());
                requestBuilder.tagFilters(tagFilters);
            }

            List<CloudResource> allFilteredBuckets = new ArrayList<>();
            String paginationToken = null;

            do {
                requestBuilder.paginationToken(paginationToken);
                GetResourcesResponse response = taggingClient.getResources(requestBuilder.build());

                for (ResourceTagMapping mapping : response.resourceTagMappingList()) {
                    String arn = mapping.resourceARN();
                    String bucketName = arn.substring(arn.lastIndexOf(":") + 1);

                    Bucket bucketInfo = Bucket.builder()
                            .name(bucketName)
                            .creationDate(null)
                            .build();

                    CloudResource resource = mapper.toCloudResource(bucketInfo, awsProvider);
                    Map<String, String> tagsMap = mapping.tags().stream()
                            .collect(Collectors.toMap(
                                    software.amazon.awssdk.services.resourcegroupstaggingapi.model.Tag::key,
                                    software.amazon.awssdk.services.resourcegroupstaggingapi.model.Tag::value
                            ));
                    try {
                        resource.setTags(objectMapper.writeValueAsString(tagsMap));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to serialize tags for bucket {}: {}", bucketName, e.getMessage());
                        resource.setTags("{}"); // 실패 시 빈 JSON 객체
                    }
                    allFilteredBuckets.add(resource);
                }
                paginationToken = response.paginationToken();

            } while (paginationToken != null && !paginationToken.isEmpty());

            List<CloudResource> finalBuckets = allFilteredBuckets;
            if (query.getNameContains() != null && !query.getNameContains().isEmpty()) {
                finalBuckets = finalBuckets.stream()
                        .filter(b -> b.getResourceName().contains(query.getNameContains()))
                        .collect(Collectors.toList());
            }

            applySorting(finalBuckets, query.getSortBy(), query.getSortDirection());

            Sort sort = (query.getSortBy() != null)
                    ? Sort.by(Sort.Direction.fromString(query.getSortDirection()), query.getSortBy())
                    : Sort.unsorted();

            PageRequest pageRequest = PageRequest.of(query.getPage(), query.getSize(), sort);

            int start = (int) pageRequest.getOffset();
            int end = Math.min(start + pageRequest.getPageSize(), finalBuckets.size());

            List<CloudResource> pagedBuckets = (start > end) ? Collections.emptyList() : finalBuckets.subList(start, end);

            return new PageImpl<>(pagedBuckets, pageRequest, finalBuckets.size());

        } catch (Exception e) {
            log.error("Failed to list S3 buckets using Tagging API", e);
            throw translateException(e);
        }
    }

    @Override
    public Optional<CloudResource> getContainer(String containerName) {
        log.debug("Getting S3 bucket details for: {}", containerName);

        try {
            // 자격증명 해결
            resolveCredentials();
            s3Client.headBucket(HeadBucketRequest.builder().bucket(containerName).build());
            log.debug("Bucket found: {}", containerName);

            CloudProvider awsProvider = getAwsProvider();
            Bucket bucketInfo = Bucket.builder()
                    .name(containerName)
                    .build();

            CloudResource resource = mapper.toCloudResource(bucketInfo, awsProvider);
            try {
                GetBucketLocationResponse locResponse = s3Client.getBucketLocation(
                        r -> r.bucket(containerName));

                String regionId = locResponse.locationConstraintAsString();
                if (regionId == null || regionId.isEmpty()) {
                    regionId = "us-east-1";
                }

                resource.setRegion(mapper.toCloudRegion(regionId));

                GetBucketTaggingResponse tagsResponse = s3Client.getBucketTagging(
                        r -> r.bucket(containerName));

                Map<String, String> tagsMap = tagsResponse.tagSet().stream()
                        .collect(Collectors.toMap(
                                Tag::key,
                                Tag::value
                        ));
                try {
                    resource.setTags(objectMapper.writeValueAsString(tagsMap));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize tags for bucket {}: {}", containerName, e.getMessage());
                    resource.setTags("{}");
                }

            } catch (S3Exception e) {
                if (e.awsErrorDetails().errorCode().equals("NoSuchTagSet")) {
                    log.debug("Bucket {} has no tags.", containerName);
                    resource.setTags("{}");
                } else {
                    log.warn("Could not retrieve details (tags, location) for bucket {}: {}",
                            containerName, e.getMessage());
                }
            }

            log.debug("Successfully retrieved S3 bucket details: {}", containerName);
            return Optional.of(resource);

        } catch (NoSuchBucketException e) {
            log.debug("S3 bucket not found: {}", containerName, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get S3 bucket: {}", containerName, e);
            throw translateException(e);
        }
    }

    @Override
    public boolean containerExists(String containerName) {
        log.debug("Checking if S3 bucket exists: {}", containerName);
        
        try {
            // 자격증명 해결
            resolveCredentials();
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(containerName)
                    .build();
            
            s3Client.headBucket(request);
            return true;
            
        } catch (NoSuchBucketException e) {
            log.debug("S3 bucket does not exist: {}", containerName);
            return false;
        } catch (Exception e) {
            log.error("Failed to check bucket existence: {}", containerName, e);
            throw translateException(e);
        }
    }

    /**
     * (In-memory) 정렬 수행
     */
    private void applySorting(List<CloudResource> buckets, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isEmpty()) {
            return;
        }

        Comparator<CloudResource> comparator;
        if ("name".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(CloudResource::getResourceName);
        } else {
            log.warn("Unsupported sort key: {}. Defaulting to name sort.", sortBy);
            comparator = Comparator.comparing(CloudResource::getResourceName);
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        buckets.sort(comparator);
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
        
        if (e instanceof S3Exception) {
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
        
        if (e instanceof SdkClientException) {
            return new BusinessException(AwsErrorCode.AWS_CREDENTIALS_INVALID);
        }
        
        if (e instanceof SdkServiceException) {
            return new BusinessException(AwsErrorCode.AWS_API_ERROR);
        }
        
        // 일반적인 예외
        return new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED);
    }
}
