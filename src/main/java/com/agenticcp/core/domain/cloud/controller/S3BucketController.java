package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.cloud.port.model.CreateS3BucketRequest;
import com.agenticcp.core.domain.cloud.port.model.S3BucketQuery;
import com.agenticcp.core.domain.cloud.port.model.UpdateS3BucketRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.service.aws.S3BucketUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

/**
 * S3 버킷 관리 REST API 컨트롤러
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/s3/buckets")
@RequiredArgsConstructor
@Tag(name = "S3 Bucket Management", description = "S3 버킷 관리 API")
public class S3BucketController {

    private final S3BucketUseCaseService s3BucketUseCaseService;

    @GetMapping
    @PreAuthorize("hasAuthority('S3_BUCKET_READ') or hasRole('ADMIN')")
    @AuditRequired(
        action = "LIST_BUCKETS",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 목록 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "S3 버킷 목록 조회",
        description = "지정된 클라우드 프로바이더의 S3 버킷 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<Page<CloudResource>>> listBuckets(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        String username = getCurrentUsername(request);
        log.info("[S3BucketController] listBuckets - user={}, provider={}, page={}, size={}", 
                username, provider, page, size);

        try {
            S3BucketQuery query = S3BucketQuery.builder()
                    .page(page)
                    .size(size)
                    .build();

            Page<CloudResource> buckets = s3BucketUseCaseService.listBuckets(provider, query);
            return ResponseEntity.ok(ApiResponse.success(buckets, "S3 버킷 목록 조회에 성공했습니다."));

        } catch (Exception e) {
            log.error("[S3BucketController] listBuckets - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 목록 조회 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/{bucketName}")
    @PreAuthorize("hasAuthority('S3_BUCKET_READ') or hasRole('ADMIN')")
    @AuditRequired(
        action = "GET_BUCKET",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 상세 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "S3 버킷 조회",
        description = "지정된 S3 버킷의 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<CloudResource>> getBucket(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "버킷 이름", required = true)
            @PathVariable String bucketName) {

        log.info("[S3BucketController] getBucket - provider={}, bucketName={}", provider, bucketName);

        try {
            CloudResource bucket = s3BucketUseCaseService.getBucket(provider, bucketName);
            return ResponseEntity.ok(ApiResponse.success(bucket, "S3 버킷 조회에 성공했습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("[S3BucketController] getBucket - bucket not found: {}", bucketName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CommonErrorCode.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("[S3BucketController] getBucket - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 조회 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/{bucketName}/exists")
    @PreAuthorize("hasAuthority('S3_BUCKET_READ') or hasRole('ADMIN')")
    @AuditRequired(
        action = "CHECK_BUCKET_EXISTS",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 존재 확인",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "S3 버킷 존재 확인",
        description = "지정된 S3 버킷의 존재 여부를 확인합니다."
    )
    public ResponseEntity<ApiResponse<Boolean>> bucketExists(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "버킷 이름", required = true)
            @PathVariable String bucketName) {

        log.info("[S3BucketController] bucketExists - provider={}, bucketName={}", provider, bucketName);

        try {
            boolean exists = s3BucketUseCaseService.bucketExists(provider, bucketName);
            return ResponseEntity.ok(ApiResponse.success(exists, "S3 버킷 존재 확인에 성공했습니다."));

        } catch (Exception e) {
            log.error("[S3BucketController] bucketExists - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 존재 확인 실패: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S3_BUCKET_CREATE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "CREATE_BUCKET",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 생성",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(
        summary = "S3 버킷 생성",
        description = "새로운 S3 버킷을 생성합니다."
    )
    public ResponseEntity<ApiResponse<CloudResource>> createBucket(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "S3 버킷 생성 요청", required = true)
            @Valid @RequestBody CreateS3BucketRequest request) {

        log.info("[S3BucketController] createBucket - provider={}, bucketName={}, region={}", 
                provider, request.getBucketName(), request.getRegion());

        try {
            CloudResource bucket = s3BucketUseCaseService.createBucket(
                    provider, 
                    request.getBucketName(), 
                    request.getRegion(), 
                    request.getTags());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(bucket, "S3 버킷 생성에 성공했습니다."));

        } catch (Exception e) {
            log.error("[S3BucketController] createBucket - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 생성 실패: " + e.getMessage()));
        }
    }

    @PutMapping("/{bucketName}")
    @PreAuthorize("hasAuthority('S3_BUCKET_UPDATE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "UPDATE_BUCKET",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 설정 업데이트",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(
        summary = "S3 버킷 업데이트",
        description = "기존 S3 버킷의 설정을 업데이트합니다."
    )
    public ResponseEntity<ApiResponse<CloudResource>> updateBucket(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "버킷 이름", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "S3 버킷 업데이트 요청", required = true)
            @Valid @RequestBody UpdateS3BucketRequest request) {

        log.info("[S3BucketController] updateBucket - provider={}, bucketName={}, versioningEnabled={}", 
                provider, bucketName, request.getVersioningEnabled());

        try {
            CloudResource bucket = s3BucketUseCaseService.updateBucket(
                    provider, 
                    bucketName, 
                    request.getVersioningEnabled(), 
                    request.getTags());
            
            return ResponseEntity.ok(ApiResponse.success(bucket, "S3 버킷 업데이트에 성공했습니다."));

        } catch (Exception e) {
            log.error("[S3BucketController] updateBucket - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 업데이트 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{bucketName}")
    @PreAuthorize("hasAuthority('S3_BUCKET_DELETE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "DELETE_BUCKET",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 삭제",
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "S3 버킷 삭제",
        description = "지정된 S3 버킷을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteBucket(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "버킷 이름", required = true)
            @PathVariable String bucketName) {

        log.info("[S3BucketController] deleteBucket - provider={}, bucketName={}", provider, bucketName);

        try {
            s3BucketUseCaseService.deleteBucket(provider, bucketName);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("[S3BucketController] deleteBucket - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 삭제 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{bucketName}/force")
    @PreAuthorize("hasAuthority('S3_BUCKET_DELETE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "FORCE_DELETE_BUCKET",
        resourceType = AuditResourceType.S3_BUCKET,
        description = "S3 버킷 강제 삭제 (내용물 포함)",
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "S3 버킷 강제 삭제",
        description = "지정된 S3 버킷을 내용물과 함께 강제 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> forceDeleteBucket(
            @Parameter(description = "클라우드 프로바이더 타입", required = true)
            @RequestParam CloudProvider.ProviderType provider,
            @Parameter(description = "버킷 이름", required = true)
            @PathVariable String bucketName) {

        log.info("[S3BucketController] forceDeleteBucket - provider={}, bucketName={}", provider, bucketName);

        try {
            s3BucketUseCaseService.forceDeleteBucket(provider, bucketName);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("[S3BucketController] forceDeleteBucket - error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 강제 삭제 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 현재 인증된 사용자명을 가져옵니다.
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            Authentication authentication = (Authentication) request.getUserPrincipal();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to get current username", e);
        }
        return "anonymous";
    }
}
