package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.service.vpc.VpcUseCaseService;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.service.vpc.VpcConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * VPC 관리 컨트롤러
 * 
 * VPC 관련 REST API 엔드포인트를 제공하는 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vpcs")
@RequiredArgsConstructor
public class VpcController {

    private final VpcUseCaseService vpcUseCaseService;

    /**
     * VPC 생성
     * 
     * @param request VPC 생성 요청
     * @return 생성된 VPC 리소스
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VPC_CREATE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "CREATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 생성",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    public ResponseEntity<CloudResource> createVpc(@RequestBody VpcCreateRequest request) {
        log.info("[VpcController] createVpc - provider={}, vpcName={}", 
                request.getProviderType(), request.getVpcName());
        CloudResource vpc = vpcUseCaseService.createVpc(request);
        log.info("[VpcController] createVpc - success resourceId={}", vpc.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vpc);
    }

    /**
     * VPC 조회 (단일)
     * 
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param region 리전
     * @param resourceId 리소스 ID
     * @return VPC 리소스
     */
    @GetMapping("/{providerType}/{accountScope}/{region}/{resourceId}")
    @PreAuthorize("hasAuthority('VPC_READ') or hasRole('ADMIN')")
    @AuditRequired(
        action = "GET_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 조회",
        includeRequestData = false,
        includeResponseData = true,
        severity = AuditSeverity.LOW
    )
    public ResponseEntity<CloudResource> getVpc(
            @PathVariable CloudProvider.ProviderType providerType,
            @PathVariable String accountScope,
            @PathVariable String region,
            @PathVariable String resourceId) {
        
        log.info("[VpcController] getVpc - provider={}, resourceId={}", providerType, resourceId);
        ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(resourceId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        Optional<CloudResource> vpc = vpcUseCaseService.getVpc(vpcId);

        if (vpc.isPresent()) {
            log.info("[VpcController] getVpc - success resourceId={}", resourceId);
            return ResponseEntity.ok(vpc.get());
        } else {
            log.warn("[VpcController] getVpc - not found resourceId={}", resourceId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * VPC 목록 조회
     * 
     * @param query VPC 조회 쿼리
     * @return VPC 리소스 목록
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VPC_READ') or hasRole('ADMIN')")
    @AuditRequired(
        action = "LIST_VPCS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 목록 조회",
        includeRequestData = false,
        includeResponseData = false,
        severity = AuditSeverity.LOW
    )
    public ResponseEntity<List<CloudResource>> listVpcs(VpcQueryRequest query) {
        log.info("[VpcController] listVpcs - provider={}", query.getProviderType());
        List<CloudResource> vpcs = vpcUseCaseService.listVpcs(query);
        log.info("[VpcController] listVpcs - success count={}", vpcs.size());
        return ResponseEntity.ok(vpcs);
    }

    /**
     * VPC 수정
     * 
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param region 리전
     * @param resourceId 리소스 ID
     * @param request VPC 수정 요청
     * @return 수정된 VPC 리소스
     */
    @PutMapping("/{providerType}/{accountScope}/{region}/{resourceId}")
    @PreAuthorize("hasAuthority('VPC_UPDATE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "UPDATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 수정",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    public ResponseEntity<CloudResource> updateVpc(
            @PathVariable CloudProvider.ProviderType providerType,
            @PathVariable String accountScope,
            @PathVariable String region,
            @PathVariable String resourceId,
            @RequestBody VpcUpdateRequest request) {
        
        log.info("[VpcController] updateVpc - provider={}, resourceId={}", providerType, resourceId);
        ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(resourceId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        CloudResource vpc = vpcUseCaseService.updateVpc(vpcId, request);
        log.info("[VpcController] updateVpc - success resourceId={}", resourceId);
        return ResponseEntity.ok(vpc);
    }

    /**
     * VPC 삭제
     * 
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param region 리전
     * @param resourceId 리소스 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{providerType}/{accountScope}/{region}/{resourceId}")
    @PreAuthorize("hasAuthority('VPC_DELETE') or hasRole('ADMIN')")
    @AuditRequired(
        action = "DELETE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 삭제",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.HIGH
    )
    public ResponseEntity<Void> deleteVpc(
            @PathVariable CloudProvider.ProviderType providerType,
            @PathVariable String accountScope,
            @PathVariable String region,
            @PathVariable String resourceId) {
        
        log.info("[VpcController] deleteVpc - provider={}, resourceId={}", providerType, resourceId);
        ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .region(region)
                .providerResourceId(resourceId)
                .serviceKey(VpcConstants.SERVICE_KEY)
                .resourceType(VpcConstants.RESOURCE_TYPE)
                .build();

        vpcUseCaseService.deleteVpc(vpcId);
        log.info("[VpcController] deleteVpc - success resourceId={}", resourceId);
        return ResponseEntity.noContent().build();
    }
}
