package com.agenticcp.core.controller;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.service.aws.VpcUseCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        
        try {
            CloudResource vpc = vpcUseCaseService.createVpc(request);
            
            log.info("[VpcController] createVpc - success resourceId={}", vpc.getResourceId());
            return ResponseEntity.status(HttpStatus.CREATED).body(vpc);
            
        } catch (Exception e) {
            log.error("[VpcController] createVpc - failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
