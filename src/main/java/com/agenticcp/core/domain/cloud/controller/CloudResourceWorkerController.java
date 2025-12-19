package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.cloud.dto.CloudResourceWorkerMapResponse;
import com.agenticcp.core.domain.cloud.service.CloudResourceWorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 클라우드 리소스-Worker 관리 컨트롤러
 * 
 * <p>클라우드 리소스와 Worker 간의 관계를 관리하는 API입니다.
 * 설계 C 기준: 리소스 단위로 Worker 접근 권한을 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-19
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud-resources/{resourceId}/workers")
@RequiredArgsConstructor
@Tag(name = "Cloud Resource Worker Management", description = "클라우드 리소스-Worker 관리 API")
public class CloudResourceWorkerController {
    
    private final CloudResourceWorkerService cloudResourceWorkerService;
    
    /**
     * 클라우드 리소스의 Worker 목록 조회
     * 
     * @param resourceId 클라우드 리소스 ID
     * @return CloudResourceWorkerMap 목록
     */
    @GetMapping
    @Operation(
        summary = "클라우드 리소스의 Worker 목록 조회",
        description = "특정 클라우드 리소스에 할당된 Worker 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = CloudResourceWorkerMapResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "클라우드 리소스를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<List<CloudResourceWorkerMapResponse>>> getWorkers(
            @Parameter(description = "클라우드 리소스 ID", required = true, example = "1")
            @PathVariable @Positive Long resourceId) {
        log.info("[CloudResourceWorkerController] getWorkers - resourceId={}", resourceId);
        
        List<CloudResourceWorkerMapResponse> responses = cloudResourceWorkerService.findByResourceId(resourceId)
                .stream()
                .map(CloudResourceWorkerMapResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "Worker 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * 클라우드 리소스에 Worker 할당
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     * @return 할당된 CloudResourceWorkerMap 정보
     */
    @PostMapping("/{workerId}")
    @Operation(
        summary = "클라우드 리소스에 Worker 할당",
        description = "클라우드 리소스에 Worker를 할당합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Worker 할당 성공",
                     content = @Content(schema = @Schema(implementation = CloudResourceWorkerMapResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "클라우드 리소스 또는 Worker를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 할당된 Worker")
    })
    public ResponseEntity<ApiResponse<CloudResourceWorkerMapResponse>> assignWorker(
            @Parameter(description = "클라우드 리소스 ID", required = true, example = "1")
            @PathVariable @Positive Long resourceId,
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId) {
        log.info("[CloudResourceWorkerController] assignWorker - resourceId={}, workerId={}", 
                resourceId, workerId);
        
        CloudResourceWorkerMapResponse response = CloudResourceWorkerMapResponse.from(
                cloudResourceWorkerService.assignWorkerToResource(resourceId, workerId));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Worker가 성공적으로 할당되었습니다."));
    }
    
    /**
     * 클라우드 리소스에서 Worker 제거
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     */
    @DeleteMapping("/{workerId}")
    @Operation(
        summary = "클라우드 리소스에서 Worker 제거",
        description = "클라우드 리소스에서 Worker를 제거합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Worker 제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "클라우드 리소스 또는 Worker를 찾을 수 없음")
    })
    public ResponseEntity<Void> removeWorker(
            @Parameter(description = "클라우드 리소스 ID", required = true, example = "1")
            @PathVariable @Positive Long resourceId,
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId) {
        log.info("[CloudResourceWorkerController] removeWorker - resourceId={}, workerId={}", 
                resourceId, workerId);
        
        cloudResourceWorkerService.removeWorkerFromResource(resourceId, workerId);
        
        return ResponseEntity.noContent().build();
    }
}

