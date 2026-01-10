package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.dto.*;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlCreateTableCommand;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlMetricQuery;
import com.agenticcp.core.domain.cloud.service.nosql.NoSqlUseCaseService;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * NoSQL 테이블 관리를 위한 REST API 컨트롤러
 *
 * 멀티 클라우드(AWS DynamoDB, GCP Firestore, Azure Cosmos DB) NoSQL 테이블의
 * 생성, 조회, 수정, 삭제, 백업, 태그, 메트릭 조회 기능을 제공합니다.
 *
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/nosql/tables")
@RequiredArgsConstructor
@Tag(name = "NoSQL Management", description = "NoSQL 테이블 관리 API (멀티 클라우드 지원)")
public class NoSqlController {

    private final NoSqlUseCaseService noSqlUseCaseService;

    // ==================== 테이블 조회 ====================

    /**
     * NoSQL 테이블 목록을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @return CloudResource 페이지
     */
    @GetMapping
    @Operation(summary = "NoSQL 테이블 목록 조회", description = "조건에 맞는 NoSQL 테이블 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Page<NoSqlTableResponse>>> listTables(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "테이블 이름 검색") @RequestParam(required = false) String nameContains,
            @Parameter(description = "리전") @RequestParam(required = false) String region) {

        log.info("[NoSqlController] listTables - provider={}, accountScope={}, page={}, size={}",
                provider, accountScope, page, size);

        NoSqlListTablesRequest request = NoSqlListTablesRequest.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .nameContains(nameContains)
                .regions(region != null ? java.util.Set.of(region) : null)
                .page(page)
                .size(size)
                .build();

        // ResourceQuery로 변환하여 서비스 호출
        var resourceQuery = com.agenticcp.core.domain.cloud.port.model.ResourceQuery.builder()
                .providerType(provider)
                .accountScope(accountScope)
                .regions(request.getRegions())
                .nameContains(nameContains)
                .page(page)
                .size(size)
                .build();

        Page<CloudResource> result = noSqlUseCaseService.listTables(provider, accountScope, resourceQuery);

        // CloudResource를 NoSqlTableResponse로 변환
        Page<NoSqlTableResponse> responsePage = result.map(NoSqlTableResponse::from);

        log.info("[NoSqlController] listTables - success, totalElements={}", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(responsePage, "NoSQL 테이블 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 NoSQL 테이블을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @return NoSqlTableResponse 또는 404
     */
    @GetMapping("/{tableName}")
    @Operation(summary = "NoSQL 테이블 상세 조회", description = "특정 NoSQL 테이블의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlTableResponse>> getTable(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName) {

        log.info("[NoSqlController] getTable - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        Optional<CloudResource> result = noSqlUseCaseService.getTable(provider, accountScope, tableName);

        if (result.isPresent()) {
            NoSqlTableResponse response = NoSqlTableResponse.from(result.get());
            log.info("[NoSqlController] getTable - success, tableName={}", tableName);
            return ResponseEntity.ok(ApiResponse.success(response, "NoSQL 테이블 조회에 성공했습니다."));
        } else {
            log.info("[NoSqlController] getTable - not found, tableName={}", tableName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== 테이블 생성/수정/삭제 ====================

    /**
     * 새로운 NoSQL 테이블을 생성합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 생성 요청 정보
     * @return 생성된 테이블 정보
     */
    @PostMapping
    @Operation(summary = "NoSQL 테이블 생성", description = "새로운 NoSQL 테이블을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "테이블 이미 존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlTableResponse>> createTable(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "생성 요청 정보")
            @Valid @RequestBody NoSqlCreateTableRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);

        log.info("[NoSqlController] createTable - provider={}, accountScope={}, tableName={}",
                provider, accountScope, request.getTableName());

        // BillingMode 변환
        NoSqlCreateTableCommand.BillingMode billingMode = request.getBillingMode() != null
                ? NoSqlCreateTableCommand.BillingMode.valueOf(request.getBillingMode().name())
                : NoSqlCreateTableCommand.BillingMode.PAY_PER_REQUEST;

        CloudResource cloudResource = noSqlUseCaseService.createTable(
                provider,
                accountScope,
                request.getTableName(),
                request.getRegion(),
                request.getPartitionKeyName(),
                request.getSortKeyName(),
                request.getKeyTypes(),
                billingMode,
                request.getReadCapacityUnits(),
                request.getWriteCapacityUnits(),
                request.getTags()
        );

        NoSqlTableResponse response = NoSqlTableResponse.from(cloudResource);
        log.info("[NoSqlController] createTable - success, tableId={}", cloudResource.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "NoSQL 테이블 생성에 성공했습니다."));
    }

    /**
     * NoSQL 테이블 구성을 수정합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 수정 요청 정보
     * @return 수정된 테이블 정보
     */
    @PutMapping("/{tableName}")
    @Operation(summary = "NoSQL 테이블 수정", description = "NoSQL 테이블의 구성을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlTableResponse>> updateTable(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "수정 요청 정보")
            @Valid @RequestBody NoSqlUpdateTableRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setTableName(tableName);

        log.info("[NoSqlController] updateTable - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        // BillingMode 변환
        NoSqlCreateTableCommand.BillingMode billingMode = request.getBillingMode() != null
                ? NoSqlCreateTableCommand.BillingMode.valueOf(request.getBillingMode().name())
                : null;

        CloudResource cloudResource = noSqlUseCaseService.updateTable(
                provider,
                accountScope,
                tableName,
                request.getRegion(),
                billingMode,
                request.getReadCapacityUnits(),
                request.getWriteCapacityUnits()
        );

        NoSqlTableResponse response = NoSqlTableResponse.from(cloudResource);
        log.info("[NoSqlController] updateTable - success, tableName={}", tableName);
        return ResponseEntity.ok(ApiResponse.success(response, "NoSQL 테이블 수정에 성공했습니다."));
    }

    /**
     * NoSQL 테이블을 삭제합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 삭제 요청 정보
     * @return 204 No Content
     */
    @DeleteMapping("/{tableName}")
    @Operation(summary = "NoSQL 테이블 삭제", description = "NoSQL 테이블을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "삭제 요청 정보")
            @RequestBody(required = false) NoSqlDeleteTableRequest request) {

        log.info("[NoSqlController] deleteTable - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        // 요청이 없으면 기본 삭제 요청 생성
        String region = request != null ? request.getRegion() : null;
        boolean force = request != null && request.isForce();
        String reason = request != null ? request.getReason() : null;

        noSqlUseCaseService.deleteTable(provider, accountScope, tableName, region, force, reason);

        log.info("[NoSqlController] deleteTable - success, tableName={}", tableName);
        return ResponseEntity.noContent().build();
    }

    // ==================== 백업 관리 ====================

    /**
     * NoSQL 테이블의 온디맨드 백업을 생성합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 백업 요청 정보
     * @return 생성된 백업 정보
     */
    @PostMapping("/{tableName}/backups")
    @Operation(summary = "NoSQL 테이블 백업 생성", description = "NoSQL 테이블의 온디맨드 백업을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "백업 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlBackupResponse>> createBackup(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "백업 요청 정보")
            @RequestBody(required = false) NoSqlBackupRequest request) {

        log.info("[NoSqlController] createBackup - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        String backupName = request != null ? request.getBackupName() : null;
        CloudResource cloudResource = noSqlUseCaseService.createBackup(provider, accountScope, tableName, backupName);

        NoSqlBackupResponse response = NoSqlBackupResponse.from(cloudResource);
        log.info("[NoSqlController] createBackup - success, backupId={}", cloudResource.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "NoSQL 테이블 백업 생성에 성공했습니다."));
    }

    /**
     * NoSQL 테이블의 백업 목록을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름 (선택)
     * @return 백업 목록
     */
    @GetMapping("/{tableName}/backups")
    @Operation(summary = "NoSQL 테이블 백업 목록 조회", description = "NoSQL 테이블의 백업 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Page<NoSqlBackupResponse>>> listBackups(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름")
            @PathVariable String tableName,
            @Parameter(description = "시작 시간") @RequestParam(required = false) Instant fromTime,
            @Parameter(description = "종료 시간") @RequestParam(required = false) Instant toTime) {

        log.info("[NoSqlController] listBackups - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        Page<CloudResource> result = noSqlUseCaseService.listBackups(
                provider, accountScope, tableName, fromTime, toTime);

        Page<NoSqlBackupResponse> responsePage = result.map(NoSqlBackupResponse::from);
        log.info("[NoSqlController] listBackups - success, totalElements={}", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(responsePage, "NoSQL 테이블 백업 목록 조회에 성공했습니다."));
    }

    /**
     * NoSQL 백업을 삭제합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param backupId 백업 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{tableName}/backups/{backupId}")
    @Operation(summary = "NoSQL 백업 삭제", description = "NoSQL 테이블의 백업을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "백업 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteBackup(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "백업 ID", required = true)
            @PathVariable String backupId) {

        log.info("[NoSqlController] deleteBackup - provider={}, accountScope={}, backupId={}",
                provider, accountScope, backupId);

        noSqlUseCaseService.deleteBackup(provider, accountScope, backupId);

        log.info("[NoSqlController] deleteBackup - success, backupId={}", backupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * NoSQL 백업에서 테이블을 복원합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 원본 테이블 이름
     * @param backupId 백업 ID
     * @param request 복원 요청 정보
     * @return 복원된 테이블 정보
     */
    @PostMapping("/{tableName}/backups/{backupId}/restore")
    @Operation(summary = "NoSQL 백업 복원", description = "NoSQL 백업에서 테이블을 복원합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "복원 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "백업 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlTableResponse>> restoreFromBackup(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "원본 테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "백업 ID", required = true)
            @PathVariable String backupId,
            @Parameter(description = "복원 요청 정보")
            @Valid @RequestBody NoSqlBackupRequest request) {

        log.info("[NoSqlController] restoreFromBackup - provider={}, accountScope={}, backupId={}, targetTable={}",
                provider, accountScope, backupId, request.getTargetTableName());

        CloudResource cloudResource = noSqlUseCaseService.restoreFromBackup(
                provider, accountScope, backupId, request.getTargetTableName());

        NoSqlTableResponse response = NoSqlTableResponse.from(cloudResource);
        log.info("[NoSqlController] restoreFromBackup - success, targetTable={}", request.getTargetTableName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "NoSQL 백업 복원에 성공했습니다."));
    }

    // ==================== 태그 관리 ====================

    /**
     * NoSQL 테이블의 태그를 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @return 태그 맵
     */
    @GetMapping("/{tableName}/tags")
    @Operation(summary = "NoSQL 테이블 태그 조회", description = "NoSQL 테이블의 모든 태그를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> getTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "리전") @RequestParam(required = false) String region) {

        log.info("[NoSqlController] getTags - provider={}, accountScope={}, tableName={}",
                provider, accountScope, tableName);

        Map<String, String> tags = noSqlUseCaseService.getTags(provider, accountScope, tableName, region);

        log.info("[NoSqlController] getTags - success, tagCount={}", tags.size());
        return ResponseEntity.ok(ApiResponse.success(tags, "NoSQL 테이블 태그 조회에 성공했습니다."));
    }

    /**
     * NoSQL 테이블에 태그를 추가하거나 업데이트합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 태그 요청 정보
     * @return 성공 응답
     */
    @PostMapping("/{tableName}/tags")
    @Operation(summary = "NoSQL 테이블 태그 추가", description = "NoSQL 테이블에 태그를 추가하거나 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "태그 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> putTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "태그 요청 정보")
            @Valid @RequestBody NoSqlTagRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setTableName(tableName);

        log.info("[NoSqlController] putTags - provider={}, accountScope={}, tableName={}, tagCount={}",
                provider, accountScope, tableName, request.getTags() != null ? request.getTags().size() : 0);

        noSqlUseCaseService.putTags(provider, accountScope, tableName, request.getRegion(), request.getTags());

        log.info("[NoSqlController] putTags - success, tableName={}", tableName);
        return ResponseEntity.ok(ApiResponse.success(null, "NoSQL 테이블 태그 추가에 성공했습니다."));
    }

    /**
     * NoSQL 테이블에서 태그를 제거합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 태그 제거 요청 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{tableName}/tags")
    @Operation(summary = "NoSQL 테이블 태그 제거", description = "NoSQL 테이블에서 태그를 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "태그 제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "태그 제거 요청 정보")
            @Valid @RequestBody NoSqlTagRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setTableName(tableName);

        log.info("[NoSqlController] removeTags - provider={}, accountScope={}, tableName={}, tagKeyCount={}",
                provider, accountScope, tableName,
                request.getTagKeysToRemove() != null ? request.getTagKeysToRemove().size() : 0);

        noSqlUseCaseService.removeTags(provider, accountScope, tableName, request.getRegion(),
                request.getTagKeysToRemove());

        log.info("[NoSqlController] removeTags - success, tableName={}", tableName);
        return ResponseEntity.ok(ApiResponse.success(null, "NoSQL 테이블 태그 제거에 성공했습니다."));
    }

    // ==================== 메트릭 조회 ====================

    /**
     * NoSQL 테이블의 메트릭을 조회합니다.
     *
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param request 메트릭 조회 요청 정보
     * @return 메트릭 응답
     */
    @PostMapping("/{tableName}/metrics")
    @Operation(summary = "NoSQL 테이블 메트릭 조회", description = "NoSQL 테이블의 성능 메트릭을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "테이블 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NoSqlMetricResponse>> queryMetrics(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "테이블 이름", required = true)
            @PathVariable String tableName,
            @Parameter(description = "메트릭 조회 요청 정보")
            @Valid @RequestBody NoSqlMetricQueryRequest request) {

        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setTableName(tableName);

        log.info("[NoSqlController] queryMetrics - provider={}, accountScope={}, tableName={}, metricTypes={}",
                provider, accountScope, tableName, request.getMetricTypes());

        // MetricType 변환
        List<NoSqlMetricQuery.MetricType> metricTypes = request.getMetricTypes().stream()
                .map(type -> NoSqlMetricQuery.MetricType.valueOf(type.name()))
                .collect(Collectors.toList());

        Map<String, Object> metricsMap = noSqlUseCaseService.queryMetrics(
                provider,
                accountScope,
                tableName,
                request.getRegion(),
                metricTypes,
                request.getStartTime(),
                request.getEndTime(),
                request.getPeriod()
        );

        NoSqlMetricResponse response = NoSqlMetricResponse.from(
                metricsMap,
                provider,
                accountScope,
                tableName,
                request.getRegion(),
                request.getStartTime(),
                request.getEndTime()
        );

        log.info("[NoSqlController] queryMetrics - success, metricCount={}", metricsMap.size());
        return ResponseEntity.ok(ApiResponse.success(response, "NoSQL 테이블 메트릭 조회에 성공했습니다."));
    }
}

