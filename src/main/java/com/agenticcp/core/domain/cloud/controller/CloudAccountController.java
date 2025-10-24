package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.cloud.dto.*;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.mapper.CloudAccountMapper;
import com.agenticcp.core.domain.cloud.service.CloudAccountService;
import com.agenticcp.core.domain.cloud.service.CloudAccountUseCaseService;
import com.agenticcp.core.domain.cloud.service.CloudProviderService;
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
import java.util.Map;
import java.util.Optional;

/**
 * 클라우드 계정 관리 Controller
 * 
 * 멀티 클라우드 계정(AWS, GCP, Azure) 등록, 조회, 수정, 삭제 API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/accounts")
@RequiredArgsConstructor
@Tag(name = "Cloud Account Management", description = "클라우드 계정 관리 API")
public class CloudAccountController {
    
    private final CloudAccountUseCaseService cloudAccountUseCaseService;
    private final CloudAccountService cloudAccountService;
    private final CloudAccountMapper cloudAccountMapper;
    private final CloudProviderService cloudProviderService;
    
    @GetMapping
    @Operation(
        summary = "클라우드 계정 목록 조회",
        description = "페이징을 지원하는 클라우드 계정 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<CloudAccountListResponse>> getCloudAccounts(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @RequestParam Long tenantId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        log.info("[CloudAccountController] getCloudAccounts - tenantId={}, page={}, size={}", tenantId, page, size);
        
        List<CloudAccountDto> accounts = cloudAccountUseCaseService.getMultiCloudAccounts(tenantId);
        
        // 페이징 처리
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, accounts.size());
        List<CloudAccountDto> pagedAccounts = accounts.subList(startIndex, endIndex);
        
        int totalPages = (int) Math.ceil((double) accounts.size() / size);
        
        CloudAccountListResponse response = CloudAccountListResponse.builder()
                .accounts(pagedAccounts)
                .totalCount((long) accounts.size())
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "클라우드 계정 상세 조회",
        description = "계정 ID로 특정 클라우드 계정 정보를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<CloudAccountDto>> getCloudAccount(
            @Parameter(description = "계정 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[CloudAccountController] getCloudAccount - id={}", id);
        
        CloudAccount account = cloudAccountService.getAccountByIdOrThrow(id);
        CloudAccountDto accountDto = cloudAccountMapper.toDto(account);
        
        return ResponseEntity.ok(ApiResponse.success(accountDto));
    }
    
    @GetMapping("/tenant/{tenantId}")
    @Operation(
        summary = "테넌트별 클라우드 계정 목록 조회",
        description = "특정 테넌트의 모든 클라우드 계정을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<List<CloudAccountDto>>> getCloudAccountsByTenant(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @PathVariable @Positive Long tenantId) {
        log.info("[CloudAccountController] getCloudAccountsByTenant - tenantId={}", tenantId);
        
        List<CloudAccountDto> accountDtos = cloudAccountUseCaseService.getMultiCloudAccounts(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(accountDtos));
    }
    
    @GetMapping("/provider/{providerType}")
    @Operation(
        summary = "프로바이더별 클라우드 계정 목록 조회",
        description = "특정 프로바이더(AWS, GCP, Azure)의 모든 계정을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<List<CloudAccountDto>>> getCloudAccountsByProvider(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @RequestParam Long tenantId,
            @Parameter(description = "프로바이더 타입 (AWS, GCP, AZURE)", required = true, example = "AWS")
            @PathVariable ProviderType providerType) {
        log.info("[CloudAccountController] getCloudAccountsByProvider - tenantId={}, providerType={}", tenantId, providerType);
        
        List<CloudAccountDto> accountDtos = cloudAccountUseCaseService.getCloudAccountsByProvider(tenantId, providerType);
        
        return ResponseEntity.ok(ApiResponse.success(accountDtos));
    }
    
    @GetMapping("/default/{providerType}")
    @Operation(
        summary = "프로바이더별 기본 계정 조회",
        description = "특정 프로바이더의 기본 계정을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기본 계정이 설정되지 않음")
    })
    public ResponseEntity<ApiResponse<CloudAccountDto>> getDefaultCloudAccount(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @RequestParam @Positive Long tenantId,
            @Parameter(description = "프로바이더 타입 (AWS, GCP, AZURE)", required = true, example = "AWS")
            @PathVariable ProviderType providerType) {
        log.info("[CloudAccountController] getDefaultCloudAccount - tenantId={}, providerType={}", 
                tenantId, providerType);
        
        // 1. ProviderType으로 Provider 조회
        List<CloudProvider> providers = cloudProviderService.getProvidersByType(providerType);
        if (providers.isEmpty()) {
            throw new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    com.agenticcp.core.domain.cloud.exception.CloudErrorCode.CLOUD_ACCOUNT_NOT_FOUND);
        }
        
        CloudProvider provider = providers.get(0); // 첫 번째 프로바이더 사용
        
        // 2. 기본 계정 조회
        Optional<CloudAccount> defaultAccount = cloudAccountService.getDefaultAccountByTenantIdAndProviderId(tenantId, provider.getId());
        
        if (defaultAccount.isEmpty()) {
            throw new com.agenticcp.core.common.exception.ResourceNotFoundException(
                    com.agenticcp.core.domain.cloud.exception.CloudErrorCode.CLOUD_ACCOUNT_NOT_FOUND);
        }
        
        CloudAccountDto accountDto = cloudAccountMapper.toDto(defaultAccount.get());
        
        return ResponseEntity.ok(ApiResponse.success(accountDto));
    }
    
    @PostMapping
    @Operation(
        summary = "클라우드 계정 등록",
        description = "새로운 클라우드 계정을 등록합니다. (AWS, GCP, Azure 지원)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 계정")
    })
    public ResponseEntity<ApiResponse<CloudAccountDto>> registerCloudAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "클라우드 계정 등록 요청 정보 (CSP별 전용 Request 사용)",
                required = true,
                content = @Content(schema = @Schema(implementation = RegisterCloudAccountRequest.class))
            )
            @Valid @RequestBody RegisterCloudAccountRequest request) {
        log.info("[CloudAccountController] registerCloudAccount - tenantId={}, providerId={}, accountId={}", 
                request.getTenantId(), request.getProviderId(), request.getAccountId());
        
        // 1. ProviderId로 Provider 조회하여 ProviderType 확인
        CloudProvider provider = cloudProviderService.getProviderByIdOrThrow(request.getProviderId());
        ProviderType providerType = provider.getProviderType();
        
        // 2. CSP별 등록 메서드 호출
        CloudAccountDto accountDto;
        switch (providerType) {
            case AWS:
                accountDto = cloudAccountUseCaseService.registerAwsAccount((RegisterAwsAccountRequest) request);
                break;
            case GCP:
                accountDto = cloudAccountUseCaseService.registerGcpAccount((RegisterGcpAccountRequest) request);
                break;
            case AZURE:
                accountDto = cloudAccountUseCaseService.registerAzureAccount((RegisterAzureAccountRequest) request);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountDto, "클라우드 계정이 등록되었습니다."));
    }
    
    @PutMapping("/{id}")
    @Operation(
        summary = "클라우드 계정 수정",
        description = "기존 클라우드 계정의 정보를 수정합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<ApiResponse<CloudAccountDto>> updateCloudAccount(
            @Parameter(description = "계정 ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "클라우드 계정 수정 요청 정보",
                required = true
            )
            @Valid @RequestBody UpdateCloudAccountRequest request) {
        log.info("[CloudAccountController] updateCloudAccount - id={}", id);
        
        CloudAccountDto accountDto = cloudAccountUseCaseService.updateAccount(id, request);
        
        return ResponseEntity.ok(ApiResponse.success(accountDto, "클라우드 계정이 수정되었습니다."));
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "클라우드 계정 삭제",
        description = "클라우드 계정을 삭제합니다. (Soft Delete)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "연결된 리소스가 있어 삭제 불가")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCloudAccount(
            @Parameter(description = "계정 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[CloudAccountController] deleteCloudAccount - id={}", id);
        
        cloudAccountUseCaseService.deleteAccount(id);
        
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/set-default")
    @Operation(
        summary = "기본 계정으로 설정",
        description = "해당 프로바이더의 기본 계정으로 설정합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<CloudAccountDto>> setDefaultAccount(
            @Parameter(description = "계정 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[CloudAccountController] setDefaultAccount - id={}", id);
        
        CloudAccount updatedAccount = cloudAccountService.setAsDefaultAccount(id);
        CloudAccountDto accountDto = cloudAccountMapper.toDto(updatedAccount);
        
        return ResponseEntity.ok(ApiResponse.success(accountDto, "기본 계정으로 설정되었습니다."));
    }
    
    @PostMapping("/{id}/test-connection")
    @Operation(
        summary = "계정 연결 테스트",
        description = "클라우드 계정의 연결 상태를 실시간으로 테스트합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 완료 (성공/실패 여부는 응답 본문 확인)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<ConnectionTestResult>> testConnection(
            @Parameter(description = "계정 ID", required = true, example = "1")
            @PathVariable @Positive Long id) {
        log.info("[CloudAccountController] testConnection - id={}", id);
        
        ConnectionTestResult result = cloudAccountUseCaseService.testConnection(id);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PostMapping("/sync")
    @Operation(
        summary = "계정 동기화",
        description = "테넌트의 모든 클라우드 계정을 동기화합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동기화 완료")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncAllAccounts(
            @Parameter(description = "테넌트 ID", required = true, example = "1")
            @RequestParam Long tenantId) {
        log.info("[CloudAccountController] syncAllAccounts - tenantId={}", tenantId);
        
        Map<String, Object> result = cloudAccountUseCaseService.syncAllAccounts(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
}
