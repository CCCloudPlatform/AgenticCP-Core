package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationMemberResponse;
import com.agenticcp.core.domain.organization.service.OrganizationMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 조직 관리 컨트롤러
 * 
 * <p>사용자가 속한 조직 목록을 조회하는 API를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/organizations")
@RequiredArgsConstructor
@Tag(name = "User Organization Management", description = "사용자 조직 관리 API")
public class UserOrganizationController {
    
    private final OrganizationMemberService organizationMemberService;
    
    /**
     * 사용자가 속한 조직 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 조직 멤버십 목록
     */
    @GetMapping
    @Operation(
        summary = "사용자의 조직 목록 조회",
        description = "특정 사용자가 속한 조직 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                     content = @Content(schema = @Schema(implementation = OrganizationMemberResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> getOrganizationsByUserId(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable @Positive Long userId) {
        log.info("[UserOrganizationController] getOrganizationsByUserId - userId={}", userId);
        
        List<OrganizationMemberResponse> responses = organizationMemberService.getOrganizationsByUserId(userId)
                .stream()
                .map(OrganizationMemberResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "조직 목록을 성공적으로 조회했습니다."));
    }
}
