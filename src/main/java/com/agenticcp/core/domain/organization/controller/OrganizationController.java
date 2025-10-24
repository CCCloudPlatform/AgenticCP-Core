package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.UserResponse;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.organization.dto.OrganizationHierarchyResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationPathResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationStatsResponse;
import com.agenticcp.core.domain.organization.dto.MoveOrganizationRequest;
import com.agenticcp.core.domain.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Slf4j
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "조직 관리 API")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    /**
     * 조직 생성
     */
    @PostMapping
    @Operation(
        summary = "조직 생성",
        description = "새로운 조직을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "조직 생성 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "409", description = "중복된 조직명")
    })
    public ResponseEntity<OrganizationResponse> createOrganization(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "조직 생성 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateOrganizationRequest.class))
            )
            @Valid @RequestBody CreateOrganizationRequest request) {
        log.info("[OrganizationController] createOrganization - orgName={}", request.getOrgName());
        
        OrganizationResponse response = organizationService.createOrganization(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 조직 조회 (단일)
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "조직 조회",
        description = "특정 조직의 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<OrganizationResponse> getOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganization - id={}", id);
        
        OrganizationResponse response = organizationService.getOrganization(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 조직 목록 조회
     */
    @GetMapping
    @Operation(
        summary = "조직 목록 조회",
        description = "모든 조직의 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class)))
    })
    public ResponseEntity<List<OrganizationResponse>> getOrganizations() {
        log.info("[OrganizationController] getOrganizations");
        
        List<OrganizationResponse> responses = organizationService.getOrganizations();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 조직 수정
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "조직 수정",
        description = "기존 조직의 정보를 수정합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "409", description = "중복된 조직명")
    })
    public ResponseEntity<OrganizationResponse> updateOrganization(
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
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 조직 삭제
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "조직 삭제",
        description = "조직을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "하위 조직이 존재하여 삭제할 수 없음")
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
     */
    @GetMapping("/count")
    @Operation(
        summary = "조직 수 조회",
        description = "전체 조직 수를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Long> getOrganizationCount() {
        log.info("[OrganizationController] getOrganizationCount");
        
        long count = organizationService.getOrganizationCount();
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * 하위 조직 목록 조회
     */
    @GetMapping("/{id}/children")
    @Operation(
        summary = "하위 조직 목록 조회",
        description = "특정 조직의 하위 조직 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<List<OrganizationResponse>> getChildOrganizations(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getChildOrganizations - id={}", id);
        
        List<OrganizationResponse> responses = organizationService.getChildOrganizations(id);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 전체 조직 트리 조회
     */
    @GetMapping("/tree")
    @Operation(
        summary = "전체 조직 트리 조회",
        description = "모든 조직을 계층 구조로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationHierarchyResponse.class)))
    })
    public ResponseEntity<List<OrganizationHierarchyResponse>> getOrganizationTree() {
        log.info("[OrganizationController] getOrganizationTree");
        
        List<OrganizationHierarchyResponse> responses = organizationService.getOrganizationTree();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 조직 경로 조회
     */
    @GetMapping("/{id}/path")
    @Operation(
        summary = "조직 경로 조회",
        description = "루트부터 현재 조직까지의 경로를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationPathResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<OrganizationPathResponse> getOrganizationPath(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationPath - id={}", id);
        
        OrganizationPathResponse response = organizationService.getOrganizationPath(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상위 조직 목록 조회
     */
    @GetMapping("/{id}/ancestors")
    @Operation(
        summary = "상위 조직 목록 조회",
        description = "특정 조직의 모든 상위 조직을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<List<OrganizationResponse>> getAncestors(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getAncestors - id={}", id);
        
        List<OrganizationResponse> responses = organizationService.getAncestors(id);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 하위 조직 목록 조회 (모든 레벨)
     */
    @GetMapping("/{id}/descendants")
    @Operation(
        summary = "하위 조직 목록 조회",
        description = "특정 조직의 모든 하위 조직을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<List<OrganizationResponse>> getDescendants(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getDescendants - id={}", id);
        
        List<OrganizationResponse> responses = organizationService.getDescendants(id);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 조직 이동
     */
    @PutMapping("/{id}/move")
    @Operation(
        summary = "조직 이동",
        description = "조직을 다른 상위 조직으로 이동합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이동 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "409", description = "순환 참조 발생")
    })
    public ResponseEntity<OrganizationResponse> moveOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "조직 이동 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = MoveOrganizationRequest.class))
            )
            @Valid @RequestBody MoveOrganizationRequest request) {
        log.info("[OrganizationController] moveOrganization - id={}, newParentId={}", id, request.getNewParentId());
        
        OrganizationResponse response = organizationService.moveOrganization(id, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 조직 통계 조회
     */
    @GetMapping("/stats")
    @Operation(
        summary = "조직 통계 조회",
        description = "조직 통계를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationStatsResponse.class)))
    })
    public ResponseEntity<OrganizationStatsResponse> getOrganizationStats() {
        log.info("[OrganizationController] getOrganizationStats");
        
        OrganizationStatsResponse response = organizationService.getOrganizationStats();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 조직별 사용자 목록 조회
     */
    @GetMapping("/{id}/users")
    @Operation(
        summary = "조직별 사용자 목록 조회",
        description = "특정 조직에 속한 사용자 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<List<UserResponse>> getOrganizationUsers(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationUsers - id={}", id);
        
        List<UserResponse> responses = organizationService.getOrganizationUsers(id);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 사용자를 조직에 추가
     */
    @PostMapping("/{id}/users")
    @Operation(
        summary = "사용자를 조직에 추가",
        description = "특정 조직에 사용자를 추가합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "404", description = "조직 또는 사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 조직에 속한 사용자")
    })
    public ResponseEntity<UserResponse> addUserToOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "사용자 추가 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = AddUserToOrganizationRequest.class))
            )
            @Valid @RequestBody AddUserToOrganizationRequest request) {
        log.info("[OrganizationController] addUserToOrganization - orgId={}, userId={}", id, request.getUserId());
        
        UserResponse response = organizationService.addUserToOrganization(id, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자를 조직에서 제거
     */
    @DeleteMapping("/{id}/users/{userId}")
    @Operation(
        summary = "사용자를 조직에서 제거",
        description = "특정 조직에서 사용자를 제거합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "제거 성공"),
        @ApiResponse(responseCode = "404", description = "조직 또는 사용자를 찾을 수 없음")
    })
    public ResponseEntity<Void> removeUserFromOrganization(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId) {
        log.info("[OrganizationController] removeUserFromOrganization - orgId={}, userId={}", id, userId);
        
        organizationService.removeUserFromOrganization(id, userId);
        
        return ResponseEntity.ok().build();
    }

    // ========== 조직-테넌트 관계 관리 API ==========

    /**
     * 조직별 테넌트 목록 조회
     */
    @GetMapping("/{id}/tenants")
    @Operation(
        summary = "조직별 테넌트 목록 조회",
        description = "특정 조직에 속한 테넌트 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<List<Tenant>> getOrganizationTenants(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationTenants - id={}", id);

        List<Tenant> tenants = organizationService.getOrganizationTenants(id);

        return ResponseEntity.ok(tenants);
    }

    /**
     * 조직별 테넌트 수 조회
     */
    @GetMapping("/{id}/tenants/count")
    @Operation(
        summary = "조직별 테넌트 수 조회",
        description = "특정 조직에 속한 테넌트 수를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Object>> getOrganizationTenantCount(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationTenantCount - id={}", id);

        long totalCount = organizationService.getOrganizationTenantCount(id);
        long activeCount = organizationService.getActiveTenantCount(id);

        Map<String, Object> response = new HashMap<>();
        response.put("totalTenants", totalCount);
        response.put("activeTenants", activeCount);
        response.put("inactiveTenants", totalCount - activeCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 조직별 테넌트 통계 조회
     */
    @GetMapping("/{id}/tenants/stats")
    @Operation(
        summary = "조직별 테넌트 통계 조회",
        description = "특정 조직의 테넌트 관련 상세 통계를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Object>> getOrganizationTenantStats(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[OrganizationController] getOrganizationTenantStats - id={}", id);

        Map<String, Object> stats = organizationService.getOrganizationTenantStats(id);

        return ResponseEntity.ok(stats);
    }
}