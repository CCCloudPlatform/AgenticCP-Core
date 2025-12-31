package com.agenticcp.core.domain.cloud.port.model.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * RDBMS 조회 쿼리 (CSP 중립적)
 * 
 * RDBMS 인스턴스 목록 조회 시 사용하는 조회 조건을 정의하는 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter에서 CSP별 필터링 로직으로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class RdbmsQuery {
    
    /**
     * 프로바이더 타입
     */
    CloudProvider.ProviderType providerType;
    
    /**
     * 계정 범위 (AccountId, SubscriptionId, ProjectId)
     */
    String accountScope;
    
    /**
     * 조회할 리전 목록 (null이면 모든 리전)
     */
    Set<String> regions;
    
    /**
     * 인스턴스 이름으로 필터링 (CSP 중립적)
     */
    String instanceName;
    
    /**
     * 엔진 타입으로 필터링 (mysql, postgresql, mariadb, oracle, sqlserver)
     */
    String engine;
    
    /**
     * 인스턴스 크기로 필터링 (CSP 중립적)
     */
    String instanceSize;
    
    /**
     * 상태로 필터링 (CSP별 상태 값 다를 수 있음)
     */
    String status;
    
    /**
     * 태그로 필터링 (키-값 쌍)
     */
    Map<String, String> tagsEquals;
    
    /**
     * 페이징 - 페이지 번호 (0부터 시작)
     */
    @Builder.Default
    int page = 0;
    
    /**
     * 페이징 - 페이지 크기
     */
    @Builder.Default
    int size = 20;
    
    /**
     * 모든 RDBMS 인스턴스 조회를 위한 기본 쿼리 생성
     */
    public static RdbmsQuery all(CloudProvider.ProviderType providerType, String accountScope) {
        return RdbmsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .build();
    }
    
    /**
     * 특정 인스턴스 이름으로 조회하는 쿼리 생성
     */
    public static RdbmsQuery byInstanceName(CloudProvider.ProviderType providerType, String accountScope, String instanceName) {
        return RdbmsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .instanceName(instanceName)
                .build();
    }
    
    /**
     * 특정 엔진 타입으로 조회하는 쿼리 생성
     */
    public static RdbmsQuery byEngine(CloudProvider.ProviderType providerType, String accountScope, String engine) {
        return RdbmsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .engine(engine)
                .build();
    }
    
    /**
     * 특정 상태로 조회하는 쿼리 생성
     */
    public static RdbmsQuery byStatus(CloudProvider.ProviderType providerType, String accountScope, String status) {
        return RdbmsQuery.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .status(status)
                .build();
    }
}
