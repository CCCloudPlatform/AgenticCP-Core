package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.CreateWorkerRequest;
import com.agenticcp.core.domain.organization.dto.WorkerResponse;
import com.agenticcp.core.domain.organization.service.WorkerService;
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
 * Worker 관리 컨트롤러
 * 
 * <p>Worker의 생성 및 조회를 제공하는 API입니다.
 * 설계 C 기준: Worker는 User 또는 Organization 기반으로 생성되며, 테넌트 독립적입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/workers")
@RequiredArgsConstructor
@Tag(name = "Worker Management", description = "Worker 관리 API")
public class WorkerController {
    
    private final WorkerService workerService;
    
    /**
     * Worker 생성 (User 기반)
     * 
     * @param userId 사용자 ID
     * @return 생성된 Worker 정보
     */
    @PostMapping
    @Operation(
        summary = "Worker 생성 (User 기반)",
        description = "User 기반으로 Worker를 생성합니다. 설계 C 기준: Worker는 테넌트 독립적입니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Worker 생성 성공",
                     content = @Content(schema = @Schema(implementation = WorkerResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 Worker")
    })
    public ResponseEntity<ApiResponse<WorkerResponse>> createWorkerFromUser(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId) {
        log.info("[WorkerController] createWorkerFromUser - userId={}", userId);
        
        WorkerResponse response = WorkerResponse.from(
                workerService.createWorkerFromUser(userId));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Worker가 성공적으로 생성되었습니다."));
    }
    
    /**
     * 사용자의 Worker 목록 조회
     * 
     * @param userId 사용자 ID
     * @return Worker 목록
     */
    @GetMapping
    @Operation(
        summary = "사용자의 Worker 목록 조회",
        description = "특정 사용자의 Worker 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = WorkerResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getWorkersByUserId(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId) {
        log.info("[WorkerController] getWorkersByUserId - userId={}", userId);
        
        List<WorkerResponse> responses = workerService.findByUserId(userId)
                .stream()
                .map(WorkerResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "Worker 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * Worker 조회
     * 
     * @param userId 사용자 ID
     * @param workerId Worker ID
     * @return Worker 정보
     */
    @GetMapping("/{workerId}")
    @Operation(
        summary = "Worker 조회",
        description = "특정 Worker의 정보를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = WorkerResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Worker를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorker(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId,
            @Parameter(description = "Worker ID", required = true, example = "1")
            @PathVariable @Positive Long workerId) {
        log.info("[WorkerController] getWorker - userId={}, workerId={}", userId, workerId);
        
        WorkerResponse response = WorkerResponse.from(workerService.findById(workerId));
        
        return ResponseEntity.ok(ApiResponse.success(response, "Worker 정보를 성공적으로 조회했습니다."));
    }
}

