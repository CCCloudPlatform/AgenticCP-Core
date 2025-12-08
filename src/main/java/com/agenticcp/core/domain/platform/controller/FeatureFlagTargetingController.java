package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.platform.dto.targeting.*;
import com.agenticcp.core.domain.platform.service.FeatureFlagService;
import com.agenticcp.core.domain.platform.service.TargetingConditionEvaluator;
import com.agenticcp.core.domain.platform.service.TargetingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기능 플래그 타겟팅 관리 컨트롤러
 * <p>
 * 기능 플래그의 고급 타겟팅 규칙을 관리하는 REST API를 제공합니다.
 * 클라우드 프로바이더, 리전, 테넌트 타입, 사용자 역할 등 다양한 조건으로
 * 기능 플래그를 타겟팅할 수 있습니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flag Targeting", description = "기능 플래그 타겟팅 관리 API v1")
public class FeatureFlagTargetingController {

    private final TargetingRuleService targetingRuleService;
    private final FeatureFlagService featureFlagService;
    private final TargetingConditionEvaluator targetingConditionEvaluator;

    /**
     * 기능 플래그의 타겟팅 규칙 목록 조회
     *
     * @param flagKey 기능 플래그 키
     * @return 타겟팅 규칙 목록 응답
     */
    @GetMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 목록 조회", 
               description = "특정 기능 플래그의 모든 타겟팅 규칙을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleListResponse>> getTargetingRules(
            @Parameter(description = "기능 플래그 키", example = "new-feature", required = true)
            @PathVariable String flagKey) {
        
        log.info("[FeatureFlagTargetingController] GET /{}/targeting-rules - Requested", flagKey);
        
        TargetRuleListResponse response = targetingRuleService.getTargetingRules(flagKey);
        
        log.info("[FeatureFlagTargetingController] GET /{}/targeting-rules - Success, ruleCount={}", 
                 flagKey, response.getRules().size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 타겟팅 규칙 조회
     *
     * @param ruleId 타겟팅 규칙 ID
     * @return 타겟팅 규칙 상세 응답
     */
    @GetMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 조회", 
               description = "특정 타겟팅 규칙의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponse>> getTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[FeatureFlagTargetingController] GET /targeting-rules/{} - Requested", ruleId);
        
        TargetRuleResponse response = targetingRuleService.getTargetingRule(ruleId);
        
        log.info("[FeatureFlagTargetingController] GET /targeting-rules/{} - Success", ruleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 타겟팅 규칙 생성
     *
     * @param flagKey 기능 플래그 키
     * @param request 타겟팅 규칙 생성 요청
     * @return 생성된 타겟팅 규칙 응답
     */
    @PostMapping("/{flagKey}/targeting-rules")
    @Operation(summary = "타겟팅 규칙 생성", 
               description = "새로운 타겟팅 규칙을 생성합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponse>> createTargetingRule(
            @Parameter(description = "기능 플래그 키", example = "new-feature", required = true)
            @PathVariable String flagKey,
            @Valid @RequestBody CreateTargetRuleRequest request) {
        
        log.info("[FeatureFlagTargetingController] POST /{}/targeting-rules - Requested, ruleName={}", 
                 flagKey, request.getRuleName());
        
        TargetRuleResponse response = targetingRuleService.createTargetingRule(flagKey, request);
        
        log.info("[FeatureFlagTargetingController] POST /{}/targeting-rules - Success, ruleId={}", 
                 flagKey, response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "타겟팅 규칙이 성공적으로 생성되었습니다."));
    }

    /**
     * 타겟팅 규칙 수정
     *
     * @param ruleId 타겟팅 규칙 ID
     * @param request 타겟팅 규칙 수정 요청
     * @return 수정된 타겟팅 규칙 응답
     */
    @PutMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 수정", 
               description = "기존 타겟팅 규칙을 수정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponse>> updateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1", required = true)
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateTargetRuleRequest request) {
        
        log.info("[FeatureFlagTargetingController] PUT /targeting-rules/{} - Requested, ruleName={}", 
                 ruleId, request.getRuleName());
        
        TargetRuleResponse response = targetingRuleService.updateTargetingRule(ruleId, request);
        
        log.info("[FeatureFlagTargetingController] PUT /targeting-rules/{} - Success", ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 성공적으로 수정되었습니다."));
    }

    /**
     * 타겟팅 규칙 삭제
     *
     * @param ruleId 타겟팅 규칙 ID
     * @return 성공 응답 (204 No Content)
     */
    @DeleteMapping("/targeting-rules/{ruleId}")
    @Operation(summary = "타겟팅 규칙 삭제", 
               description = "타겟팅 규칙을 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[FeatureFlagTargetingController] DELETE /targeting-rules/{} - Requested", ruleId);
        
        targetingRuleService.deleteTargetingRule(ruleId);
        
        log.info("[FeatureFlagTargetingController] DELETE /targeting-rules/{} - Success", ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 타겟팅 규칙 활성화
     *
     * @param ruleId 타겟팅 규칙 ID
     * @return 활성화된 타겟팅 규칙 응답
     */
    @PatchMapping("/targeting-rules/{ruleId}/activate")
    @Operation(summary = "타겟팅 규칙 활성화", 
               description = "비활성화된 타겟팅 규칙을 활성화합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponse>> activateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[FeatureFlagTargetingController] PATCH /targeting-rules/{}/activate - Requested", ruleId);
        
        TargetRuleResponse response = targetingRuleService.activateTargetingRule(ruleId);
        
        log.info("[FeatureFlagTargetingController] PATCH /targeting-rules/{}/activate - Success", ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 활성화되었습니다."));
    }

    /**
     * 타겟팅 규칙 비활성화
     *
     * @param ruleId 타겟팅 규칙 ID
     * @return 비활성화된 타겟팅 규칙 응답
     */
    @PatchMapping("/targeting-rules/{ruleId}/deactivate")
    @Operation(summary = "타겟팅 규칙 비활성화", 
               description = "활성화된 타겟팅 규칙을 비활성화합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "타겟팅 규칙을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleResponse>> deactivateTargetingRule(
            @Parameter(description = "타겟팅 규칙 ID", example = "1", required = true)
            @PathVariable Long ruleId) {
        
        log.info("[FeatureFlagTargetingController] PATCH /targeting-rules/{}/deactivate - Requested", ruleId);
        
        TargetRuleResponse response = targetingRuleService.deactivateTargetingRule(ruleId);
        
        log.info("[FeatureFlagTargetingController] PATCH /targeting-rules/{}/deactivate - Success", ruleId);
        return ResponseEntity.ok(ApiResponse.success(response, "타겟팅 규칙이 비활성화되었습니다."));
    }

    /**
     * 타겟팅 규칙 평가
     *
     * @param flagKey 기능 플래그 키
     * @param request 타겟팅 규칙 평가 요청
     * @return 타겟팅 규칙 평가 응답
     */
    @PostMapping("/{flagKey}/evaluate")
    @Operation(summary = "타겟팅 규칙 평가", 
               description = "특정 사용자/테넌트에 대해 타겟팅 규칙을 평가하여 기능 플래그 활성화 여부를 결정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "평가 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기능 플래그를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<TargetRuleEvaluationResponse>> evaluateTargeting(
            @Parameter(description = "기능 플래그 키", example = "new-feature", required = true)
            @PathVariable String flagKey,
            @Valid @RequestBody TargetRuleEvaluationRequest request) {
        
        log.info("[FeatureFlagTargetingController] POST /{}/evaluate - Requested, tenantId={}, userId={}", 
                 flagKey, request.getTenantId(), request.getUserId());
        
        var featureFlag = featureFlagService.getFlagByKeyOrThrow(flagKey);
        TargetRuleEvaluationResponse response = targetingConditionEvaluator.evaluateTargeting(featureFlag, request);
        
        log.info("[FeatureFlagTargetingController] POST /{}/evaluate - Success, result={}", 
                 flagKey, response.getResult());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
