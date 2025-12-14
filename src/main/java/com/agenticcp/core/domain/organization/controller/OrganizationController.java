package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationStatsResponse;
import com.agenticcp.core.domain.organization.service.OrganizationService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
// [DEPRECATED imports - 주석처리된 API에서 사용]
// import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
// import com.agenticcp.core.domain.organization.dto.UserResponse;
// import com.agenticcp.core.domain.organization.dto.OrganizationHierarchyResponse;
// import com.agenticcp.core.domain.organization.dto.OrganizationPathResponse;
// import com.agenticcp.core.domain.organization.dto.MoveOrganizationRequest;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 조직 관리 컨트롤러
 * 
 * <p>조직의 생성, 조회, 수정, 삭제 및 계층 구조 관리를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "조직 관리 API")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    /**
     * 조직 생성
     * 
     * @param request 조직 생성 요청 정보
     * @return 생성된 조직 정보
     */
    @PostMapping
    @Operation(
        summary = "조직 생성",
        description = "새로운 조직을 생성합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "조직 생성 성공",
                     content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 조직명")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "조직 생성 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateOrganizationRequest.class))
            )
            @Valid @RequestBody CreateOrganizationRequest request) {
        log.info("[OrganizationController] createOrganization - orgName={}", request.getOrgName());
        
        OrganizationResponse response = organizationService.createOrganization(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "조직이 성공적으로 생성되었습니다."));
    }
    
    /**
     * 조직 조회 (단일)
     * 
     * @param id 조직 ID
     * @return 조직 정보
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "조직 조회",
        description = "특정 조직의 정보를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganization - id={}", id);
        
        OrganizationResponse response = organizationService.getOrganization(id);
        
        return ResponseEntity.ok(ApiResponse.success(response, "조직 정보를 성공적으로 조회했습니다."));
    }
    
    /**
     * 조직 목록 조회
     * 
     * @return 조직 목록
     */
    @GetMapping
    @Operation(
        summary = "조직 목록 조회",
        description = "모든 조직의 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getOrganizations() {
        log.info("[OrganizationController] getOrganizations");
        
        List<OrganizationResponse> responses = organizationService.getOrganizations();
        
        return ResponseEntity.ok(ApiResponse.success(responses, "조직 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * 조직 수정
     * 
     * @param id 조직 ID
     * @param request 조직 수정 요청 정보
     * @return 수정된 조직 정보
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "조직 수정",
        description = "기존 조직의 정보를 수정합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 조직명")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "조직 수정 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateOrganizationRequest.class))
            )
            @Valid @RequestBody UpdateOrganizationRequest request) {
        log.info("[OrganizationController] updateOrganization - id={}, orgName={}", id, request.getOrgName());
        
        OrganizationResponse response = organizationService.updateOrganization(id, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "조직 정보를 성공적으로 수정했습니다."));
    }
    
    /**
     * 조직 삭제
     * 
     * @param id 조직 ID
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "조직 삭제",
        description = "조직을 삭제합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "하위 조직이 존재하여 삭제할 수 없음")
    })
    public ResponseEntity<Void> deleteOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] deleteOrganization - id={}", id);
        
        organizationService.deleteOrganization(id);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 조직 수 조회
     * 
     * @return 조직 수
     */
    @GetMapping("/count")
    @Operation(
        summary = "조직 수 조회",
        description = "전체 조직 수를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<ApiResponse<Long>> getOrganizationCount() {
        log.info("[OrganizationController] getOrganizationCount");
        
        long count = organizationService.getOrganizationCount();
        
        return ResponseEntity.ok(ApiResponse.success(count, "조직 수를 성공적으로 조회했습니다."));
    }
    
    // ========== [DEPRECATED] 조직 계층 구조 API ==========
    // TODO: #163 ERD에 따라 조직 계층 구조(parent_org_id)가 없음
    // - 상위/하위 조직 개념 제거
    // - 조직 트리, 경로, 이동 기능 제거
    // - 필요시 별도 설계 후 재구현

    /*
    @GetMapping("/{id}/children")
    @Operation(summary = "[DEPRECATED] 하위 조직 목록 조회")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getChildOrganizations(
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getChildOrganizations - id={}", id);
        List<OrganizationResponse> responses = organizationService.getChildOrganizations(id);
        return ResponseEntity.ok(ApiResponse.success(responses, "하위 조직 목록을 성공적으로 조회했습니다."));
    }
    
    @GetMapping("/tree")
    @Operation(summary = "[DEPRECATED] 전체 조직 트리 조회")
    public ResponseEntity<ApiResponse<List<OrganizationHierarchyResponse>>> getOrganizationTree() {
        log.info("[OrganizationController] getOrganizationTree");
        List<OrganizationHierarchyResponse> responses = organizationService.getOrganizationTree();
        return ResponseEntity.ok(ApiResponse.success(responses, "조직 트리를 성공적으로 조회했습니다."));
    }
    
    @GetMapping("/{id}/path")
    @Operation(summary = "[DEPRECATED] 조직 경로 조회")
    public ResponseEntity<ApiResponse<OrganizationPathResponse>> getOrganizationPath(
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationPath - id={}", id);
        OrganizationPathResponse response = organizationService.getOrganizationPath(id);
        return ResponseEntity.ok(ApiResponse.success(response, "조직 경로를 성공적으로 조회했습니다."));
    }
    
    @GetMapping("/{id}/ancestors")
    @Operation(summary = "[DEPRECATED] 상위 조직 목록 조회")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAncestors(
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getAncestors - id={}", id);
        List<OrganizationResponse> responses = organizationService.getAncestors(id);
        return ResponseEntity.ok(ApiResponse.success(responses, "상위 조직 목록을 성공적으로 조회했습니다."));
    }
    
    @GetMapping("/{id}/descendants")
    @Operation(summary = "[DEPRECATED] 하위 조직 목록 조회 (모든 레벨)")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getDescendants(
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getDescendants - id={}", id);
        List<OrganizationResponse> responses = organizationService.getDescendants(id);
        return ResponseEntity.ok(ApiResponse.success(responses, "하위 조직 목록을 성공적으로 조회했습니다."));
    }
    
    @PutMapping("/{id}/move")
    @Operation(summary = "[DEPRECATED] 조직 이동")
    public ResponseEntity<ApiResponse<OrganizationResponse>> moveOrganization(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MoveOrganizationRequest request) {
        log.info("[OrganizationController] moveOrganization - id={}, newParentId={}", id, request.getNewParentId());
        OrganizationResponse response = organizationService.moveOrganization(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "조직이 성공적으로 이동되었습니다."));
    }
    */
    
    /**
     * 조직 통계 조회
     * 
     * @return 조직 통계 정보
     */
    @GetMapping("/stats")
    @Operation(
        summary = "조직 통계 조회",
        description = "조직 통계를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationStatsResponse.class)))
    })
    public ResponseEntity<ApiResponse<OrganizationStatsResponse>> getOrganizationStats() {
        log.info("[OrganizationController] getOrganizationStats");
        
        OrganizationStatsResponse response = organizationService.getOrganizationStats();
        
        return ResponseEntity.ok(ApiResponse.success(response, "조직 통계를 성공적으로 조회했습니다."));
    }
    
    // ========== [DEPRECATED] 조직-사용자 관계 API ==========
    // ✅ 완료: #172에 따라 OrganizationMember API로 대체 완료
    // - OrganizationMemberController: /api/v1/organizations/{organizationId}/members
    // - UserOrganizationController: /api/v1/users/{userId}/organizations
    // - 아래 API들은 주석 처리되어 있으며, OrganizationMember API 사용 권장

    /*
    @GetMapping("/{id}/users")
    @Operation(
        summary = "[DEPRECATED] 조직별 사용자 목록 조회",
        description = "특정 조직에 속한 사용자 목록을 조회합니다. (OrganizationMember API로 대체 예정)"
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> getOrganizationUsers(
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationUsers - id={}", id);
        List<UserResponse> responses = organizationService.getOrganizationUsers(id);
        return ResponseEntity.ok(ApiResponse.success(responses, "조직 사용자 목록을 성공적으로 조회했습니다."));
    }
    
    @PostMapping("/{id}/users")
    @Operation(
        summary = "[DEPRECATED] 사용자를 조직에 추가",
        description = "특정 조직에 사용자를 추가합니다. (OrganizationMember API로 대체 예정)"
    )
    public ResponseEntity<ApiResponse<UserResponse>> addUserToOrganization(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AddUserToOrganizationRequest request) {
        log.info("[OrganizationController] addUserToOrganization - orgId={}, userId={}", id, request.getUserId());
        UserResponse response = organizationService.addUserToOrganization(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "사용자가 조직에 성공적으로 추가되었습니다."));
    }
    
    @DeleteMapping("/{id}/users/{userId}")
    @Operation(
        summary = "[DEPRECATED] 사용자를 조직에서 제거",
        description = "특정 조직에서 사용자를 제거합니다. (OrganizationMember API로 대체 예정)"
    )
    public ResponseEntity<ApiResponse<Void>> removeUserFromOrganization(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId) {
        log.info("[OrganizationController] removeUserFromOrganization - orgId={}, userId={}", id, userId);
        organizationService.removeUserFromOrganization(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "사용자가 조직에서 성공적으로 제거되었습니다."));
    }
    */

    // ========== 조직-테넌트 관계 관리 API ==========

    /**
     * 조직의 테넌트 조회 (1:1 관계)
     * 
     * @param id 조직 ID
     * @return 테넌트 정보
     */
    @GetMapping("/{id}/tenant")
    @Operation(
        summary = "조직 테넌트 조회",
        description = "특정 조직에 연결된 테넌트를 조회합니다. (1:1 관계)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Tenant>> getOrganizationTenant(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationTenant - id={}", id);

        Tenant tenant = organizationService.getOrganizationTenant(id);

        return ResponseEntity.ok(ApiResponse.success(tenant, "조직 테넌트를 성공적으로 조회했습니다."));
    }

    /**
     * 조직 테넌트 존재 여부 조회
     * 
     * @param id 조직 ID
     * @return 테넌트 존재 여부
     */
    @GetMapping("/{id}/tenant/exists")
    @Operation(
        summary = "조직 테넌트 존재 여부 조회",
        description = "특정 조직에 테넌트가 존재하는지 확인합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkOrganizationTenant(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] checkOrganizationTenant - id={}", id);

        boolean hasTenant = organizationService.hasTenant(id);
        boolean hasActiveTenant = organizationService.hasActiveTenant(id);

        Map<String, Object> response = new HashMap<>();
        response.put("hasTenant", hasTenant);
        response.put("hasActiveTenant", hasActiveTenant);

        return ResponseEntity.ok(ApiResponse.success(response, "조직 테넌트 존재 여부를 성공적으로 조회했습니다."));
    }

    /**
     * 조직 테넌트 상세 정보 조회
     * 
     * @param id 조직 ID
     * @return 테넌트 상세 정보
     */
    @GetMapping("/{id}/tenant/info")
    @Operation(
        summary = "조직 테넌트 상세 정보 조회",
        description = "특정 조직의 테넌트 상세 정보를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationTenantInfo(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationTenantInfo - id={}", id);

        Map<String, Object> info = organizationService.getOrganizationTenantInfo(id);

        return ResponseEntity.ok(ApiResponse.success(info, "조직 테넌트 정보를 성공적으로 조회했습니다."));
    }
}