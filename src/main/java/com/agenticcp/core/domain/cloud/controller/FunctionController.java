package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.dto.FunctionCreateRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionInvokeRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionQueryRequest;
import com.agenticcp.core.domain.cloud.dto.FunctionResponse;
import com.agenticcp.core.domain.cloud.dto.FunctionUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.service.function.FunctionUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Serverless Function 관리 REST API 컨트롤러
 *
 * 멀티 클라우드(AWS Lambda, Azure Functions, GCP Cloud Functions) Function의 생성, 조회, 수정, 삭제, 실행 기능을 제공합니다.
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/functions")
@RequiredArgsConstructor
@Tag(name = "Serverless Function Management", description = "Serverless Function 관리 API (멀티 클라우드 지원)")
public class FunctionController {

    private final FunctionUseCaseService functionUseCaseService;

    // ==================== Function 생성 ====================

    /**
     * Serverless Function을 생성합니다.
     *
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @param request Function 생성 요청
     * @return 생성된 Function 리소스
     */
    @PostMapping
    @PreAuthorize("hasAuthority('FUNCTION_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "CREATE_FUNCTION",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 생성",
            includeRequestData = true,
            includeResponseData = true,
            severity = AuditSeverity.MEDIUM
    )
    @Operation(
            summary = "Serverless Function 생성",
            description = "새로운 Serverless Function을 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Function 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<FunctionResponse>> createFunction(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Function 생성 요청", required = true)
            @Valid @RequestBody FunctionCreateRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);

        log.info("[FunctionController] createFunction - provider={}, accountScope={}, functionName={}",
                provider, accountScope, request.getFunctionName());

        CloudResource resource = functionUseCaseService.createFunction(request);
        FunctionResponse response = FunctionResponse.from(resource);

        log.info("[FunctionController] createFunction - success resourceId={}", resource.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Serverless Function 생성에 성공했습니다."));
    }

    // ==================== Function 조회 ====================

    /**
     * Serverless Function 목록을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 조회 요청 (Query 파라미터)
     * @return Function 리소스 목록
     */
    @GetMapping
    @PreAuthorize("hasAuthority('FUNCTION_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "LIST_FUNCTIONS",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 목록 조회",
            includeRequestData = false,
            includeResponseData = false,
            severity = AuditSeverity.LOW
    )
    @Operation(
            summary = "Serverless Function 목록 조회",
            description = "지정된 클라우드 프로바이더의 Function 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Function 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Page<FunctionResponse>>> listFunctions(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Valid FunctionQueryRequest request) {

        request.setProviderType(provider);
        request.setAccountScope(accountScope);

        log.info("[FunctionController] listFunctions - provider={}, accountScope={}",
                provider, accountScope);

        Page<CloudResource> resources = functionUseCaseService.listFunctions(request);
        Page<FunctionResponse> responses = resources.map(FunctionResponse::from);

        log.info("[FunctionController] listFunctions - success count={}", responses.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(responses, "Serverless Function 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 Serverless Function을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param functionId Function ID/ARN
     * @param region 리전
     * @return Function 리소스
     */
    @GetMapping("/{functionId}")
    @PreAuthorize("hasAuthority('FUNCTION_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "GET_FUNCTION",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 조회",
            includeRequestData = false,
            includeResponseData = true,
            severity = AuditSeverity.LOW
    )
    @Operation(
            summary = "Serverless Function 상세 조회",
            description = "특정 Function의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Function 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Function을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<FunctionResponse>> getFunction(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Function ID/ARN", required = true, example = "arn:aws:lambda:us-east-1:123456789012:function:my-function")
            @PathVariable String functionId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region) {

        log.info("[FunctionController] getFunction - provider={}, accountScope={}, functionId={}, region={}",
                provider, accountScope, functionId, region);

        Optional<CloudResource> resource = functionUseCaseService.getFunction(provider, accountScope, region, functionId);

        if (resource.isPresent()) {
            FunctionResponse response = FunctionResponse.from(resource.get());
            log.info("[FunctionController] getFunction - success functionId={}", functionId);
            return ResponseEntity.ok(ApiResponse.success(response, "Serverless Function 조회에 성공했습니다."));
        } else {
            log.warn("[FunctionController] getFunction - not found functionId={}", functionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== Function 수정 ====================

    /**
     * Serverless Function을 수정합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param functionId Function ID/ARN
     * @param region 리전
     * @param request Function 수정 요청
     * @return 수정된 Function 리소스
     */
    @PutMapping("/{functionId}")
    @PreAuthorize("hasAuthority('FUNCTION_UPDATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "UPDATE_FUNCTION",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 수정",
            includeRequestData = true,
            includeResponseData = true,
            severity = AuditSeverity.MEDIUM
    )
    @Operation(
            summary = "Serverless Function 수정",
            description = "Function의 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Function 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Function을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<FunctionResponse>> updateFunction(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Function ID/ARN", required = true, example = "arn:aws:lambda:us-east-1:123456789012:function:my-function")
            @PathVariable String functionId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region,

            @Parameter(description = "Function 수정 요청", required = true)
            @Valid @RequestBody FunctionUpdateRequest request) {

        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setFunctionId(functionId);
        request.setRegion(region);

        log.info("[FunctionController] updateFunction - provider={}, accountScope={}, functionId={}, region={}",
                provider, accountScope, functionId, region);

        CloudResource resource = functionUseCaseService.updateFunction(request);
        FunctionResponse response = FunctionResponse.from(resource);

        log.info("[FunctionController] updateFunction - success functionId={}", functionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Serverless Function 수정에 성공했습니다."));
    }

    // ==================== Function 삭제 ====================

    /**
     * Serverless Function을 삭제합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param functionId Function ID/ARN
     * @param region 리전
     * @param request 삭제 요청 (선택)
     * @return 삭제 결과
     */
    @DeleteMapping("/{functionId}")
    @PreAuthorize("hasAuthority('FUNCTION_DELETE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "DELETE_FUNCTION",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 삭제",
            includeRequestData = true,
            includeResponseData = false,
            severity = AuditSeverity.HIGH
    )
    @Operation(
            summary = "Serverless Function 삭제",
            description = "Function을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Function 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Function을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteFunction(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Function ID/ARN", required = true, example = "arn:aws:lambda:us-east-1:123456789012:function:my-function")
            @PathVariable String functionId,

            @Parameter(description = "리전", required = true, example = "us-east-1")
            @RequestParam String region,

            @Parameter(description = "Function 삭제 요청")
            @RequestBody(required = false) FunctionDeleteRequest request) {

        if (request == null) {
            request = FunctionDeleteRequest.builder()
                    .functionId(functionId)
                    .build();
        }

        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setFunctionId(functionId);
        request.setRegion(region);

        log.info("[FunctionController] deleteFunction - provider={}, accountScope={}, functionId={}, region={}",
                provider, accountScope, functionId, region);

        functionUseCaseService.deleteFunction(request);

        log.info("[FunctionController] deleteFunction - success functionId={}", functionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Function 실행 ====================

    /**
     * Serverless Function을 실행합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param functionId Function ID/ARN
     * @param request 실행 요청
     * @return 실행 결과
     */
    @PostMapping("/{functionId}/invoke")
    @PreAuthorize("hasAuthority('FUNCTION_INVOKE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "INVOKE_FUNCTION",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Serverless Function 실행",
            includeRequestData = true,
            includeResponseData = true,
            severity = AuditSeverity.MEDIUM
    )
    @Operation(
            summary = "Serverless Function 실행",
            description = "Function을 실행합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Function 실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Function을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<String>> invokeFunction(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Function ID/ARN", required = true, example = "arn:aws:lambda:us-east-1:123456789012:function:my-function")
            @PathVariable String functionId,

            @Parameter(description = "Function 실행 요청", required = true)
            @Valid @RequestBody FunctionInvokeRequest request) {

        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setFunctionId(functionId);

        log.info("[FunctionController] invokeFunction - provider={}, accountScope={}, functionId={}, invocationType={}",
                provider, accountScope, functionId, request.getInvocationType());

        String result = functionUseCaseService.invokeFunction(request);

        log.info("[FunctionController] invokeFunction - success functionId={}", functionId);
        return ResponseEntity.ok(ApiResponse.success(result, "Serverless Function 실행에 성공했습니다."));
    }
}
