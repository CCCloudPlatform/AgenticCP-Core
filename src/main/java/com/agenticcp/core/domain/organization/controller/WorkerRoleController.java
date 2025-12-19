package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AssignRoleRequest;
import com.agenticcp.core.domain.organization.dto.WorkerRoleResponse;
import com.agenticcp.core.domain.organization.service.WorkerRoleService;
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
 * Worker 역할 관리 컨트롤러
 * 
 * <p>Worker에게 역할을 부여하고 관리하는 API입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workers/{workerId}/roles")
@RequiredArgsConstructor
@Tag(name = "Worker Role Management", description = "Worker 역할 관리 API")
public class WorkerRoleController {
    
    private final WorkerRoleService workerRoleService;
    
    /**
     * Worker의 역할 목록 조회
     * 
     * @param workerId Worker ID
     * @return WorkerRole 목록
     */
    @GetMapping
    @Operation(
        summary = "Worker의 역할 목록 조회",
        description = "특정 Worker의 역할 목록을 조회합니다. 설계 C 기준: Role이 이미 tenant_id를 가지므로 WorkerRole에는 tenant_id가 없습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = WorkerRoleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Worker를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<List<WorkerRoleResponse>>> getRoles(
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId) {
        log.info("[WorkerRoleController] getRoles - workerId={}", workerId);
        
        List<WorkerRoleResponse> responses = workerRoleService.findByWorkerId(workerId)
                .stream()
                .map(WorkerRoleResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "역할 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * Worker에게 역할 부여
     * 
     * @param workerId Worker ID
     * @param request 역할 부여 요청 정보
     * @return 부여된 WorkerRole 정보
     */
    @PutMapping
    @Operation(
        summary = "Worker에게 역할 부여",
        description = "Worker에게 역할을 부여합니다. 설계 C 기준: Role이 이미 tenant_id를 가지므로 별도로 tenant_id를 지정할 필요가 없습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 부여 성공",
                     content = @Content(schema = @Schema(implementation = WorkerRoleResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Worker 또는 Role을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 부여된 역할")
    })
    public ResponseEntity<ApiResponse<WorkerRoleResponse>> assignRole(
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "역할 부여 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = AssignRoleRequest.class))
            )
            @Valid @RequestBody AssignRoleRequest request) {
        log.info("[WorkerRoleController] assignRole - workerId={}, roleId={}", 
                workerId, request.getRoleId());
        
        WorkerRoleResponse response = WorkerRoleResponse.from(
                workerRoleService.assignRole(workerId, request.getRoleId()));
        
        return ResponseEntity.ok(ApiResponse.success(response, "역할이 성공적으로 부여되었습니다."));
    }
    
    /**
     * Worker에서 역할 제거
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     */
    @DeleteMapping
    @Operation(
        summary = "Worker에서 역할 제거",
        description = "Worker에서 역할을 제거합니다. 설계 C 기준: Role이 이미 tenant_id를 가지므로 별도로 tenant_id를 지정할 필요가 없습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "역할 제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "WorkerRole을 찾을 수 없음")
    })
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId,
            @Parameter(description = "Role ID", required = true, example = "1")
            @RequestParam @Positive Long roleId) {
        log.info("[WorkerRoleController] removeRole - workerId={}, roleId={}", 
                workerId, roleId);
        
        workerRoleService.removeRole(workerId, roleId);
        
        return ResponseEntity.noContent().build();
    }
}

