package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.dto.DnsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.DnsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.DnsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.DnsResponse;
import com.agenticcp.core.domain.cloud.dto.DnsUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.service.dns.DnsUseCaseService;
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

import java.util.Optional;

/**
 * DNS 관리 REST API 컨트롤러
 * 
 * 멀티 클라우드(AWS Route53, Azure DNS, GCP Cloud DNS) DNS 호스팅 존의 생성, 조회, 수정, 삭제 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/dns/zones")
@RequiredArgsConstructor
@Tag(name = "DNS Management", description = "DNS 호스팅 존 관리 API (멀티 클라우드 지원)")
public class DnsController {

    private final DnsUseCaseService dnsUseCaseService;

    // ==================== DNS 호스팅 존 생성 ====================

    /**
     * DNS 호스팅 존을 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @param request DNS 호스팅 존 생성 요청
     * @return 생성된 DNS 호스팅 존 리소스
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DNS_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "CREATE_DNS_ZONE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "DNS 호스팅 존 생성",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(
        summary = "DNS 호스팅 존 생성",
        description = "새로운 DNS 호스팅 존을 생성합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "DNS 호스팅 존 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<DnsResponse>> createHostedZone(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "DNS 호스팅 존 생성 요청", required = true)
            @Valid @RequestBody DnsCreateRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[DnsController] createHostedZone - provider={}, accountScope={}, zoneName={}", 
                provider, accountScope, request.getZoneName());
        
        CloudResource resource = dnsUseCaseService.createHostedZone(request);
        DnsResponse response = DnsResponse.from(resource);
        
        log.info("[DnsController] createHostedZone - success resourceId={}", resource.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "DNS 호스팅 존 생성에 성공했습니다."));
    }

    // ==================== DNS 호스팅 존 조회 ====================

    /**
     * DNS 호스팅 존 목록을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 조회 요청 파라미터
     * @return DNS 호스팅 존 리소스 목록
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DNS_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "LIST_DNS_ZONES",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "DNS 호스팅 존 목록 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "DNS 호스팅 존 목록 조회",
        description = "지정된 클라우드 프로바이더의 DNS 호스팅 존 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DNS 호스팅 존 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Page<DnsResponse>>> listHostedZones(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Valid DnsQueryRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[DnsController] listHostedZones - provider={}, accountScope={}", 
                provider, accountScope);
        
        Page<CloudResource> resources = dnsUseCaseService.listHostedZones(request);
        Page<DnsResponse> responses = resources.map(DnsResponse::from);
        
        log.info("[DnsController] listHostedZones - success count={}", responses.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(responses, "DNS 호스팅 존 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 DNS 호스팅 존을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param zoneId 호스팅 존 ID
     * @param region 리전
     * @return DNS 호스팅 존 리소스
     */
    @GetMapping("/{zoneId}")
    @PreAuthorize("hasAuthority('DNS_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "GET_DNS_ZONE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "DNS 호스팅 존 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "DNS 호스팅 존 상세 조회",
        description = "특정 DNS 호스팅 존의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DNS 호스팅 존 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DNS 호스팅 존을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<DnsResponse>> getHostedZone(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "호스팅 존 ID", required = true, example = "/hostedzone/Z1234567890")
            @PathVariable String zoneId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam(required = false) String region) {
        
        log.info("[DnsController] getHostedZone - provider={}, accountScope={}, zoneId={}, region={}", 
                provider, accountScope, zoneId, region);
        
        Optional<CloudResource> resource = dnsUseCaseService.getHostedZone(provider, accountScope, region, zoneId);

        if (resource.isPresent()) {
            DnsResponse response = DnsResponse.from(resource.get());
            log.info("[DnsController] getHostedZone - success zoneId={}", zoneId);
            return ResponseEntity.ok(ApiResponse.success(response, "DNS 호스팅 존 조회에 성공했습니다."));
        } else {
            log.warn("[DnsController] getHostedZone - not found zoneId={}", zoneId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== DNS 호스팅 존 수정 ====================

    /**
     * DNS 호스팅 존을 수정합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param zoneId 호스팅 존 ID
     * @param region 리전
     * @param request DNS 호스팅 존 수정 요청
     * @return 수정된 DNS 호스팅 존 리소스
     */
    @PutMapping("/{zoneId}")
    @PreAuthorize("hasAuthority('DNS_UPDATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "UPDATE_DNS_ZONE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "DNS 호스팅 존 수정",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(
        summary = "DNS 호스팅 존 수정",
        description = "DNS 호스팅 존의 정보를 수정합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DNS 호스팅 존 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DNS 호스팅 존을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<DnsResponse>> updateHostedZone(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "호스팅 존 ID", required = true, example = "/hostedzone/Z1234567890")
            @PathVariable String zoneId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam(required = false) String region,

            @Parameter(description = "DNS 호스팅 존 수정 요청", required = true)
            @Valid @RequestBody DnsUpdateRequest request) {
        
        log.info("[DnsController] updateHostedZone - provider={}, accountScope={}, zoneId={}, region={}", 
                provider, accountScope, zoneId, region);
        
        CloudResource resource = dnsUseCaseService.updateHostedZone(provider, accountScope, region, zoneId, request);
        DnsResponse response = DnsResponse.from(resource);
        
        log.info("[DnsController] updateHostedZone - success zoneId={}", zoneId);
        return ResponseEntity.ok(ApiResponse.success(response, "DNS 호스팅 존 수정에 성공했습니다."));
    }

    // ==================== DNS 호스팅 존 삭제 ====================

    /**
     * DNS 호스팅 존을 삭제합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param zoneId 호스팅 존 ID
     * @param region 리전
     * @param request 삭제 요청 (선택적)
     * @return 삭제 결과
     */
    @DeleteMapping("/{zoneId}")
    @PreAuthorize("hasAuthority('DNS_DELETE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "DELETE_DNS_ZONE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "DNS 호스팅 존 삭제",
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "DNS 호스팅 존 삭제",
        description = "DNS 호스팅 존을 삭제합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "DNS 호스팅 존 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DNS 호스팅 존을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteHostedZone(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "호스팅 존 ID", required = true, example = "/hostedzone/Z1234567890")
            @PathVariable String zoneId,

            @Parameter(description = "리전", example = "us-east-1")
            @RequestParam(required = false) String region,

            @Parameter(description = "삭제 요청 (선택적)")
            @RequestBody(required = false) DnsDeleteRequest request) {
        
        log.info("[DnsController] deleteHostedZone - provider={}, accountScope={}, zoneId={}, region={}", 
                provider, accountScope, zoneId, region);
        
        // Request가 없으면 기본값으로 생성
        if (request == null) {
            request = DnsDeleteRequest.builder()
                    .providerType(provider)
                    .accountScope(accountScope)
                    .region(region)
                    .zoneId(zoneId)
                    .forceDelete(false)
                    .build();
        } else {
            // PathVariable 값 주입
            request.setProviderType(provider);
            request.setAccountScope(accountScope);
            request.setZoneId(zoneId);
            if (region != null) {
                request.setRegion(region);
            }
        }

        dnsUseCaseService.deleteHostedZone(request);
        
        log.info("[DnsController] deleteHostedZone - success zoneId={}", zoneId);
        return ResponseEntity.noContent().build();
    }
}
