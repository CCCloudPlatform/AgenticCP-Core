package com.agenticcp.core.domain.cloud.adapter.outbound.aws.nosql;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDynamoDbConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlBackupCommand;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.NoSqlBackupPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS DynamoDB 백업 관리 어댑터
 * 
 * NoSqlBackupPort를 구현하여 DynamoDB 온디맨드 백업 및
 * PITR(Point-In-Time Recovery) 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDynamoDbBackupAdapter implements NoSqlBackupPort, ProviderScoped {

    private final AwsDynamoDbConfig dynamoDbConfig;
    private final AwsDynamoDbMapper mapper;
    private final AwsDynamoDbErrorTranslator errorTranslator;
    private final CloudProviderRepository cloudProviderRepository;

    /**
     * DynamoDB 테이블의 온디맨드 백업을 생성합니다.
     *
     * @param command 백업 생성 명령
     * @return 생성된 백업 정보를 담은 CloudResource
     * @throws BusinessException 백업 생성 실패 시
     */
    @Override
    public CloudResource createBackup(NoSqlBackupCommand command) {
        log.info("[AwsDynamoDbBackupAdapter] Creating backup for table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), null)) {
            String backupName = command.getBackupId() != null 
                    ? command.getBackupId() 
                    : command.getTableName() + "-backup-" + System.currentTimeMillis();

            CreateBackupRequest request = CreateBackupRequest.builder()
                    .tableName(command.getTableName())
                    .backupName(backupName)
                    .build();

            CreateBackupResponse response = client.createBackup(request);
            BackupDetails backupDetails = response.backupDetails();

            log.info("[AwsDynamoDbBackupAdapter] Successfully created backup: {} for table: {}",
                    backupDetails.backupArn(), command.getTableName());

            return buildBackupCloudResource(backupDetails);

        } catch (TableNotFoundException e) {
            log.error("[AwsDynamoDbBackupAdapter] Table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (BackupInUseException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup already in progress for table: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "이미 백업이 진행 중입니다.");
        } catch (LimitExceededException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup limit exceeded for table: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.RESOURCE_QUOTA_EXCEEDED,
                    "백업 개수 제한을 초과했습니다.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbBackupAdapter] Failed to create backup for table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 테이블의 백업 목록을 조회합니다.
     *
     * @param command 백업 조회 명령 (테이블명, 기간 필터 등)
     * @return 백업 목록 페이지
     */
    @Override
    public Page<CloudResource> listBackups(NoSqlBackupCommand command) {
        log.info("[AwsDynamoDbBackupAdapter] Listing backups for table: {}", command.getTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), null)) {
            ListBackupsRequest.Builder requestBuilder = ListBackupsRequest.builder();

            if (command.getTableName() != null) {
                requestBuilder.tableName(command.getTableName());
            }
            if (command.getFromTime() != null) {
                requestBuilder.timeRangeLowerBound(command.getFromTime());
            }
            if (command.getToTime() != null) {
                requestBuilder.timeRangeUpperBound(command.getToTime());
            }

            ListBackupsResponse response = client.listBackups(requestBuilder.build());

            List<CloudResource> backups = new ArrayList<>();
            for (BackupSummary summary : response.backupSummaries()) {
                backups.add(buildBackupSummaryCloudResource(summary));
            }

            log.info("[AwsDynamoDbBackupAdapter] Found {} backups for table: {}",
                    backups.size(), command.getTableName());

            return new PageImpl<>(backups, PageRequest.of(0, backups.size()), backups.size());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbBackupAdapter] Failed to list backups for table: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 백업을 삭제합니다.
     *
     * @param command 백업 삭제 명령 (backupId = 백업 ARN)
     * @throws BusinessException 삭제 실패 시
     * @throws ResourceNotFoundException 백업을 찾을 수 없을 때
     */
    @Override
    public void deleteBackup(NoSqlBackupCommand command) {
        log.info("[AwsDynamoDbBackupAdapter] Deleting backup: {}", command.getBackupId());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), null)) {
            DeleteBackupRequest request = DeleteBackupRequest.builder()
                    .backupArn(command.getBackupId())
                    .build();

            client.deleteBackup(request);

            log.info("[AwsDynamoDbBackupAdapter] Successfully deleted backup: {}", command.getBackupId());

        } catch (BackupNotFoundException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup not found: {}", command.getBackupId());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED);
        } catch (BackupInUseException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup is in use: {}", command.getBackupId());
            throw new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "백업이 사용 중입니다.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbBackupAdapter] Failed to delete backup: {}", command.getBackupId(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * DynamoDB 백업에서 테이블을 복원합니다.
     * 
     * operationType에 따라:
     * - RESTORE_FROM_BACKUP: 온디맨드 백업에서 복원
     * - RESTORE_TO_POINT_IN_TIME: PITR을 사용한 시점 복원
     *
     * @param command 복원 명령 (백업 ARN 또는 시점)
     * @return 복원된 테이블의 CloudResource
     * @throws BusinessException 복원 실패 시
     */
    @Override
    public CloudResource restore(NoSqlBackupCommand command) {
        if (command.getOperationType() == NoSqlBackupCommand.OperationType.RESTORE_TO_POINT_IN_TIME) {
            return restoreToPointInTime(command);
        }
        return restoreFromBackup(command);
    }

    /**
     * 온디맨드 백업에서 테이블을 복원합니다.
     */
    private CloudResource restoreFromBackup(NoSqlBackupCommand command) {
        log.info("[AwsDynamoDbBackupAdapter] Restoring table from backup: {} to {}",
                command.getBackupId(), command.getTargetTableName());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), null)) {
            String targetTableName = command.getTargetTableName() != null 
                    ? command.getTargetTableName() 
                    : command.getTableName() + "-restored-" + System.currentTimeMillis();

            RestoreTableFromBackupRequest request = RestoreTableFromBackupRequest.builder()
                    .backupArn(command.getBackupId())
                    .targetTableName(targetTableName)
                    .build();

            RestoreTableFromBackupResponse response = client.restoreTableFromBackup(request);
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbBackupAdapter] Successfully initiated restore to table: {}",
                    tableDescription.tableName());

            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (BackupNotFoundException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup not found: {}", command.getBackupId());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED);
        } catch (TableAlreadyExistsException e) {
            log.error("[AwsDynamoDbBackupAdapter] Target table already exists: {}", command.getTargetTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "대상 테이블이 이미 존재합니다.");
        } catch (BackupInUseException e) {
            log.error("[AwsDynamoDbBackupAdapter] Backup is in use: {}", command.getBackupId());
            throw new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "백업이 사용 중입니다.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbBackupAdapter] Failed to restore from backup: {}", command.getBackupId(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * PITR(Point-In-Time Recovery)을 사용하여 특정 시점으로 테이블을 복원합니다.
     */
    private CloudResource restoreToPointInTime(NoSqlBackupCommand command) {
        log.info("[AwsDynamoDbBackupAdapter] Restoring table {} to point in time: {}",
                command.getTableName(), command.getRestorePointInTime());

        try (DynamoDbClient client = dynamoDbConfig.createDynamoDbClient(command.getSession(), null)) {
            String targetTableName = command.getTargetTableName() != null 
                    ? command.getTargetTableName() 
                    : command.getTableName() + "-pitr-" + System.currentTimeMillis();

            RestoreTableToPointInTimeRequest.Builder requestBuilder = RestoreTableToPointInTimeRequest.builder()
                    .sourceTableName(command.getTableName())
                    .targetTableName(targetTableName);

            if (command.getRestorePointInTime() != null) {
                requestBuilder.restoreDateTime(command.getRestorePointInTime());
            } else {
                // 시점이 지정되지 않으면 최신 복원 가능 시점으로 복원
                requestBuilder.useLatestRestorableTime(true);
            }

            RestoreTableToPointInTimeResponse response = client.restoreTableToPointInTime(requestBuilder.build());
            TableDescription tableDescription = response.tableDescription();

            log.info("[AwsDynamoDbBackupAdapter] Successfully initiated PITR restore to table: {}",
                    tableDescription.tableName());

            return mapper.toCloudResource(tableDescription, findAwsProvider());

        } catch (TableNotFoundException e) {
            log.error("[AwsDynamoDbBackupAdapter] Source table not found: {}", command.getTableName());
            throw new ResourceNotFoundException(CloudErrorCode.NOSQL_TABLE_NOT_FOUND);
        } catch (PointInTimeRecoveryUnavailableException e) {
            log.error("[AwsDynamoDbBackupAdapter] PITR not enabled for table: {}", command.getTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "Point-in-Time Recovery가 활성화되지 않았습니다.");
        } catch (TableAlreadyExistsException e) {
            log.error("[AwsDynamoDbBackupAdapter] Target table already exists: {}", command.getTargetTableName());
            throw new BusinessException(CloudErrorCode.NOSQL_TABLE_ALREADY_EXISTS,
                    "대상 테이블이 이미 존재합니다.");
        } catch (InvalidRestoreTimeException e) {
            log.error("[AwsDynamoDbBackupAdapter] Invalid restore time: {}", command.getRestorePointInTime());
            throw new BusinessException(CloudErrorCode.NOSQL_BACKUP_OPERATION_FAILED,
                    "유효하지 않은 복원 시점입니다.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AwsDynamoDbBackupAdapter] Failed to restore to point in time: {}", command.getTableName(), e);
            throw errorTranslator.translate(e);
        }
    }

    /**
     * BackupDetails를 CloudResource로 변환합니다.
     */
    private CloudResource buildBackupCloudResource(BackupDetails backupDetails) {
        Map<String, String> tags = new HashMap<>();
        tags.put("backupStatus", backupDetails.backupStatusAsString());
        tags.put("backupType", backupDetails.backupTypeAsString());

        return CloudResource.builder()
                .resourceId(backupDetails.backupArn())
                .resourceName(backupDetails.backupName())
                .displayName(backupDetails.backupName())
                .provider(findAwsProvider())
                .resourceType(CloudResource.ResourceType.SNAPSHOT)
                .lifecycleState(mapBackupStatus(backupDetails.backupStatusAsString()))
                .instanceType("DYNAMODB_BACKUP")
                .storageGb(backupDetails.backupSizeBytes() != null 
                        ? backupDetails.backupSizeBytes() / (1024 * 1024 * 1024) : 0L)
                .tags(tags)
                .createdInCloud(backupDetails.backupCreationDateTime() != null 
                        ? LocalDateTime.ofInstant(backupDetails.backupCreationDateTime(), ZoneId.systemDefault()) 
                        : null)
                .lastSync(LocalDateTime.now())
                .build();
    }

    /**
     * BackupSummary를 CloudResource로 변환합니다.
     */
    private CloudResource buildBackupSummaryCloudResource(BackupSummary summary) {
        Map<String, String> tags = new HashMap<>();
        tags.put("backupStatus", summary.backupStatusAsString());
        tags.put("backupType", summary.backupTypeAsString());
        tags.put("tableName", summary.tableName());

        return CloudResource.builder()
                .resourceId(summary.backupArn())
                .resourceName(summary.backupName())
                .displayName(summary.backupName())
                .provider(findAwsProvider())
                .resourceType(CloudResource.ResourceType.SNAPSHOT)
                .lifecycleState(mapBackupStatus(summary.backupStatusAsString()))
                .instanceType("DYNAMODB_BACKUP")
                .storageGb(summary.backupSizeBytes() != null 
                        ? summary.backupSizeBytes() / (1024 * 1024 * 1024) : 0L)
                .tags(tags)
                .createdInCloud(summary.backupCreationDateTime() != null 
                        ? LocalDateTime.ofInstant(summary.backupCreationDateTime(), ZoneId.systemDefault()) 
                        : null)
                .lastSync(LocalDateTime.now())
                .build();
    }

    /**
     * 백업 상태를 LifecycleState로 매핑합니다.
     */
    private CloudResource.LifecycleState mapBackupStatus(String status) {
        if (status == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }

        return switch (status.toUpperCase()) {
            case "AVAILABLE" -> CloudResource.LifecycleState.RUNNING;
            case "CREATING" -> CloudResource.LifecycleState.PENDING;
            case "DELETED" -> CloudResource.LifecycleState.TERMINATED;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    private CloudProvider findAwsProvider() {
        return cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)
                .orElseThrow(() -> new ResourceNotFoundException(CloudErrorCode.CLOUD_PROVIDER_NOT_FOUND));
    }
}

