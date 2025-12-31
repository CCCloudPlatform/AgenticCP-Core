package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AddMemberRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationMemberResponse;
import com.agenticcp.core.domain.organization.service.OrganizationMemberService;
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
 * 조직 멤버 관리 컨트롤러
 * 
 * <p>조직과 사용자 간의 관계를 관리하는 API를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
@RequiredArgsConstructor
@Tag(name = "Organization Member Management", description = "조직 멤버 관리 API")
public class OrganizationMemberController {
    
    private final OrganizationMemberService organizationMemberService;
    
    /**
     * 조직의 멤버 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return 멤버 목록
     */
    @GetMapping
    @Operation(
        summary = "조직 멤버 목록 조회",
        description = "특정 조직의 멤버 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationMemberResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> getMembers(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long organizationId) {
        log.info("[OrganizationMemberController] getMembers - organizationId={}", organizationId);
        
        List<OrganizationMemberResponse> responses = organizationMemberService.getMembers(organizationId)
                .stream()
                .map(OrganizationMemberResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "멤버 목록을 성공적으로 조회했습니다."));
    }
    
    /**
     * 조직에 멤버 추가
     * 
     * @param organizationId 조직 ID
     * @param request 멤버 추가 요청 정보
     * @return 추가된 멤버 정보
     */
    @PostMapping
    @Operation(
        summary = "조직에 멤버 추가",
        description = "조직에 사용자를 멤버로 추가합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "멤버 추가 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationMemberResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직 또는 사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 멤버로 등록된 사용자")
    })
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> addMember(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "멤버 추가 요청 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = AddMemberRequest.class))
            )
            @Valid @RequestBody AddMemberRequest request) {
        log.info("[OrganizationMemberController] addMember - organizationId={}, userId={}", 
                organizationId, request.getUserId());
        
        OrganizationMemberResponse response = OrganizationMemberResponse.from(
                organizationMemberService.addMember(organizationId, request.getUserId(), request.getRole()));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "멤버가 성공적으로 추가되었습니다."));
    }
    
    /**
     * 조직에서 멤버 제거
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     */
    @DeleteMapping("/{userId}")
    @Operation(
        summary = "조직에서 멤버 제거",
        description = "조직에서 사용자를 멤버에서 제거합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "멤버 제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "조직 또는 멤버를 찾을 수 없음")
    })
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "조직 ID", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId) {
        log.info("[OrganizationMemberController] removeMember - organizationId={}, userId={}", 
                organizationId, userId);
        
        organizationMemberService.removeMember(organizationId, userId);
        
        return ResponseEntity.noContent().build();
    }
    
}
