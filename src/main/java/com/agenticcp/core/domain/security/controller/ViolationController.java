package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.security.dto.PolicyViolationDTO;
import com.agenticcp.core.domain.security.dto.ViolationStatistics;
import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.mapper.PolicyViolationMapper;
import com.agenticcp.core.domain.security.service.PolicyViolationResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정책 위반 관리 Controller
 * 
 * <p>정책 위반 내역 조회, 통계, 수동 대응 등의 API를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/security/violations")
@RequiredArgsConstructor
@Tag(name = "정책 위반 관리", description = "보안 정책 위반 내역 조회 및 대응 API")
public class ViolationController {

    private final PolicyViolationResponseService violationService;

    /**
     * 정책 위반 내역 조회
     */
    @GetMapping
    @Operation(summary = "정책 위반 내역 조회", description = "테넌트별 정책 위반 내역을 페이징 조회합니다")
    public ResponseEntity<ApiResponse<Page<PolicyViolationDTO>>> getViolations(
            @Parameter(description = "테넌트 ID") @RequestParam Long tenantId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 필드") @RequestParam(defaultValue = "detectedAt") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "DESC") String direction) {

        log.info("[ViolationController] getViolations - tenantId={}, page={}, size={}", tenantId, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PolicyViolation> violations = violationService.getViolations(tenantId, pageable);
        Page<PolicyViolationDTO> violationDTOs = violations.map(PolicyViolationMapper::toDTO);

        return ResponseEntity.ok(ApiResponse.success(violationDTOs, 
                String.format("위반 내역 조회 완료 (총 %d건)", violations.getTotalElements())));
    }

    /**
     * 특정 위반 내역 상세 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "위반 내역 상세 조회", description = "특정 위반 내역의 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<PolicyViolationDTO>> getViolation(
            @Parameter(description = "위반 ID") @PathVariable Long id) {

        log.info("[ViolationController] getViolation - id={}", id);

        PolicyViolation violation = violationService.getViolation(id)
                .orElseThrow(() -> new IllegalArgumentException("위반 내역을 찾을 수 없습니다: " + id));

        PolicyViolationDTO dto = PolicyViolationMapper.toDTO(violation);

        return ResponseEntity.ok(ApiResponse.success(dto, "위반 내역 조회 완료"));
    }

    /**
     * 위반 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "위반 통계 조회", description = "지정된 기간 동안의 정책 위반 통계를 조회합니다")
    public ResponseEntity<ApiResponse<ViolationStatistics>> getStatistics(
            @Parameter(description = "테넌트 ID") @RequestParam Long tenantId,
            @Parameter(description = "시작 시간") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "종료 시간") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("[ViolationController] getStatistics - tenantId={}, 기간: {} ~ {}", tenantId, startTime, endTime);

        ViolationStatistics statistics = violationService.getStatistics(tenantId, startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(statistics, 
                String.format("통계 조회 완료 (총 %d건의 위반)", statistics.getTotalViolations())));
    }

    /**
     * 수동 대응 액션 실행
     */
    @PostMapping("/{id}/respond")
    @Operation(summary = "수동 대응 실행", description = "위반 내역에 대한 수동 대응 액션을 실행합니다")
    public ResponseEntity<ApiResponse<PolicyViolationDTO>> respondToViolation(
            @Parameter(description = "위반 ID") @PathVariable Long id,
            @Parameter(description = "대응자") @RequestParam String respondedBy,
            @Parameter(description = "대응 노트") @RequestParam(required = false) String notes) {

        log.info("[ViolationController] respondToViolation - id={}, respondedBy={}", id, respondedBy);

        PolicyViolation violation = violationService.resolveViolation(id, respondedBy, notes);
        PolicyViolationDTO dto = PolicyViolationMapper.toDTO(violation);

        return ResponseEntity.ok(ApiResponse.success(dto, "위반에 대한 수동 대응이 완료되었습니다"));
    }

    /**
     * 오탐지 처리
     */
    @PostMapping("/{id}/false-positive")
    @Operation(summary = "오탐지 처리", description = "위반 내역을 오탐지로 처리합니다")
    public ResponseEntity<ApiResponse<PolicyViolationDTO>> markAsFalsePositive(
            @Parameter(description = "위반 ID") @PathVariable Long id,
            @Parameter(description = "처리자") @RequestParam String handledBy,
            @Parameter(description = "처리 노트") @RequestParam(required = false) String notes) {

        log.info("[ViolationController] markAsFalsePositive - id={}, handledBy={}", id, handledBy);

        PolicyViolation violation = violationService.markAsFalsePositive(id, handledBy, notes);
        PolicyViolationDTO dto = PolicyViolationMapper.toDTO(violation);

        return ResponseEntity.ok(ApiResponse.success(dto, "위반 내역이 오탐지로 처리되었습니다"));
    }

    /**
     * 위반 해결 처리
     */
    @PutMapping("/{id}/resolve")
    @Operation(summary = "위반 해결", description = "위반 내역을 해결 완료로 처리합니다")
    public ResponseEntity<ApiResponse<PolicyViolationDTO>> resolveViolation(
            @Parameter(description = "위반 ID") @PathVariable Long id,
            @Parameter(description = "해결자") @RequestParam String resolvedBy,
            @Parameter(description = "해결 노트") @RequestParam(required = false) String notes) {

        log.info("[ViolationController] resolveViolation - id={}, resolvedBy={}", id, resolvedBy);

        PolicyViolation violation = violationService.resolveViolation(id, resolvedBy, notes);
        PolicyViolationDTO dto = PolicyViolationMapper.toDTO(violation);

        return ResponseEntity.ok(ApiResponse.success(dto, "위반이 해결되었습니다"));
    }

    /**
     * 최근 위반 내역 조회 (간단 조회)
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 위반 내역 조회", description = "최근 위반 내역을 간단히 조회합니다")
    public ResponseEntity<ApiResponse<List<PolicyViolationDTO>>> getRecentViolations(
            @Parameter(description = "테넌트 ID") @RequestParam Long tenantId,
            @Parameter(description = "조회 건수") @RequestParam(defaultValue = "10") int limit) {

        log.info("[ViolationController] getRecentViolations - tenantId={}, limit={}", tenantId, limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<PolicyViolation> violations = violationService.getViolations(tenantId, pageable);
        
        List<PolicyViolationDTO> dtos = violations.getContent().stream()
                .map(PolicyViolationMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, 
                String.format("최근 위반 %d건 조회 완료", dtos.size())));
    }
}

