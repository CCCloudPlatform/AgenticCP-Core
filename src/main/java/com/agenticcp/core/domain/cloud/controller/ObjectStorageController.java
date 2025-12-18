package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.dto.CreateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.dto.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.dto.UpdateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.service.storage.ObjectStorageUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;

/**
 * Object Storage Container 관리 REST API 컨트롤러
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/storage/containers")
@RequiredArgsConstructor
@Tag(name = "Object Storage Container Management", description = "Object Storage Container 관리 API")
public class ObjectStorageController {

    private final ObjectStorageUseCaseService objectStorageUseCaseService;

    @PostMapping
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_CREATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "CREATE_CONTAINER",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Object Storage Container 생성",
            severity = AuditSeverity.HIGH,
            includeRequestData = true,
            includeResponseData = true
    )
    @Operation(
            summary = "Object Storage Container 생성",
            description = "새로운 Object Storage Container를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Container 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Container 이름 중복"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> createContainer(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Object Storage Container 생성 요청", required = true)
            @Valid @RequestBody CreateObjectStorageContainerRequest request) {

        request.setProviderType(provider);
        request.setAccountScope(accountScope);

        log.info("[ObjectStorageController] createContainer - provider={}, accountScope={}, containerName={}",
                provider, accountScope, request.getContainerName());
        
        CloudResource container = objectStorageUseCaseService.createContainer(request);
        
        log.info("[ObjectStorageController] createContainer - success containerId={}", container.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(container, "Object Storage Container 생성에 성공했습니다."));
    }


    @PutMapping("/{containerName}")
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_UPDATE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "UPDATE_CONTAINER",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Object Storage Container 설정 업데이트",
            severity = AuditSeverity.HIGH,
            includeRequestData = true,
            includeResponseData = true
    )
    @Operation(
            summary = "Object Storage Container 업데이트",
            description = "기존 Object Storage Container의 설정을 업데이트합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Container 업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Container를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> updateContainer(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Container 이름", required = true, example = "my-container")
            @PathVariable String containerName,

            @Parameter(description = "Object Storage Container 업데이트 요청", required = true)
            @Valid @RequestBody UpdateObjectStorageContainerRequest request) {

        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setContainerName(containerName);

        log.info("[ObjectStorageController] updateContainer - provider={}, accountScope={}, containerName={}",
                provider, accountScope, containerName);
        
        CloudResource container = objectStorageUseCaseService.updateContainer(request);
        log.info("[ObjectStorageController] updateContainer - success containerId={}", container.getId());

        return ResponseEntity.ok(ApiResponse.success(container, "Object Storage Container 업데이트에 성공했습니다."));
    }


    @GetMapping
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "LIST_CONTAINERS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "Object Storage Container 목록 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "Object Storage Container 목록 조회",
        description = "지정된 클라우드 프로바이더의 Object Storage Container 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Container 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Page<CloudResource>>> listContainers(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("[ObjectStorageController] listContainers - provider={}, accountScope={}, page={}, size={}",
                provider, accountScope, page, size);
        
        ObjectStorageContainerQueryRequest query = ObjectStorageContainerQueryRequest.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .page(page)
                .size(size)
                .build();

        Page<CloudResource> containers = objectStorageUseCaseService.listContainers(query);
        
        log.info("[ObjectStorageController] listContainers - success count={}", containers.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(containers, "Object Storage Container 목록 조회에 성공했습니다."));
    }


    @GetMapping("/{containerName}")
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "GET_CONTAINER",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "Object Storage Container 상세 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "Object Storage Container 조회",
        description = "지정된 Object Storage Container의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Container 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Container를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> getContainer(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Container 이름", required = true, example = "my-container")
            @PathVariable String containerName) {

        log.info("[ObjectStorageController] getContainer - provider={}, accountScope={}, containerName={}",
                provider, accountScope, containerName);
        
        CloudResource container = objectStorageUseCaseService.getContainer(provider, accountScope, containerName);
        
        log.info("[ObjectStorageController] getContainer - success containerId={}", container.getId());
        return ResponseEntity.ok(ApiResponse.success(container, "Object Storage Container 조회에 성공했습니다."));
    }


    @RequestMapping(value = "/{containerName}", method = RequestMethod.HEAD)
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_READ') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
        action = "CHECK_CONTAINER_EXISTS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "Object Storage Container 존재 확인 (HEAD)",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(
        summary = "Object Storage Container 존재 확인 (HEAD)",
        description = "지정된 Object Storage Container의 존재 여부를 HEAD 요청으로 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Container 존재함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Container 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> containerExists(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Container 이름", required = true, example = "my-container")
            @PathVariable String containerName) {

        log.info("[ObjectStorageController] containerExists - provider={}, accountScope={}, containerName={}",
                provider, accountScope, containerName);

        boolean exists = objectStorageUseCaseService.containerExists(provider, accountScope, containerName);

        log.info("[ObjectStorageController] containerExists - success exists={}", exists);

        if (exists) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    @DeleteMapping("/{containerName}")
    @PreAuthorize("hasAuthority('OBJECT_STORAGE_CONTAINER_DELETE') or hasRole('SUPER_ADMIN')")
    @AuditRequired(
            action = "DELETE_CONTAINER",
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            description = "Object Storage Container 삭제",
            severity = AuditSeverity.CRITICAL,
            includeRequestData = true,
            includeResponseData = false
    )
    @Operation(
            summary = "Object Storage Container 삭제",
            description = "지정된 Object Storage Container를 삭제합니다. force=true일 경우 내용물 포함 강제 삭제"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Container 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Container를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteContainer(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,

            @Parameter(description = "계정 식별자 (Account ID 등)", required = true, example = "123456789012")
            @PathVariable String accountScope,

            @Parameter(description = "Container 이름", required = true, example = "my-container")
            @PathVariable String containerName,

            @Parameter(description = "강제 삭제 여부 (내용물 포함)", example = "false")
            @RequestParam(defaultValue = "false") boolean force) {

        log.info("[ObjectStorageController] deleteContainer - provider={}, accountScope={}, containerName={}, force={}",
                provider, accountScope, containerName, force);

        if (force) {
            // force 값에 따라 다른 서비스 호출
            objectStorageUseCaseService.forceDeleteContainer(provider, accountScope, containerName);
            log.info("[ObjectStorageController] forceDeleteContainer - success containerName={}", containerName);
        } else {
            objectStorageUseCaseService.deleteContainer(provider, accountScope, containerName);
            log.info("[ObjectStorageController] deleteContainer - success containerName={}", containerName);
        }

        return ResponseEntity.noContent().build();
    }
}
