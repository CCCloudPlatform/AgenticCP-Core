package com.agenticcp.core.domain.cloud.service.nosql;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.nosql.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * NoSQL 포트 라우터
 *
 * 헥사고날 아키텍처의 애플리케이션 계층에서 클라우드 프로바이더 타입에 따라
 * 적절한 NoSQL 포트 구현체를 선택하는 라우터입니다.
 *
 * 지원 포트:
 * - NoSqlTableManagementPort: 테이블 생성/수정/삭제
 * - NoSqlIndexManagementPort: GSI 관리
 * - NoSqlStreamManagementPort: 스트림 관리
 * - NoSqlBackupPort: 백업/복원
 * - NoSqlTaggingPort: 태그 관리
 * - NoSqlMonitoringPort: 메트릭 조회
 * - NoSqlDiscoveryPort: 테이블 조회
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Component
public class NoSqlPortRouter {

    private final Map<ProviderType, NoSqlTableManagementPort> tableManagementPorts;
    private final Map<ProviderType, NoSqlIndexManagementPort> indexManagementPorts;
    private final Map<ProviderType, NoSqlStreamManagementPort> streamManagementPorts;
    private final Map<ProviderType, NoSqlBackupPort> backupPorts;
    private final Map<ProviderType, NoSqlTaggingPort> taggingPorts;
    private final Map<ProviderType, NoSqlMonitoringPort> monitoringPorts;
    private final Map<ProviderType, NoSqlDiscoveryPort> discoveryPorts;

    /**
     * 생성자 - Spring이 자동으로 어댑터들을 주입합니다.
     */
    public NoSqlPortRouter(
            List<NoSqlTableManagementPort> tableManagementPortList,
            List<NoSqlIndexManagementPort> indexManagementPortList,
            List<NoSqlStreamManagementPort> streamManagementPortList,
            List<NoSqlBackupPort> backupPortList,
            List<NoSqlTaggingPort> taggingPortList,
            List<NoSqlMonitoringPort> monitoringPortList,
            List<NoSqlDiscoveryPort> discoveryPortList
    ) {
        this.tableManagementPorts = buildPortMap(tableManagementPortList);
        this.indexManagementPorts = buildPortMap(indexManagementPortList);
        this.streamManagementPorts = buildPortMap(streamManagementPortList);
        this.backupPorts = buildPortMap(backupPortList);
        this.taggingPorts = buildPortMap(taggingPortList);
        this.monitoringPorts = buildPortMap(monitoringPortList);
        this.discoveryPorts = buildPortMap(discoveryPortList);

        log.info("[NoSqlPortRouter] NoSQL router initialized: " +
                        "tableManagement={}, indexManagement={}, streamManagement={}, " +
                        "backup={}, tagging={}, monitoring={}, discovery={}",
                tableManagementPorts.keySet(),
                indexManagementPorts.keySet(),
                streamManagementPorts.keySet(),
                backupPorts.keySet(),
                taggingPorts.keySet(),
                monitoringPorts.keySet(),
                discoveryPorts.keySet());
    }

    /**
     * 포트 리스트를 ProviderType 기반 맵으로 변환합니다.
     */
    private <T> Map<ProviderType, T> buildPortMap(List<T> ports) {
        Map<ProviderType, T> map = new EnumMap<>(ProviderType.class);
        ports.stream()
                .filter(port -> port instanceof ProviderScoped)
                .forEach(port -> {
                    ProviderType providerType = ((ProviderScoped) port).getProviderType();
                    map.put(providerType, port);
                    log.debug("[NoSqlPortRouter] Registered {} port for provider {}",
                            port.getClass().getSimpleName(), providerType);
                });
        return map;
    }

    /**
     * 테이블 관리 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlTableManagementPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlTableManagementPort tableManagement(ProviderType providerType) {
        return requirePort(tableManagementPorts, providerType, "TableManagement");
    }

    /**
     * 인덱스 관리 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlIndexManagementPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlIndexManagementPort indexManagement(ProviderType providerType) {
        return requirePort(indexManagementPorts, providerType, "IndexManagement");
    }

    /**
     * 스트림 관리 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlStreamManagementPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlStreamManagementPort streamManagement(ProviderType providerType) {
        return requirePort(streamManagementPorts, providerType, "StreamManagement");
    }

    /**
     * 백업 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlBackupPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlBackupPort backup(ProviderType providerType) {
        return requirePort(backupPorts, providerType, "Backup");
    }

    /**
     * 태그 관리 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlTaggingPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlTaggingPort tagging(ProviderType providerType) {
        return requirePort(taggingPorts, providerType, "Tagging");
    }

    /**
     * 모니터링 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlMonitoringPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlMonitoringPort monitoring(ProviderType providerType) {
        return requirePort(monitoringPorts, providerType, "Monitoring");
    }

    /**
     * 테이블 조회 포트를 반환합니다.
     *
     * @param providerType 프로바이더 타입
     * @return NoSqlDiscoveryPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public NoSqlDiscoveryPort discovery(ProviderType providerType) {
        return requirePort(discoveryPorts, providerType, "Discovery");
    }

    /**
     * 포트 맵에서 프로바이더에 해당하는 포트를 조회합니다.
     *
     * @param portMap 포트 맵
     * @param providerType 프로바이더 타입
     * @param portName 포트 이름 (로깅용)
     * @return 포트 인스턴스
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    private <T> T requirePort(Map<ProviderType, T> portMap, ProviderType providerType, String portName) {
        T port = portMap.get(providerType);
        if (port == null) {
            log.error("[NoSqlPortRouter] No {} port registered for provider: {}", portName, providerType);
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 프로바이더입니다: %s (NoSql%sPort)", providerType, portName));
        }
        return port;
    }

    /**
     * 특정 프로바이더의 테이블 관리 포트가 등록되어 있는지 확인합니다.
     */
    public boolean hasTableManagementPort(ProviderType providerType) {
        return tableManagementPorts.containsKey(providerType);
    }

    /**
     * 등록된 프로바이더 목록을 반환합니다. (테이블 관리 포트 기준)
     */
    public java.util.Set<ProviderType> getSupportedProviders() {
        return tableManagementPorts.keySet();
    }
}

