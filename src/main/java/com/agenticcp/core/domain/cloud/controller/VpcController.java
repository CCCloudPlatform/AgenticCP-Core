package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.dto.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.service.vpc.VpcUseCaseService;
import com.agenticcp.core.domain.cloud.dto.VpcQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.service.vpc.VpcConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * VPC 관리 REST API 컨트롤러
 * 
 * 멀티 클라우드(AWS, GCP, Azure) VPC의 생성, 조회, 수정, 삭제 기능을 제공합니다.
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/vpcs")
@RequiredArgsConstructor
@Tag(name = "VPC Management", description = "VPC 관리 API (멀티 클라우드 지원)")
public class VpcController {

    private final VpcUseCaseService vpcUseCaseService;

    // ==================== VPC 생성 ====================

    /**
     * VPC를 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @param request VPC 생성 요청
     * @return 생성된 VPC 리소스
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VPC_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "CREATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 생성",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    @Operation(
        summary = "VPC 생성",
        description = "새로운 VPC를 생성합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "VPC 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> createVpc(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "VPC 생성 요청", required = true)
            @Valid @RequestBody VpcCreateRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[VpcController] createVpc - provider={}, accountScope={}, vpcName={}", 
                provider, accountScope, request.getVpcName());
        
        CloudResource vpc = vpcUseCaseService.createVpc(request);
        
        log.info("[VpcController] createVpc - success resourceId={}", vpc.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(vpc, "VPC 생성에 성공했습니다."));
    }

    // ==================== VPC 조회 ====================

    /**
     * VPC 목록을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param region 리전 (선택)
     * @param vpcName VPC 이름 필터 (선택)
     * @param cidrBlock CIDR 블록 필터 (선택)
     * @return VPC 리소스 목록
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VPC_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "LIST_VPCS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 목록 조회",
        includeRequestData = false,
        includeResponseData = false,
        severity = AuditSeverity.LOW
    )
    @Operation(
        summary = "VPC 목록 조회",
        description = "지정된 클라우드 프로바이더의 VPC 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "VPC 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CloudResource>>> listVpcs(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "리전", example = "us-east-1")
            @RequestParam(required = false) String region,

            @Parameter(description = "VPC 이름 필터", example = "my-vpc")
            @RequestParam(required = false) String vpcName,

            @Parameter(description = "CIDR 블록 필터", example = "10.0.0.0/16")
            @RequestParam(required = false) String cidrBlock) {
        
        log.info("[VpcController] listVpcs - provider={}, accountScope={}, region={}", 
                provider, accountScope, region);
        
        VpcQueryRequest query = VpcQueryRequest.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .region(region)
                .vpcName(vpcName)
                .cidrBlock(cidrBlock)
                .build();
        
        List<CloudResource> vpcs = vpcUseCaseService.listVpcs(query);
        
        log.info("[VpcController] listVpcs - success count={}", vpcs.size());
        return ResponseEntity.ok(ApiResponse.success(vpcs, "VPC 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 VPC를 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param vpcId VPC ID
     * @param region 리전
     * @return VPC 리소스
     */
    @GetMapping("/{vpcId}")
    @PreAuthorize("hasAuthority('VPC_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "GET_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 조회",
        includeRequestData = false,
        includeResponseData = true,
        severity = AuditSeverity.LOW
    )
    @Operation(
        summary = "VPC 상세 조회",
        description = "특정 VPC의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "VPC 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VPC를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> getVpc(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "VPC ID", required = true, example = "vpc-12345678")
            @PathVariable String vpcId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region) {
        
        log.info("[VpcController] getVpc - provider={}, accountScope={}, vpcId={}, region={}", 
                provider, accountScope, vpcId, region);
        
        ResourceIdentity resourceIdentity = ResourceIdentity.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(vpcId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        Optional<CloudResource> vpc = vpcUseCaseService.getVpc(resourceIdentity);

        if (vpc.isPresent()) {
            log.info("[VpcController] getVpc - success vpcId={}", vpcId);
            return ResponseEntity.ok(ApiResponse.success(vpc.get(), "VPC 조회에 성공했습니다."));
        } else {
            log.warn("[VpcController] getVpc - not found vpcId={}", vpcId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== VPC 수정 ====================

    /**
     * VPC를 수정합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param vpcId VPC ID
     * @param region 리전
     * @param request VPC 수정 요청
     * @return 수정된 VPC 리소스
     */
    @PutMapping("/{vpcId}")
    @PreAuthorize("hasAuthority('VPC_UPDATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "UPDATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 수정",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    @Operation(
        summary = "VPC 수정",
        description = "VPC의 정보를 수정합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "VPC 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VPC를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> updateVpc(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "VPC ID", required = true, example = "vpc-12345678")
            @PathVariable String vpcId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region,

            @Parameter(description = "VPC 수정 요청", required = true)
            @Valid @RequestBody VpcUpdateRequest request) {
        
        log.info("[VpcController] updateVpc - provider={}, accountScope={}, vpcId={}, region={}", 
                provider, accountScope, vpcId, region);
        
        ResourceIdentity resourceIdentity = ResourceIdentity.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(vpcId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        CloudResource vpc = vpcUseCaseService.updateVpc(resourceIdentity, request);
        
        log.info("[VpcController] updateVpc - success vpcId={}", vpcId);
        return ResponseEntity.ok(ApiResponse.success(vpc, "VPC 수정에 성공했습니다."));
    }

    // ==================== VPC 삭제 ====================

    /**
     * VPC를 삭제합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param vpcId VPC ID
     * @param region 리전
     * @return 삭제 결과
     */
    @DeleteMapping("/{vpcId}")
    @PreAuthorize("hasAuthority('VPC_DELETE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "DELETE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 삭제",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.HIGH
    )
    @Operation(
        summary = "VPC 삭제",
        description = "VPC를 삭제합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "VPC 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VPC를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteVpc(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "VPC ID", required = true, example = "vpc-12345678")
            @PathVariable String vpcId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region) {
        
        log.info("[VpcController] deleteVpc - provider={}, accountScope={}, vpcId={}, region={}", 
                provider, accountScope, vpcId, region);
        
        ResourceIdentity resourceIdentity = ResourceIdentity.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(vpcId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        vpcUseCaseService.deleteVpc(resourceIdentity);
        
        log.info("[VpcController] deleteVpc - success vpcId={}", vpcId);
        return ResponseEntity.noContent().build();
    }
}
