package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.dto.targeting.*;
import com.agenticcp.core.domain.platform.service.FeatureFlagService;
import com.agenticcp.core.domain.platform.service.TargetingConditionEvaluator;
import com.agenticcp.core.domain.platform.service.TargetingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기능 플래그 타겟팅 관리 컨트롤러
 * 
 * 기능 플래그의 고급 타겟팅 규칙을 관리하는 REST API를 제공합니다.
 * 클라우드 프로바이더, 리전, 테넌트 타입, 사용자 역할 등 다양한 조건으로
 * 기능 플래그를 타겟팅할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/platform/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flag Targeting", description = "기능 플래그 타겟팅 관리 API")
public class FeatureFlagTargetingController {

    private final TargetingRuleService targetingRuleService;
    private final FeatureFlagService featureFlagService;
    private final TargetingConditionEvaluator targetingConditionEvaluator;

    /**
     * 기능 플래그의 타겟팅 규칙 목록 조회
     */
    @GetMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 목록 조회", 
               description = "특정 기능 플래그의 모든 타겟팅 규칙을 조회합니다.")
    public ResponseEntity<ApiResponse<TargetRuleListResponse>> getTargetingRules(
            @Parameter(description = "기능 플래그 키", example = "new-feature")
            @PathVariable String flagKey) {
        
        TargetRuleListResponse response = targetingRuleService.getTargetingRules(flagKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 타겟팅 규칙 조회
     */
    @GetMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 조회", 
               description = "특정 타겟팅 규칙의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<TargetRuleResponse>> getTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1")
            @PathVariable Long ruleId) {
        
        TargetRuleResponse response = targetingRuleService.getTargetingRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 타겟팅 규칙 생성
     */
    @PostMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 생성", 
               description = "새로운 타겟팅 규칙을 생성합니다.")
    public ResponseEntity<ApiResponse<TargetRuleResponse>> createTargetingRule(
            @Parameter(description = "기능 플래그 키", example = "new-feature")
            @PathVariable String flagKey,
            @Valid @RequestBody CreateTargetRuleRequest request) {
        
        TargetRuleResponse response = targetingRuleService.createTargetingRule(flagKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "타겟팅 규칙이 성공적으로 생성되었습니다."));
    }

    /**
     * 타겟팅 규칙 수정
     */
    @PutMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 수정", 
               description = "기존 타겟팅 규칙을 수정합니다.")
    public ResponseEntity<ApiResponse<TargetRuleResponse>> updateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1")
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateTargetRuleRequest request) {
        
        TargetRuleResponse response = targetingRuleService.updateTargetingRule(ruleId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 성공적으로 수정되었습니다."));
    }

    /**
     * 타겟팅 규칙 삭제
     */
    @DeleteMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 삭제", 
               description = "타겟팅 규칙을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1")
            @PathVariable Long ruleId) {
        
        targetingRuleService.deleteTargetingRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "타겟팅 규칙이 성공적으로 삭제되었습니다."));
    }

    /**
     * 타겟팅 규칙 활성화
     */
    @PatchMapping("/targeting-rules/{ruleId}/activate")
    @Operation(summary = "타겟팅 규칙 활성화", 
               description = "비활성화된 타겟팅 규칙을 활성화합니다.")
    public ResponseEntity<ApiResponse<TargetRuleResponse>> activateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1")
            @PathVariable Long ruleId) {
        
        TargetRuleResponse response = targetingRuleService.activateTargetingRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 활성화되었습니다."));
    }

    /**
     * 타겟팅 규칙 비활성화
     */
    @PatchMapping("/targeting-rules/{ruleId}/deactivate")
    @Operation(summary = "타겟팅 규칙 비활성화", 
               description = "활성화된 타겟팅 규칙을 비활성화합니다.")
    public ResponseEntity<ApiResponse<TargetRuleResponse>> deactivateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1")
            @PathVariable Long ruleId) {
        
        TargetRuleResponse response = targetingRuleService.deactivateTargetingRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 비활성화되었습니다."));
    }

    /**
     * 타겟팅 규칙 평가
     */
    @PostMapping("/{flagKey}/evaluate")
    @Operation(summary = "타겟팅 규칙 평가", 
               description = "특정 사용자/테넌트에 대해 타겟팅 규칙을 평가하여 기능 플래그 활성화 여부를 결정합니다.")
    public ResponseEntity<ApiResponse<TargetRuleEvaluationResponse>> evaluateTargeting(
            @Parameter(description = "기능 플래그 키", example = "new-feature")
            @PathVariable String flagKey,
            @Valid @RequestBody TargetRuleEvaluationRequest request) {
        
        var featureFlag = featureFlagService.getFlagByKeyOrThrow(flagKey);
        TargetRuleEvaluationResponse response = targetingConditionEvaluator.evaluateTargeting(featureFlag, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
