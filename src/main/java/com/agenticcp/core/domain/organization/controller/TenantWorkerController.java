package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AssignWorkerRequest;
import com.agenticcp.core.domain.organization.dto.TenantWorkerMapResponse;
import com.agenticcp.core.domain.organization.service.TenantWorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 테넌트-Worker 관리 컨트롤러
 * 
 * <p>테넌트와 Worker 간의 관계를 관리하는 API입니다.
 * Shared Tenant에 Worker를 할당하고 관리합니다.</p>
 * 
 * @deprecated 설계 C 기준: TenantWorkerMap은 제거되었으며, CloudResourceWorkerController를 사용합니다.
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/workers")
@RequiredArgsConstructor
@Tag(name = "Tenant Worker Management", description = "테넌트-Worker 관리 API")
public class TenantWorkerController {
    
    private final TenantWorkerService tenantWorkerService;
    
    /**
     * 테넌트의 Worker 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return TenantWorkerMap 목록
     */
    @GetMapping
    @Operation(
        summary = "테넌트의 Worker 목록 조회",
        description = "특정 테넌트에 할당된 Worker 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = TenantWorkerMapResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테넌트를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<List<TenantWorkerMapResponse>>> getWorkers(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @PathVariable @Positive Long tenantId) {
        log.info("[TenantWorkerController] getWorkers - tenantId={}", tenantId);
        
        List<TenantWorkerMapResponse> responses = tenantWorkerService.findByTenantId(tenantId)
                .stream()
                .map(TenantWorkerMapResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "Worker 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * 테넌트에 Worker 할당 (Shared Tenant용)
     * 
     * @param tenantId 테넌트 ID
     * @param request Worker 할당 요청 정보
     * @return 할당된 TenantWorkerMap 정보
     */
    @PostMapping
    @Operation(
        summary = "테넌트에 Worker 할당",
        description = "Shared Tenant에 Worker를 할당합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Worker 할당 성공",
                     content = @Content(schema = @Schema(implementation = TenantWorkerMapResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테넌트 또는 Worker를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 할당된 Worker")
    })
    public ResponseEntity<ApiResponse<TenantWorkerMapResponse>> assignWorker(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @PathVariable @Positive Long tenantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Worker 할당 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = AssignWorkerRequest.class))
            )
            @Valid @RequestBody AssignWorkerRequest request) {
        log.info("[TenantWorkerController] assignWorker - tenantId={}, workerId={}", 
                tenantId, request.getWorkerId());
        
        TenantWorkerMapResponse response = TenantWorkerMapResponse.from(
                tenantWorkerService.assignWorkerToTenant(tenantId, request.getWorkerId(), request.getAccessScope()));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Worker가 성공적으로 할당되었습니다."));
    }
    
    /**
     * 테넌트에서 Worker 제거
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     */
    @DeleteMapping("/{workerId}")
    @Operation(
        summary = "테넌트에서 Worker 제거",
        description = "테넌트에서 Worker를 제거합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Worker 제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테넌트 또는 Worker를 찾을 수 없음")
    })
    public ResponseEntity<Void> removeWorker(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @PathVariable @Positive Long tenantId,
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId) {
        log.info("[TenantWorkerController] removeWorker - tenantId={}, workerId={}", tenantId, workerId);
        
        tenantWorkerService.removeWorkerFromTenant(tenantId, workerId);
        
        return ResponseEntity.noContent().build();
    }
}

