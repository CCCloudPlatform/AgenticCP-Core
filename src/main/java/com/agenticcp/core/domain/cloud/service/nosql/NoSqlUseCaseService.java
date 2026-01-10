package com.agenticcp.core.domain.cloud.service.nosql;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.nosql.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NoSQL 유스케이스 서비스
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 NoSQL 테이블 관련 비즈니스 로직을 처리합니다.
 * 포트 인터페이스를 통해서만 외부 시스템과 통신하며, 트랜잭션을 담당합니다.
 *
 * JIT 세션 관리 패턴을 따릅니다:
 * - 모든 작업에서 Service 레벨에서 세션을 획득하여 Port에 전달
 * - getSession()을 통한 Redis 캐싱 활용
 *
 * 지원 기능:
 * - 테이블 생성/수정/삭제
 * - 인덱스 관리 (GSI)
 * - 스트림 관리
 * - 백업/복원
 * - 태그 관리
 * - 메트릭 조회
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoSqlUseCaseService {

    private final NoSqlPortRouter portRouter;
    private final AccountCredentialManagementPort credentialProviderPort;

    // ==================== 세션 관리 ====================

    /**
     * 세션 자격증명을 획득합니다.
     * JIT 세션 관리 패턴을 따릅니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @return CloudSessionCredential 세션 자격증명
     */
    private CloudSessionCredential getSession(ProviderType providerType, String accountScope) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        return credentialProviderPort.getSession(tenantKey, accountScope, providerType);
    }

    // ==================== 테이블 조회 ====================

    /**
     * NoSQL 테이블 목록을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param query 조회 조건
     * @return CloudResource 페이지
     */
    public Page<CloudResource> listTables(ProviderType providerType, String accountScope, ResourceQuery query) {
        log.info("[NoSqlUseCaseService] listTables - provider={}, accountScope={}", providerType, accountScope);

        Page<CloudResource> result = portRouter.discovery(providerType).listTables(query);

        log.info("[NoSqlUseCaseService] listTables - success, totalElements={}", result.getTotalElements());
        return result;
    }

    /**
     * 특정 NoSQL 테이블을 조회합니다.
     *
     * @param providerType 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    public Optional<CloudResource> getTable(ProviderType providerType, String accountScope, String tableName) {
        log.info("[NoSqlUseCaseService] getTable - provider={}, tableName={}", providerType, tableName);

        ResourceIdentity identity = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .providerResourceId(tableName)
                .build();

        Optional<CloudResource> result = portRouter.discovery(providerType).getTable(identity);

        log.info("[NoSqlUseCaseService] getTable - success, found={}", result.isPresent());
        return result;
    }

    // ==================== 테이블 관리 ====================

    /**
     * NoSQL 테이블을 생성합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param partitionKeyName 파티션 키 이름
     * @param sortKeyName 정렬 키 이름 (선택)
     * @param keyTypes 키 타입 맵
     * @param billingMode 빌링 모드
     * @param readCapacityUnits RCU (프로비저닝 모드)
     * @param writeCapacityUnits WCU (프로비저닝 모드)
     * @param tags 태그
     * @return 생성된 테이블 CloudResource
     */
    @Transactional
    public CloudResource createTable(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            String partitionKeyName,
            String sortKeyName,
            Map<String, String> keyTypes,
            NoSqlCreateTableCommand.BillingMode billingMode,
            Long readCapacityUnits,
            Long writeCapacityUnits,
            Map<String, String> tags
    ) {
        log.info("[NoSqlUseCaseService] createTable - provider={}, tableName={}, region={}",
                providerType, tableName, region);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlCreateTableCommand command = NoSqlCreateTableCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .partitionKeyName(partitionKeyName)
                .sortKeyName(sortKeyName)
                .keyTypes(keyTypes)
                .billingMode(billingMode)
                .readCapacityUnits(readCapacityUnits)
                .writeCapacityUnits(writeCapacityUnits)
                .tags(tags)
                .session(session)
                .build();

        CloudResource result = portRouter.tableManagement(providerType).createTable(command);

        log.info("[NoSqlUseCaseService] createTable - success, resourceId={}", result.getResourceId());
        return result;
    }

    /**
     * NoSQL 테이블 구성을 수정합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param billingMode 빌링 모드 (선택)
     * @param readCapacityUnits RCU (선택)
     * @param writeCapacityUnits WCU (선택)
     * @return 수정된 테이블 CloudResource
     */
    @Transactional
    public CloudResource updateTable(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            NoSqlCreateTableCommand.BillingMode billingMode,
            Long readCapacityUnits,
            Long writeCapacityUnits
    ) {
        log.info("[NoSqlUseCaseService] updateTable - provider={}, tableName={}", providerType, tableName);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlUpdateTableCommand command = NoSqlUpdateTableCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .billingMode(billingMode)
                .readCapacityUnits(readCapacityUnits)
                .writeCapacityUnits(writeCapacityUnits)
                .session(session)
                .build();

        CloudResource result = portRouter.tableManagement(providerType).updateTable(command);

        log.info("[NoSqlUseCaseService] updateTable - success, resourceId={}", result.getResourceId());
        return result;
    }

    /**
     * NoSQL 테이블을 삭제합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param force 강제 삭제 여부
     * @param reason 삭제 사유
     */
    @Transactional
    public void deleteTable(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            boolean force,
            String reason
    ) {
        log.info("[NoSqlUseCaseService] deleteTable - provider={}, tableName={}, force={}",
                providerType, tableName, force);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlDeleteTableCommand command = NoSqlDeleteTableCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .force(force)
                .reason(reason)
                .session(session)
                .build();

        portRouter.tableManagement(providerType).deleteTable(command);

        log.info("[NoSqlUseCaseService] deleteTable - success, tableName={}", tableName);
    }

    // ==================== 인덱스 관리 ====================

    /**
     * NoSQL 테이블의 GSI를 관리합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param globalSecondaryIndexes GSI 정의 목록
     * @return 수정된 테이블 CloudResource
     */
    @Transactional
    public CloudResource updateIndexes(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            List<Map<String, Object>> globalSecondaryIndexes
    ) {
        log.info("[NoSqlUseCaseService] updateIndexes - provider={}, tableName={}, indexCount={}",
                providerType, tableName, globalSecondaryIndexes != null ? globalSecondaryIndexes.size() : 0);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlUpdateTableCommand command = NoSqlUpdateTableCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .globalSecondaryIndexes(globalSecondaryIndexes)
                .session(session)
                .build();

        CloudResource result = portRouter.indexManagement(providerType).updateIndexes(command);

        log.info("[NoSqlUseCaseService] updateIndexes - success, resourceId={}", result.getResourceId());
        return result;
    }

    // ==================== 스트림 관리 ====================

    /**
     * NoSQL 테이블의 스트림 구성을 변경합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param streamEnabled 스트림 활성화 여부
     * @param streamViewType 스트림 뷰 타입
     * @return 수정된 테이블 CloudResource
     */
    @Transactional
    public CloudResource updateStreamConfiguration(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            boolean streamEnabled,
            String streamViewType
    ) {
        log.info("[NoSqlUseCaseService] updateStreamConfiguration - provider={}, tableName={}, enabled={}",
                providerType, tableName, streamEnabled);

        CloudSessionCredential session = getSession(providerType, accountScope);

        Map<String, Object> streamOptions = Map.of(
                "enabled", streamEnabled,
                "viewType", streamViewType != null ? streamViewType : "NEW_AND_OLD_IMAGES"
        );

        NoSqlUpdateTableCommand command = NoSqlUpdateTableCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .streamOptions(streamOptions)
                .session(session)
                .build();

        CloudResource result = portRouter.streamManagement(providerType).updateStreamConfiguration(command);

        log.info("[NoSqlUseCaseService] updateStreamConfiguration - success, resourceId={}", result.getResourceId());
        return result;
    }

    // ==================== 백업 관리 ====================

    /**
     * NoSQL 테이블의 온디맨드 백업을 생성합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param backupName 백업 이름 (선택)
     * @return 생성된 백업 CloudResource
     */
    @Transactional
    public CloudResource createBackup(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String backupName
    ) {
        log.info("[NoSqlUseCaseService] createBackup - provider={}, tableName={}", providerType, tableName);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlBackupCommand command = NoSqlBackupCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .backupId(backupName)
                .operationType(NoSqlBackupCommand.OperationType.CREATE_BACKUP)
                .session(session)
                .build();

        CloudResource result = portRouter.backup(providerType).createBackup(command);

        log.info("[NoSqlUseCaseService] createBackup - success, backupId={}", result.getResourceId());
        return result;
    }

    /**
     * NoSQL 테이블의 백업 목록을 조회합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름 (선택)
     * @param fromTime 시작 시간 (선택)
     * @param toTime 종료 시간 (선택)
     * @return 백업 목록 페이지
     */
    public Page<CloudResource> listBackups(
            ProviderType providerType,
            String accountScope,
            String tableName,
            Instant fromTime,
            Instant toTime
    ) {
        log.info("[NoSqlUseCaseService] listBackups - provider={}, tableName={}", providerType, tableName);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlBackupCommand command = NoSqlBackupCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .fromTime(fromTime)
                .toTime(toTime)
                .operationType(NoSqlBackupCommand.OperationType.LIST_BACKUPS)
                .session(session)
                .build();

        Page<CloudResource> result = portRouter.backup(providerType).listBackups(command);

        log.info("[NoSqlUseCaseService] listBackups - success, totalElements={}", result.getTotalElements());
        return result;
    }

    /**
     * NoSQL 백업을 삭제합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param backupArn 백업 ARN
     */
    @Transactional
    public void deleteBackup(
            ProviderType providerType,
            String accountScope,
            String backupArn
    ) {
        log.info("[NoSqlUseCaseService] deleteBackup - provider={}, backupArn={}", providerType, backupArn);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlBackupCommand command = NoSqlBackupCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .backupId(backupArn)
                .operationType(NoSqlBackupCommand.OperationType.DELETE_BACKUP)
                .session(session)
                .build();

        portRouter.backup(providerType).deleteBackup(command);

        log.info("[NoSqlUseCaseService] deleteBackup - success, backupArn={}", backupArn);
    }

    /**
     * NoSQL 백업에서 테이블을 복원합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param backupArn 백업 ARN
     * @param targetTableName 복원 대상 테이블 이름
     * @return 복원된 테이블 CloudResource
     */
    @Transactional
    public CloudResource restoreFromBackup(
            ProviderType providerType,
            String accountScope,
            String backupArn,
            String targetTableName
    ) {
        log.info("[NoSqlUseCaseService] restoreFromBackup - provider={}, backupArn={}, targetTable={}",
                providerType, backupArn, targetTableName);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlBackupCommand command = NoSqlBackupCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .backupId(backupArn)
                .targetTableName(targetTableName)
                .operationType(NoSqlBackupCommand.OperationType.RESTORE_FROM_BACKUP)
                .session(session)
                .build();

        CloudResource result = portRouter.backup(providerType).restore(command);

        log.info("[NoSqlUseCaseService] restoreFromBackup - success, resourceId={}", result.getResourceId());
        return result;
    }

    /**
     * PITR(Point-In-Time Recovery)을 사용하여 특정 시점으로 테이블을 복원합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param sourceTableName 원본 테이블 이름
     * @param targetTableName 복원 대상 테이블 이름
     * @param restorePointInTime 복원 시점 (null이면 최신 시점)
     * @return 복원된 테이블 CloudResource
     */
    @Transactional
    public CloudResource restoreToPointInTime(
            ProviderType providerType,
            String accountScope,
            String sourceTableName,
            String targetTableName,
            Instant restorePointInTime
    ) {
        log.info("[NoSqlUseCaseService] restoreToPointInTime - provider={}, sourceTable={}, targetTable={}",
                providerType, sourceTableName, targetTableName);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlBackupCommand command = NoSqlBackupCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(sourceTableName)
                .targetTableName(targetTableName)
                .restorePointInTime(restorePointInTime)
                .operationType(NoSqlBackupCommand.OperationType.RESTORE_TO_POINT_IN_TIME)
                .session(session)
                .build();

        CloudResource result = portRouter.backup(providerType).restore(command);

        log.info("[NoSqlUseCaseService] restoreToPointInTime - success, resourceId={}", result.getResourceId());
        return result;
    }

    // ==================== 태그 관리 ====================

    /**
     * NoSQL 테이블에 태그를 추가하거나 업데이트합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param tags 태그 맵
     */
    @Transactional
    public void putTags(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            Map<String, String> tags
    ) {
        log.info("[NoSqlUseCaseService] putTags - provider={}, tableName={}, tagCount={}",
                providerType, tableName, tags != null ? tags.size() : 0);

        CloudSessionCredential session = getSession(providerType, accountScope);

        NoSqlTagCommand command = NoSqlTagCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .tags(tags)
                .operationType(NoSqlTagCommand.OperationType.ADD_OR_UPDATE)
                .session(session)
                .build();

        portRouter.tagging(providerType).putTags(command);

        log.info("[NoSqlUseCaseService] putTags - success, tableName={}", tableName);
    }

    /**
     * NoSQL 테이블에서 태그를 제거합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param tagKeys 제거할 태그 키 목록
     */
    @Transactional
    public void removeTags(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            List<String> tagKeys
    ) {
        log.info("[NoSqlUseCaseService] removeTags - provider={}, tableName={}, tagKeyCount={}",
                providerType, tableName, tagKeys != null ? tagKeys.size() : 0);

        CloudSessionCredential session = getSession(providerType, accountScope);

        // 태그 키만 사용 (값은 무시됨)
        Map<String, String> tagMap = new java.util.HashMap<>();
        if (tagKeys != null) {
            tagKeys.forEach(key -> tagMap.put(key, ""));
        }

        NoSqlTagCommand command = NoSqlTagCommand.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .tags(tagMap)
                .operationType(NoSqlTagCommand.OperationType.REMOVE)
                .session(session)
                .build();

        portRouter.tagging(providerType).removeTags(command);

        log.info("[NoSqlUseCaseService] removeTags - success, tableName={}", tableName);
    }

    /**
     * NoSQL 테이블의 태그를 조회합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @return 태그 맵
     */
    public Map<String, String> getTags(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region
    ) {
        log.info("[NoSqlUseCaseService] getTags - provider={}, tableName={}", providerType, tableName);

        Map<String, String> result = portRouter.tagging(providerType).getTags(
                providerType, accountScope, tableName, region);

        log.info("[NoSqlUseCaseService] getTags - success, tagCount={}", result.size());
        return result;
    }

    // ==================== 메트릭 조회 ====================

    /**
     * NoSQL 테이블의 메트릭을 조회합니다.
     *
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param tableName 테이블 이름
     * @param region 리전
     * @param metricTypes 조회할 메트릭 타입 목록
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param period 샘플 간격
     * @return 메트릭 결과 맵
     */
    public Map<String, Object> queryMetrics(
            ProviderType providerType,
            String accountScope,
            String tableName,
            String region,
            List<NoSqlMetricQuery.MetricType> metricTypes,
            Instant startTime,
            Instant endTime,
            Duration period
    ) {
        log.info("[NoSqlUseCaseService] queryMetrics - provider={}, tableName={}", providerType, tableName);

        NoSqlMetricQuery query = NoSqlMetricQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .metricTypes(metricTypes)
                .startTime(startTime)
                .endTime(endTime)
                .period(period)
                .build();

        Map<String, Object> result = portRouter.monitoring(providerType).queryMetrics(query);

        log.info("[NoSqlUseCaseService] queryMetrics - success, metricCount={}", result.size());
        return result;
    }
}

