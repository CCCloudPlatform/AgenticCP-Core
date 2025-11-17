package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.platform.dto.ApprovalProcessRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalResponse;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.agenticcp.core.domain.platform.service.FeatureFlagApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 기능 플래그 승인 컨트롤러
 * 
 * 기능 플래그 변경에 대한 승인 워크플로우를 관리하는 API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/feature-flags/approvals")
@RequiredArgsConstructor
@Tag(name = "Feature Flag Approval", description = "기능 플래그 승인 관리 API")
public class FeatureFlagApprovalController {

    private final FeatureFlagApprovalService approvalService;

    /**
     * 승인 요청 생성
     * 
     * @param request 승인 요청 정보
     * @return 생성된 승인 요청
     */
    @PostMapping
    @Operation(
            summary = "승인 요청 생성",
            description = "기능 플래그 변경에 대한 승인을 요청합니다."
    )
    public ResponseEntity<ApiResponse<FeatureFlagApprovalResponse>> requestApproval(
            @Parameter(description = "승인 요청 정보")
            @Valid @RequestBody FeatureFlagApprovalRequest request) {
        
        log.info("[FeatureFlagApprovalController] requestApproval - flagKey={}", request.getFlagKey());

        String userId = getCurrentUserId();
        FeatureFlagApprovalResponse response = approvalService.requestApproval(request, userId);

        log.info("[FeatureFlagApprovalController] requestApproval - success approvalId={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "승인 요청이 생성되었습니다."));
    }

    /**
     * 승인 처리
     * 
     * @param approvalId 승인 요청 ID
     * @param request 승인 처리 요청
     * @return 승인 처리된 승인 요청
     */
    @PostMapping("/{approvalId}/approve")
    @Operation(
            summary = "승인 처리",
            description = "승인 요청을 승인 처리합니다."
    )
    public ResponseEntity<ApiResponse<FeatureFlagApprovalResponse>> approveRequest(
            @Parameter(description = "승인 요청 ID", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "승인 처리 요청")
            @Valid @RequestBody ApprovalProcessRequest request) {
        
        log.info("[FeatureFlagApprovalController] approveRequest - approvalId={}", approvalId);

        String userId = getCurrentUserId();
        FeatureFlagApprovalResponse response = approvalService.approveRequest(approvalId, request, userId);

        log.info("[FeatureFlagApprovalController] approveRequest - success approvalId={}", approvalId);
        return ResponseEntity.ok(ApiResponse.success(response, "승인 요청이 승인되었습니다."));
    }

    /**
     * 거부 처리
     * 
     * @param approvalId 승인 요청 ID
     * @param request 거부 처리 요청
     * @return 거부 처리된 승인 요청
     */
    @PostMapping("/{approvalId}/reject")
    @Operation(
            summary = "거부 처리",
            description = "승인 요청을 거부 처리합니다."
    )
    public ResponseEntity<ApiResponse<FeatureFlagApprovalResponse>> rejectRequest(
            @Parameter(description = "승인 요청 ID", example = "1")
            @PathVariable Long approvalId,
            @Parameter(description = "거부 처리 요청")
            @Valid @RequestBody ApprovalProcessRequest request) {
        
        log.info("[FeatureFlagApprovalController] rejectRequest - approvalId={}", approvalId);

        String userId = getCurrentUserId();
        FeatureFlagApprovalResponse response = approvalService.rejectRequest(approvalId, request, userId);

        log.info("[FeatureFlagApprovalController] rejectRequest - success approvalId={}", approvalId);
        return ResponseEntity.ok(ApiResponse.success(response, "승인 요청이 거부되었습니다."));
    }

    /**
     * 승인 요청 목록 조회 (페이징)
     * 
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param status 필터: 승인 상태 (선택)
     * @return 승인 요청 목록
     */
    @GetMapping
    @Operation(
            summary = "승인 요청 목록 조회",
            description = "모든 승인 요청을 조회합니다. 상태별 필터링이 가능합니다."
    )
    public ResponseEntity<ApiResponse<Page<FeatureFlagApprovalResponse>>> getApprovals(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "필터: 승인 상태", example = "PENDING")
            @RequestParam(required = false) ApprovalStatus status) {
        
        log.info("[FeatureFlagApprovalController] getApprovals - page={} size={} status={}", page, size, status);

        // 페이징 설정
        Pageable pageable = PageRequest.of(
                page, 
                Math.min(Math.max(size, 1), 100), // 1~100 사이로 제한
                Sort.by(Sort.Direction.DESC, "requestedAt")
        );

        Page<FeatureFlagApprovalResponse> responses;

        if (status != null) {
            // 상태별 필터링
            responses = approvalService.getApprovalsByStatus(status, pageable);
        } else {
            // 전체 조회
            responses = approvalService.getApprovals(pageable);
        }

        log.info("[FeatureFlagApprovalController] getApprovals - success total={}", responses.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(
                responses, 
                String.format("승인 요청 목록 조회가 완료되었습니다. (총 %d건)", responses.getTotalElements())
        ));
    }

    /**
     * 승인 요청 상세 조회
     * 
     * @param approvalId 승인 요청 ID
     * @return 승인 요청 상세 정보
     */
    @GetMapping("/{approvalId}")
    @Operation(
            summary = "승인 요청 상세 조회",
            description = "특정 승인 요청의 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<FeatureFlagApprovalResponse>> getApproval(
            @Parameter(description = "승인 요청 ID", example = "1")
            @PathVariable Long approvalId) {
        
        log.info("[FeatureFlagApprovalController] getApproval - approvalId={}", approvalId);

        FeatureFlagApprovalResponse response = approvalService.getApproval(approvalId);

        log.info("[FeatureFlagApprovalController] getApproval - success approvalId={} status={}", 
                approvalId, response.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "승인 요청 조회가 완료되었습니다."));
    }

    /**
     * 플래그별 승인 요청 목록 조회
     * 
     * @param flagKey 플래그 키
     * @return 승인 요청 목록
     */
    @GetMapping("/flag/{flagKey}")
    @Operation(
            summary = "플래그별 승인 요청 목록 조회",
            description = "특정 기능 플래그의 모든 승인 요청을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<FeatureFlagApprovalResponse>>> getApprovalsByFlagKey(
            @Parameter(description = "기능 플래그 키", example = "new-feature")
            @PathVariable String flagKey) {
        
        log.info("[FeatureFlagApprovalController] getApprovalsByFlagKey - flagKey={}", flagKey);

        List<FeatureFlagApprovalResponse> responses = approvalService.getApprovalsByFlagKey(flagKey);

        log.info("[FeatureFlagApprovalController] getApprovalsByFlagKey - success flagKey={} count={}", 
                flagKey, responses.size());
        return ResponseEntity.ok(ApiResponse.success(
                responses, 
                String.format("승인 요청 목록 조회가 완료되었습니다. (총 %d건)", responses.size())
        ));
    }

    /**
     * 현재 인증된 사용자 ID 추출
     * 
     * @return 사용자 ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        log.warn("[FeatureFlagApprovalController] getCurrentUserId - authentication not found, using 'system'");
        return "system";
    }
}

