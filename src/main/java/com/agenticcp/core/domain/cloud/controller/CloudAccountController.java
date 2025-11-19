package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.domain.cloud.port.model.account.*;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.service.account.CloudAccountUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클라우드 계정 관리 Controller
 * 클라우드 계정의 등록, 조회, 수정, 삭제 등의 API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/cloud/accounts")
@RequiredArgsConstructor
@Tag(name = "Cloud Account", description = "클라우드 계정 관리 API")
public class CloudAccountController {

    private final CloudAccountUseCaseService cloudAccountUseCaseService;

    /**
     * 클라우드 계정을 등록합니다.
     * 
     * @param request 계정 등록 요청
     * @return CloudAccountDto 등록된 계정 정보
     */
    @PostMapping
    @Operation(summary = "클라우드 계정 등록", description = "새로운 클라우드 계정을 등록합니다. 자격증명 검증 후 등록됩니다.")
    public ResponseEntity<CloudAccountDto> registerCloudAccount(
            @Valid @RequestBody RegisterCloudAccountRequest request) {
        log.info("[CloudAccountController] registerCloudAccount - providerType={}", 
                 request.getProviderType());
        
        CloudAccountDto result = cloudAccountUseCaseService.registerCloudAccount(request);
        
        log.info("[CloudAccountController] registerCloudAccount - success, accountId={}", 
                 result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 테넌트의 모든 클라우드 계정을 조회합니다.
     * 
     * @return CloudAccountDto 리스트
     */
    @GetMapping
    @Operation(summary = "전체 계정 목록 조회", description = "현재 테넌트의 모든 클라우드 계정을 조회합니다.")
    public ResponseEntity<List<CloudAccountDto>> getAllCloudAccounts() {
        log.info("[CloudAccountController] getAllCloudAccounts");
        
        List<CloudAccountDto> result = cloudAccountUseCaseService.getAllCloudAccounts();
        
        log.info("[CloudAccountController] getAllCloudAccounts - success, count={}", 
                 result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 클라우드 계정을 조회합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "특정 계정 조회", description = "계정 ID로 특정 클라우드 계정을 조회합니다.")
    public ResponseEntity<CloudAccountDto> getCloudAccountById(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId) {
        log.info("[CloudAccountController] getCloudAccountById - accountId={}", accountId);
        
        CloudAccountDto result = cloudAccountUseCaseService.getCloudAccountById(accountId);
        
        log.info("[CloudAccountController] getCloudAccountById - success");
        return ResponseEntity.ok(result);
    }

    /**
     * 클라우드 계정 정보를 수정합니다.
     * 
     * @param accountId 계정 ID
     * @param request 수정 요청
     * @return CloudAccountDto 수정된 계정 정보
     */
    @PutMapping("/{accountId}")
    @Operation(summary = "계정 정보 수정", description = "클라우드 계정 정보를 수정합니다.")
    public ResponseEntity<CloudAccountDto> updateCloudAccount(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateCloudAccountRequest request) {
        log.info("[CloudAccountController] updateCloudAccount - accountId={}", accountId);
        
        CloudAccountDto result = cloudAccountUseCaseService.updateCloudAccount(accountId, request);
        
        log.info("[CloudAccountController] updateCloudAccount - success");
        return ResponseEntity.ok(result);
    }

    /**
     * 클라우드 계정을 삭제합니다.
     * 
     * @param accountId 계정 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{accountId}")
    @Operation(summary = "계정 삭제", description = "클라우드 계정을 삭제합니다. (소프트 삭제)")
    public ResponseEntity<Void> deleteCloudAccount(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId) {
        log.info("[CloudAccountController] deleteCloudAccount - accountId={}", accountId);
        
        cloudAccountUseCaseService.deleteCloudAccount(accountId);
        
        log.info("[CloudAccountController] deleteCloudAccount - success");
        return ResponseEntity.noContent().build();
    }

    /**
     * 계정 등록 전 자격증명을 검증합니다.
     * 
     * @param request 검증 요청
     * @return AccountValidationResult 검증 결과
     */
    @PostMapping("/validate")
    @Operation(summary = "계정 사전 검증", description = "계정 등록 전 자격증명의 유효성을 검증합니다.")
    public ResponseEntity<AccountValidationResult> validateAccount(
            @Valid @RequestBody AccountValidationRequest request) {
        log.info("[CloudAccountController] validateAccount - providerType={}", 
                 request.getProviderType());
        
        AccountValidationResult result = 
            cloudAccountUseCaseService.validateAccountBeforeRegistration(request);
        
        log.info("[CloudAccountController] validateAccount - success, valid={}", 
                 result.getValid());
        return ResponseEntity.ok(result);
    }

    /**
     * 계정 연결을 테스트합니다.
     * 
     * @param accountId 계정 ID
     * @return ConnectionTestResult 연결 테스트 결과
     */
    @PostMapping("/{accountId}/test-connection")
    @Operation(summary = "연결 테스트", description = "등록된 계정의 연결 상태를 테스트합니다.")
    public ResponseEntity<ConnectionTestResult> testConnection(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId) {
        log.info("[CloudAccountController] testConnection - accountId={}", accountId);
        
        ConnectionTestResult result = cloudAccountUseCaseService.testConnection(accountId);
        
        log.info("[CloudAccountController] testConnection - success, success={}", 
                 result.getSuccess());
        return ResponseEntity.ok(result);
    }

    /**
     * 계정 정보를 동기화합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto 동기화된 계정 정보
     */
    @PostMapping("/{accountId}/sync")
    @Operation(summary = "계정 정보 동기화", description = "클라우드 프로바이더로부터 최신 계정 정보를 동기화합니다.")
    public ResponseEntity<CloudAccountDto> syncAccountInfo(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId) {
        log.info("[CloudAccountController] syncAccountInfo - accountId={}", accountId);
        
        CloudAccountDto result = cloudAccountUseCaseService.syncAccountInfo(accountId);
        
        log.info("[CloudAccountController] syncAccountInfo - success");
        return ResponseEntity.ok(result);
    }

    /**
     * 프로바이더별 계정을 조회합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return CloudAccountDto 리스트
     */
    @GetMapping("/provider/{providerType}")
    @Operation(summary = "프로바이더별 계정 조회", description = "특정 프로바이더 타입의 계정들을 조회합니다.")
    public ResponseEntity<List<CloudAccountDto>> getAccountsByProvider(
            @Parameter(description = "프로바이더 타입 (AWS, AZURE, GCP)", required = true)
            @PathVariable ProviderType providerType) {
        log.info("[CloudAccountController] getAccountsByProvider - providerType={}", 
                 providerType);
        
        List<CloudAccountDto> result = 
            cloudAccountUseCaseService.getAccountsByProviderType(providerType);
        
        log.info("[CloudAccountController] getAccountsByProvider - success, count={}", 
                 result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 기본 계정으로 설정합니다.
     * 
     * @param accountId 계정 ID
     * @return CloudAccountDto 업데이트된 계정 정보
     */
    @PatchMapping("/{accountId}/set-default")
    @Operation(summary = "기본 계정 설정", description = "특정 계정을 프로바이더 타입의 기본 계정으로 설정합니다.")
    public ResponseEntity<CloudAccountDto> setAsDefault(
            @Parameter(description = "계정 ID", required = true)
            @PathVariable Long accountId) {
        log.info("[CloudAccountController] setAsDefault - accountId={}", accountId);
        
        CloudAccountDto result = cloudAccountUseCaseService.setAccountAsDefault(accountId);
        
        log.info("[CloudAccountController] setAsDefault - success");
        return ResponseEntity.ok(result);
    }

    /**
     * 프로바이더 타입별 기본 계정을 조회합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return CloudAccountDto
     */
    @GetMapping("/default/{providerType}")
    @Operation(summary = "프로바이더 타입별 기본 계정 조회", 
               description = "특정 프로바이더 타입의 기본 계정을 조회합니다.")
    public ResponseEntity<CloudAccountDto> getDefaultAccountByProvider(
            @Parameter(description = "프로바이더 타입 (AWS, AZURE, GCP)", required = true)
            @PathVariable ProviderType providerType) {
        log.info("[CloudAccountController] getDefaultAccountByProvider - providerType={}", 
                 providerType);
        
        CloudAccountDto result = 
            cloudAccountUseCaseService.getDefaultAccountByProviderType(providerType);
        
        log.info("[CloudAccountController] getDefaultAccountByProvider - success");
        return ResponseEntity.ok(result);
    }
}

