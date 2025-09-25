package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.dto.CreateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.dto.TargetRuleResponseDto;
import com.agenticcp.core.domain.platform.dto.UpdateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.service.TargetingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 타겟팅 규칙 관리 컨트롤러
 * 
 * 고급 타겟팅 시스템의 타겟팅 규칙 CRUD API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Targeting Rules", description = "타겟팅 규칙 관리 API")
public class TargetingRuleController {

    private final TargetingRuleService targetingRuleService;

    /**
     * 특정 기능 플래그의 모든 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return 타겟팅 규칙 목록
     */
    @GetMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 조회", description = "특정 기능 플래그의 모든 타겟팅 규칙을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<TargetRuleResponseDto>>> getTargetRules(
            @Parameter(description = "기능 플래그 키", required = true)
            @PathVariable String flagKey) {
        
        log.info("[TargetingRuleController] getTargetRules - flagKey={}", flagKey);
        
        List<TargetRuleResponseDto> targetRules = targetingRuleService.getTargetRulesByFlagKey(flagKey);
        
        ApiResponse<List<TargetRuleResponseDto>> response = ApiResponse.<List<TargetRuleResponseDto>>builder()
                .success(true)
                .data(targetRules)
                .message("타겟팅 규칙 조회가 완료되었습니다")
                .build();
        
        log.info("[TargetingRuleController] getTargetRules - success count={} flagKey={}", targetRules.size(), flagKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 기능 플래그의 활성화된 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return 활성화된 타겟팅 규칙 목록
     */
    @GetMapping("/{flagKey}/targeting-rules/active")
    @Operation(summary = "활성화된 타겟팅 규칙 조회", description = "특정 기능 플래그의 활성화된 타겟팅 규칙을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<TargetRuleResponseDto>>> getActiveTargetRules(
            @Parameter(description = "기능 플래그 키", required = true)
            @PathVariable String flagKey) {
        
        log.info("[TargetingRuleController] getActiveTargetRules - flagKey={}", flagKey);
        
        List<TargetRuleResponseDto> targetRules = targetingRuleService.getActiveTargetRulesByFlagKey(flagKey);
        
        ApiResponse<List<TargetRuleResponseDto>> response = ApiResponse.<List<TargetRuleResponseDto>>builder()
                .success(true)
                .data(targetRules)
                .message("활성화된 타겟팅 규칙 조회가 완료되었습니다")
                .build();
        
        log.info("[TargetingRuleController] getActiveTargetRules - success count={} flagKey={}", targetRules.size(), flagKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 페이징을 통한 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @param pageable 페이징 정보
     * @return 페이징된 타겟팅 규칙 목록
     */
    @GetMapping("/{flagKey}/targeting-rules/paged")
    @Operation(summary = "페이징 타겟팅 규칙 조회", description = "페이징을 통한 타겟팅 규칙 조회")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Page<TargetRuleResponseDto>>> getTargetRulesWithPagination(
            @Parameter(description = "기능 플래그 키", required = true)
            @PathVariable String flagKey,
            @Parameter(description = "페이징 정보")
            Pageable pageable) {
        
        log.info("[TargetingRuleController] getTargetRulesWithPagination - flagKey={} page={} size={}", 
                flagKey, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TargetRuleResponseDto> targetRulePage = targetingRuleService.getTargetRulesByFlagKeyWithPagination(flagKey, pageable);
        
        ApiResponse<Page<TargetRuleResponseDto>> response = ApiResponse.<Page<TargetRuleResponseDto>>builder()
                .success(true)
                .data(targetRulePage)
                .message("페이징 타겟팅 규칙 조회가 완료되었습니다")
                .build();
        
        log.info("[TargetingRuleController] getTargetRulesWithPagination - success totalElements={} flagKey={}", 
                targetRulePage.getTotalElements(), flagKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * ID로 타겟팅 규칙 조회
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @return 타겟팅 규칙
     */
    @GetMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 상세 조회", description = "ID로 타겟팅 규칙을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponseDto>> getTargetRule(
            @Parameter(description = "타겟팅 규칙 ID", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[TargetingRuleController] getTargetRule - ruleId={}", ruleId);
        
        Optional<TargetRuleResponseDto> targetRule = targetingRuleService.getTargetRuleById(ruleId);
        
        if (targetRule.isPresent()) {
            ApiResponse<TargetRuleResponseDto> response = ApiResponse.<TargetRuleResponseDto>builder()
                    .success(true)
                    .data(targetRule.get())
                    .message("타겟팅 규칙 조회가 완료되었습니다")
                    .build();
            
            log.info("[TargetingRuleController] getTargetRule - success ruleId={}", ruleId);
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<TargetRuleResponseDto> response = ApiResponse.<TargetRuleResponseDto>builder()
                    .success(false)
                    .message("타겟팅 규칙을 찾을 수 없습니다")
                    .build();
            
            log.warn("[TargetingRuleController] getTargetRule - not found ruleId={}", ruleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * 타겟팅 규칙 생성
     * 
     * @param flagKey 기능 플래그 키
     * @param request 타겟팅 규칙 생성 요청
     * @return 생성된 타겟팅 규칙
     */
    @PostMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 생성", description = "새로운 타겟팅 규칙을 생성합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponseDto>> createTargetRule(
            @Parameter(description = "기능 플래그 키", required = true)
            @PathVariable String flagKey,
            @Parameter(description = "타겟팅 규칙 생성 요청", required = true)
            @Valid @RequestBody CreateTargetRuleRequestDto request) {
        
        log.info("[TargetingRuleController] createTargetRule - flagKey={} ruleName={}", flagKey, request.getRuleName());
        
        // 요청에 flagKey 설정
        request.setFlagKey(flagKey);
        
        TargetRuleResponseDto createdRule = targetingRuleService.createTargetRule(request);
        
        ApiResponse<TargetRuleResponseDto> response = ApiResponse.<TargetRuleResponseDto>builder()
                .success(true)
                .data(createdRule)
                .message("타겟팅 규칙이 성공적으로 생성되었습니다")
                .build();
        
        log.info("[TargetingRuleController] createTargetRule - success ruleId={} flagKey={}", 
                createdRule.getId(), flagKey);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 타겟팅 규칙 수정
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @param request 타겟팅 규칙 수정 요청
     * @return 수정된 타겟팅 규칙
     */
    @PutMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 수정", description = "기존 타겟팅 규칙을 수정합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponseDto>> updateTargetRule(
            @Parameter(description = "타겟팅 규칙 ID", required = true)
            @PathVariable Long ruleId,
            @Parameter(description = "타겟팅 규칙 수정 요청", required = true)
            @Valid @RequestBody UpdateTargetRuleRequestDto request) {
        
        log.info("[TargetingRuleController] updateTargetRule - ruleId={}", ruleId);
        
        TargetRuleResponseDto updatedRule = targetingRuleService.updateTargetRule(ruleId, request);
        
        ApiResponse<TargetRuleResponseDto> response = ApiResponse.<TargetRuleResponseDto>builder()
                .success(true)
                .data(updatedRule)
                .message("타겟팅 규칙이 성공적으로 수정되었습니다")
                .build();
        
        log.info("[TargetingRuleController] updateTargetRule - success ruleId={}", ruleId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 타겟팅 규칙 삭제
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 삭제", description = "타겟팅 규칙을 삭제합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteTargetRule(
            @Parameter(description = "타겟팅 규칙 ID", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[TargetingRuleController] deleteTargetRule - ruleId={}", ruleId);
        
        targetingRuleService.deleteTargetRule(ruleId);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("타겟팅 규칙이 성공적으로 삭제되었습니다")
                .build();
        
        log.info("[TargetingRuleController] deleteTargetRule - success ruleId={}", ruleId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 타겟팅 규칙 활성화/비활성화 토글
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @param enabled 활성화 여부
     * @return 수정된 타겟팅 규칙
     */
    @PatchMapping("/targeting-rules/{ruleId}/toggle")
    @Operation(summary = "타겟팅 규칙 토글", description = "타겟팅 규칙의 활성화/비활성화 상태를 토글합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponseDto>> toggleTargetRule(
            @Parameter(description = "타겟팅 규칙 ID", required = true)
            @PathVariable Long ruleId,
            @Parameter(description = "활성화 여부", required = true)
            @RequestParam boolean enabled) {
        
        log.info("[TargetingRuleController] toggleTargetRule - ruleId={} enabled={}", ruleId, enabled);
        
        TargetRuleResponseDto toggledRule = targetingRuleService.toggleTargetRule(ruleId, enabled);
        
        ApiResponse<TargetRuleResponseDto> response = ApiResponse.<TargetRuleResponseDto>builder()
                .success(true)
                .data(toggledRule)
                .message("타겟팅 규칙 상태가 성공적으로 변경되었습니다")
                .build();
        
        log.info("[TargetingRuleController] toggleTargetRule - success ruleId={} enabled={}", ruleId, enabled);
        
        return ResponseEntity.ok(response);
    }
}
