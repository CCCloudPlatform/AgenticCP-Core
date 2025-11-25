package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;
import java.util.function.Function;
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

    private final AwsS3BucketMapper mapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final ObjectMapper objectMapper;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    private final AwsClientConfig awsClientConfig;
    private final AwsS3ErrorTranslator errorTranslator;

    @Override
    public Page<CloudResource> listContainers(ObjectStorageContainerQueryRequest query) {
        log.debug("Listing S3 buckets using Resource Groups Tagging API with query: {}", query);

        return executeWithTaggingClient(query.getAccountScope(), client -> {
            CloudProvider awsProvider = getAwsProvider();
            List<ResourceTagMapping> rawMappings = fetchAllTagMappings(client, query);

            List<CloudResource> resources = rawMappings.stream()
                    .map(mapping -> mapToCloudResource(mapping, awsProvider))
                    .collect(Collectors.toList());

            return applyMemoryOperations(resources, query);
        });
    }

    @Override
    public Optional<CloudResource> getContainer(String accountScope, String containerName) {
        log.debug("Getting S3 bucket details for: {}, accountScope={}", containerName, accountScope);

        return executeWithS3Client(accountScope, client -> {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(containerName).build());

                CloudResource resource = mapper.toCloudResource(
                        Bucket.builder().name(containerName).build(),
                        getAwsProvider()
                );

                enrichResourceDetails(client, containerName, resource);
                log.debug("Successfully retrieved S3 bucket details: {}", containerName);
                return Optional.of(resource);
            } catch (NoSuchBucketException e) {
                log.info("Bucket not found: {}", containerName);
                return Optional.empty();
            }
        });
    }

    @Override
    public boolean containerExists(String accountScope, String containerName) {
        log.debug("Checking if S3 bucket exists: {}, accountScope={}", containerName, accountScope);

        return executeWithS3Client(accountScope, client -> {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(containerName).build());
                return true;
            } catch (NoSuchBucketException e) {
                log.info("Bucket not found: {}", containerName);
                return false;
            }
        });
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    private <R> R executeWithS3Client(String accountScope, Function<S3Client, R> action) {
        String resolvedScope = requireAccountScope(accountScope);
        CloudSessionCredential session = acquireSession(resolvedScope);

        try (S3Client client = awsClientConfig.createS3Client(session, null)) {
            return action.apply(client);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw errorTranslator.translate(e);
        }
    }

    private <R> R executeWithTaggingClient(String accountScope, Function<ResourceGroupsTaggingApiClient, R> action) {
        String resolvedScope = requireAccountScope(accountScope);
        CloudSessionCredential session = acquireSession(resolvedScope);

        try (ResourceGroupsTaggingApiClient client =
                     awsClientConfig.createResourceGroupsTaggingApiClient(session, null)) {
            return action.apply(client);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw errorTranslator.translate(e);
        }
    }

    private List<ResourceTagMapping> fetchAllTagMappings(ResourceGroupsTaggingApiClient client,
                                                         ObjectStorageContainerQueryRequest query) {
        GetResourcesRequest.Builder requestBuilder = GetResourcesRequest.builder()
                .resourceTypeFilters("s3:bucket");

        if (query.getTagsEquals() != null && !query.getTagsEquals().isEmpty()) {
            List<TagFilter> tagFilters = query.getTagsEquals().entrySet().stream()
                    .map(entry -> TagFilter.builder().key(entry.getKey()).values(entry.getValue()).build())
                    .collect(Collectors.toList());
            requestBuilder.tagFilters(tagFilters);
        }

        List<ResourceTagMapping> allMappings = new ArrayList<>();
        String paginationToken = null;

        do {
            requestBuilder.paginationToken(paginationToken);
            GetResourcesResponse response = client.getResources(requestBuilder.build());
            allMappings.addAll(response.resourceTagMappingList());
            paginationToken = response.paginationToken();
        } while (paginationToken != null && !paginationToken.isEmpty());

        return allMappings;
    }

    private CloudResource mapToCloudResource(ResourceTagMapping mapping, CloudProvider provider) {
        String arn = mapping.resourceARN();
        String bucketName = arn.substring(arn.lastIndexOf(":") + 1);

        CloudResource resource = mapper.toCloudResource(
                Bucket.builder().name(bucketName).build(),
                provider
        );

        Map<String, String> tagsMap = mapping.tags().stream()
                .collect(Collectors.toMap(
                        software.amazon.awssdk.services.resourcegroupstaggingapi.model.Tag::key,
                        software.amazon.awssdk.services.resourcegroupstaggingapi.model.Tag::value
                ));
        setTagsJson(resource, tagsMap);
        return resource;
    }

    private void enrichResourceDetails(S3Client client, String bucketName, CloudResource resource) {
        try {
            GetBucketLocationResponse locResponse = client.getBucketLocation(r -> r.bucket(bucketName));
            String regionId = locResponse.locationConstraintAsString();
            if (regionId == null || regionId.isEmpty()) {
                regionId = "us-east-1";
            }
            resource.setRegion(mapper.toCloudRegion(regionId));
        } catch (S3Exception e) {
            log.warn("Could not retrieve location for bucket {}: {}", bucketName, e.getMessage());
        }

        try {
            GetBucketTaggingResponse tagsResponse = client.getBucketTagging(r -> r.bucket(bucketName));
            Map<String, String> tagsMap = tagsResponse.tagSet().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value));
            setTagsJson(resource, tagsMap);
        } catch (S3Exception e) {
            if ("NoSuchTagSet".equals(e.awsErrorDetails().errorCode())) {
                log.debug("Bucket {} has no tags.", bucketName);
            } else {
                log.warn("Could not retrieve tags for bucket {}: {}", bucketName, e.getMessage());
            }
            resource.setTags("{}");
        }
    }

    private void setTagsJson(CloudResource resource, Map<String, String> tags) {
        try {
            resource.setTags(objectMapper.writeValueAsString(tags));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tags: {}", e.getMessage());
            resource.setTags("{}");
        }
    }

    private Page<CloudResource> applyMemoryOperations(List<CloudResource> resources,
                                                      ObjectStorageContainerQueryRequest query) {
        List<CloudResource> filtered = resources;
        if (query.getNameContains() != null && !query.getNameContains().isEmpty()) {
            filtered = filtered.stream()
                    .filter(resource -> resource.getResourceName().contains(query.getNameContains()))
                    .collect(Collectors.toList());
        }

        applySorting(filtered, query.getSortBy(), query.getSortDirection());

        int page = query.getPage();
        int size = query.getSize();
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<CloudResource> paged = (start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

        Sort sort = (query.getSortBy() != null)
                ? Sort.by(resolveSortDirection(query.getSortDirection()), query.getSortBy())
                : Sort.unsorted();

        return new PageImpl<>(paged, PageRequest.of(page, size, sort), filtered.size());
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

    private Sort.Direction resolveSortDirection(String sortDirection) {
        return (sortDirection != null)
                ? Sort.Direction.fromString(sortDirection)
                : Sort.Direction.ASC;
    }

    /**
     * AWS Provider를 조회합니다.
     */
    private CloudProvider getAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }

    private String requireAccountScope(String accountScope) {
        if (accountScope == null || accountScope.isBlank()) {
            throw new BusinessException(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED, "AccountScope가 필요합니다.");
        }
        return accountScope;
    }

    private CloudSessionCredential acquireSession(String accountScope) {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            CloudSessionCredential session = accountCredentialManagementPort.getSession(
                    tenantKey, accountScope, getProviderType());
            log.debug("세션 획득 완료: accountScope={}, expiresAt={}", accountScope, session.getExpiresAt());
            return session;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("세션 획득 실패: accountScope={}", accountScope, e);
            throw new BusinessException(CredentialErrorCode.INVALID_CREDENTIALS,
                    "세션 획득에 실패했습니다: " + e.getMessage());
        }
    }
}
