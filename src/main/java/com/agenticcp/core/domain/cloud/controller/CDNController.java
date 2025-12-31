package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.dto.*;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import com.agenticcp.core.domain.cloud.service.cdn.CDNUseCaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * CDN Distribution 관리 REST API 컨트롤러
 * 
 * AWS CloudFront Distribution의 생성, 조회, 수정, 삭제 및 캐시 무효화 기능을 제공합니다.
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/cloudfront/distributions")
@RequiredArgsConstructor
@Tag(name = "CDN Distribution Management", description = "CDN Distribution 관리 API (CloudFront 지원)")
public class CDNController {

    private static final String SERVICE_KEY = "CloudFront";
    private static final String RESOURCE_TYPE = "CDN_DISTRIBUTION";

    private final CDNUseCaseService cdnUseCaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Distribution 생성 ====================

    /**
     * CDN Distribution을 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입 (AWS)
     * @param accountScope 계정 스코프
     * @param request Distribution 생성 요청
     * @return 생성된 Distribution 리소스
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CDN_DISTRIBUTION_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "CREATE_CDN_DISTRIBUTION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN Distribution 생성",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.HIGH
    )
    @Operation(
        summary = "CDN Distribution 생성",
        description = "새로운 CDN Distribution을 생성합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Distribution 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> createDistribution(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "CDN Distribution 생성 요청", required = true)
            @Valid @RequestBody CDNDistributionCreateRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[CDNController] createDistribution - provider={}, accountScope={}, distributionName={}", 
                provider, accountScope, request.getDistributionName());
        
        // Request DTO를 Command로 변환
        CreateDistributionCommand command = CreateDistributionCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(request.getAccountScope())
                .serviceKey(SERVICE_KEY)
                .resourceType(RESOURCE_TYPE)
                .distributionName(request.getDistributionName())
                .comment(request.getComment())
                .enabled(request.getEnabled())
                .origin(request.getOrigin())
                .cacheBehaviors(request.getCacheBehaviors())
                .aliases(request.getAliases())
                .sslCertificateId(request.getSslCertificateId())
                .tags(request.getTags())
                .build();
        
        CloudResource distribution = cdnUseCaseService.createDistribution(command);
        
        log.info("[CDNController] createDistribution - success resourceId={}", distribution.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(distribution, "CDN Distribution 생성에 성공했습니다."));
    }

    // ==================== Distribution 조회 ====================

    /**
     * CDN Distribution 목록을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionName Distribution 이름 필터 (선택)
     * @param enabled 활성화 여부 필터 (선택)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param sortBy 정렬 기준 (기본값: name)
     * @param sortDirection 정렬 방향 (기본값: asc)
     * @return Distribution 리소스 목록 (페이징 정보 포함)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CDN_DISTRIBUTION_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "LIST_CDN_DISTRIBUTIONS",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN Distribution 목록 조회",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.LOW
    )
    @Operation(
        summary = "CDN Distribution 목록 조회",
        description = "지정된 클라우드 프로바이더의 CDN Distribution 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Distribution 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Page<CloudResource>>> listDistributions(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution 이름 필터", example = "my-distribution")
            @RequestParam(required = false) String distributionName,

            @Parameter(description = "활성화 여부 필터", example = "true")
            @RequestParam(required = false) Boolean enabled,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기 (1-100)", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "정렬 기준", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "정렬 방향 (asc, desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        log.info("[CDNController] listDistributions - provider={}, accountScope={}, page={}, size={}", 
                provider, accountScope, page, size);
        
        // 페이징 파라미터 검증
        if (page < 0) {
            throw new com.agenticcp.core.common.exception.BusinessException(
                    CloudErrorCode.INVALID_REQUEST, "페이지 번호는 0 이상이어야 합니다");
        }
        if (size < 1 || size > 100) {
            throw new com.agenticcp.core.common.exception.BusinessException(
                    CloudErrorCode.INVALID_REQUEST, "페이지 크기는 1 이상 100 이하여야 합니다");
        }
        if (sortDirection != null && !sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new com.agenticcp.core.common.exception.BusinessException(
                    CloudErrorCode.INVALID_REQUEST, "정렬 방향은 'asc' 또는 'desc'여야 합니다");
        }
        
        CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .distributionName(distributionName)
                .enabled(enabled)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        Page<CloudResource> distributions = cdnUseCaseService.listDistributions(query);
        
        log.info("[CDNController] listDistributions - success count={}", distributions.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(distributions, "CDN Distribution 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 CDN Distribution을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @return Distribution 리소스
     */
    @GetMapping("/{distributionId}")
    @PreAuthorize("hasAuthority('CDN_DISTRIBUTION_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "GET_CDN_DISTRIBUTION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN Distribution 상세 조회",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.LOW
    )
    @Operation(
        summary = "CDN Distribution 상세 조회",
        description = "특정 CDN Distribution의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Distribution 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Distribution을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> getDistribution(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution ID", required = true, example = "E2QWRUHAPOMQZL")
            @PathVariable String distributionId) {
        
        log.info("[CDNController] getDistribution - provider={}, accountScope={}, distributionId={}", 
                provider, accountScope, distributionId);
        
        Optional<CloudResource> distribution = cdnUseCaseService.getDistribution(accountScope, distributionId, provider);
        
        if (distribution.isPresent()) {
            log.info("[CDNController] getDistribution - success distributionId={}", distributionId);
            return ResponseEntity.ok(ApiResponse.success(distribution.get(), "CDN Distribution 조회에 성공했습니다."));
        } else {
            log.warn("[CDNController] getDistribution - not found distributionId={}", distributionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== Distribution 수정 ====================

    /**
     * CDN Distribution을 수정합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @param request Distribution 수정 요청
     * @return 수정된 Distribution 리소스
     */
    @PutMapping("/{distributionId}")
    @PreAuthorize("hasAuthority('CDN_DISTRIBUTION_UPDATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "UPDATE_CDN_DISTRIBUTION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN Distribution 설정 업데이트",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.HIGH
    )
    @Operation(
        summary = "CDN Distribution 수정",
        description = "CDN Distribution의 설정을 수정합니다. ETag 기반 동시성 제어가 적용됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Distribution 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Distribution을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동시성 충돌 (ETag 불일치)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> updateDistribution(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution ID", required = true, example = "E2QWRUHAPOMQZL")
            @PathVariable String distributionId,

            @Parameter(description = "CDN Distribution 수정 요청", required = true)
            @Valid @RequestBody CDNDistributionUpdateRequest request) {
        
        log.info("[CDNController] updateDistribution - provider={}, accountScope={}, distributionId={}", 
                provider, accountScope, distributionId);
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        // 현재 Distribution 조회하여 ETag 획득
        Optional<CloudResource> currentDistribution = cdnUseCaseService.getDistribution(
                accountScope, distributionId, provider);
        
        if (currentDistribution.isEmpty()) {
            log.warn("[CDNController] updateDistribution - not found distributionId={}", distributionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
        
        // metadata에서 ETag 추출 (간단한 예시, 실제로는 JSON 파싱 필요)
        String etag = extractEtagFromMetadata(currentDistribution.get().getMetadata());
        
        // Request DTO를 Command로 변환
        UpdateDistributionCommand command = UpdateDistributionCommand.builder()
                .providerType(request.getProviderType())
                .accountScope(request.getAccountScope())
                .distributionId(distributionId)
                .etag(etag)
                .comment(request.getComment())
                .enabled(request.getEnabled())
                .cacheBehaviors(request.getCacheBehaviors())
                .aliases(request.getAliases())
                .sslCertificateId(request.getSslCertificateId())
                .tags(request.getTags())
                .build();
        
        CloudResource distribution = cdnUseCaseService.updateDistribution(command);
        
        log.info("[CDNController] updateDistribution - success distributionId={}", distributionId);
        return ResponseEntity.ok(ApiResponse.success(distribution, "CDN Distribution 수정에 성공했습니다."));
    }

    // ==================== Distribution 삭제 ====================

    /**
     * CDN Distribution을 삭제합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{distributionId}")
    @PreAuthorize("hasAuthority('CDN_DISTRIBUTION_DELETE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "DELETE_CDN_DISTRIBUTION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN Distribution 삭제",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.CRITICAL
    )
    @Operation(
        summary = "CDN Distribution 삭제",
        description = "CDN Distribution을 삭제합니다. 삭제 전 자동으로 비활성화됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Distribution 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Distribution을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteDistribution(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution ID", required = true, example = "E2QWRUHAPOMQZL")
            @PathVariable String distributionId) {
        
        log.info("[CDNController] deleteDistribution - provider={}, accountScope={}, distributionId={}", 
                provider, accountScope, distributionId);
        
        // 현재 Distribution 조회하여 ETag 획득
        Optional<CloudResource> currentDistribution = cdnUseCaseService.getDistribution(
                accountScope, distributionId, provider);
        
        if (currentDistribution.isEmpty()) {
            log.warn("[CDNController] deleteDistribution - not found distributionId={}", distributionId);
            return ResponseEntity.notFound().build();
        }
        
        // metadata에서 ETag 추출
        String etag = extractEtagFromMetadata(currentDistribution.get().getMetadata());
        
        // DeleteDistributionCommand 생성
        DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .distributionId(distributionId)
                .etag(etag)
                .build();
        
        cdnUseCaseService.deleteDistribution(command);
        
        log.info("[CDNController] deleteDistribution - success distributionId={}", distributionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 캐시 무효화 ====================

    /**
     * 캐시 무효화를 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @param request 캐시 무효화 요청
     * @return 생성된 무효화 결과
     */
    @PostMapping("/{distributionId}/invalidations")
    @PreAuthorize("hasAuthority('CDN_INVALIDATION_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "CREATE_CDN_INVALIDATION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN 캐시 무효화 생성",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    @Operation(
        summary = "캐시 무효화 생성",
        description = "CDN Distribution의 캐시를 무효화합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "캐시 무효화 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Distribution을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CDNInvalidationResponse>> createInvalidation(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution ID", required = true, example = "E2QWRUHAPOMQZL")
            @PathVariable String distributionId,

            @Parameter(description = "캐시 무효화 요청", required = true)
            @Valid @RequestBody CDNInvalidationRequest request) {
        
        log.info("[CDNController] createInvalidation - provider={}, accountScope={}, distributionId={}, paths={}", 
                provider, accountScope, distributionId, request.getPaths());
        
        // CreateInvalidationCommand 생성
        CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                .accountScope(accountScope)
                .distributionId(distributionId)
                .paths(request.getPaths())
                .callerReference(request.getCallerReference())
                .build();
        
        InvalidationResult result = cdnUseCaseService.createInvalidation(command);
        CDNInvalidationResponse response = CDNInvalidationResponse.from(result);
        
        log.info("[CDNController] createInvalidation - success invalidationId={}", result.invalidationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "캐시 무효화 생성에 성공했습니다."));
    }

    /**
     * 캐시 무효화 상태를 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param distributionId Distribution ID
     * @param invalidationId 무효화 ID
     * @return 무효화 결과
     */
    @GetMapping("/{distributionId}/invalidations/{invalidationId}")
    @PreAuthorize("hasAuthority('CDN_INVALIDATION_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "GET_CDN_INVALIDATION",
        resourceType = AuditResourceType.CDN_DISTRIBUTION,
        description = "CDN 캐시 무효화 상태 조회",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.LOW
    )
    @Operation(
        summary = "캐시 무효화 상태 조회",
        description = "특정 캐시 무효화의 상태를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "무효화 상태 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "무효화를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CDNInvalidationResponse>> getInvalidation(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Distribution ID", required = true, example = "E2QWRUHAPOMQZL")
            @PathVariable String distributionId,

            @Parameter(description = "무효화 ID", required = true, example = "I2J3K4L5M6N7O")
            @PathVariable String invalidationId) {
        
        log.info("[CDNController] getInvalidation - provider={}, accountScope={}, distributionId={}, invalidationId={}", 
                provider, accountScope, distributionId, invalidationId);
        
        Optional<InvalidationResult> result = cdnUseCaseService.getInvalidation(
                accountScope, distributionId, invalidationId, provider);
        
        if (result.isPresent()) {
            CDNInvalidationResponse response = CDNInvalidationResponse.from(result.get());
            log.info("[CDNController] getInvalidation - success invalidationId={}", invalidationId);
            return ResponseEntity.ok(ApiResponse.success(response, "캐시 무효화 상태 조회에 성공했습니다."));
        } else {
            log.warn("[CDNController] getInvalidation - not found invalidationId={}", invalidationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    /**
     * metadata에서 ETag를 추출합니다.
     * 
     * @param metadata JSON 문자열 형태의 metadata
     * @return ETag 값, 없으면 null
     */
    private String extractEtagFromMetadata(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = objectMapper.readValue(metadata, Map.class);
            Object etagObj = metadataMap.get("etag");
            if (etagObj != null) {
                return etagObj.toString();
            }
        } catch (JsonProcessingException e) {
            log.warn("[CDNController] Failed to parse metadata JSON for ETag extraction: {}", e.getMessage());
        }
        
        return null;
    }
}

