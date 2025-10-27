package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.Ec2CreateRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2DeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2Query;
import com.agenticcp.core.domain.cloud.port.model.Ec2UpdateRequest;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Optional;

/**
 * EC2 인스턴스 관리 포트 인터페이스
 * 
 * 이 포트는 EC2 인스턴스의 모든 관리 작업을 정의합니다.
 * AWS, Azure, GCP 등 다양한 클라우드 제공업체에서 동일한 인터페이스로 구현됩니다.
 * 
 * 헥사고날 아키텍처의 핵심인 포트(계약)로, 비즈니스 로직과 외부 시스템을 분리합니다.
 */
public interface Ec2ManagementPort {
    
    // ==================== 인스턴스 조회 ====================
    
    /**
     * EC2 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건 (필터링, 페이징 포함)
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     * @throws com.agenticcp.core.common.exception.BusinessException 조회 권한 없음, 잘못된 쿼리 조건
     */
    Page<CloudResource> listInstances(Ec2Query query);
    
    /**
     * 특정 EC2 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID (null 불가)
     * @return CloudResource (존재하지 않으면 Optional.empty())
     * @throws com.agenticcp.core.common.exception.BusinessException 잘못된 인스턴스 ID 형식
     */
    Optional<CloudResource> getInstance(String instanceId);
    
    // ==================== 인스턴스 생명주기 ====================
    
    /**
     * 새로운 EC2 인스턴스를 생성합니다.
     * 
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     * @throws com.agenticcp.core.common.exception.BusinessException 생성 권한 없음, 잘못된 요청 정보
     */
    String createInstance(Ec2CreateRequest request);
    
    /**
     * 중지된 EC2 인스턴스를 시작합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 시작 권한 없음
     */
    void startInstance(String instanceId);
    
    /**
     * 실행 중인 EC2 인스턴스를 중지합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 중지 권한 없음
     */
    void stopInstance(String instanceId);
    
    /**
     * EC2 인스턴스를 재부팅합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 재부팅 권한 없음
     */
    void rebootInstance(String instanceId);
    
    /**
     * EC2 인스턴스를 종료합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 종료 권한 없음
     */
    void terminateInstance(String instanceId);
    
    /**
     * EC2 인스턴스를 삭제합니다 (삭제 요청 정보 포함).
     * 
     * @param request 삭제 요청 정보 (강제 삭제, 스냅샷 생성 등)
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 삭제 권한 없음
     */
    void deleteInstance(Ec2DeleteRequest request);
    
    // ==================== 인스턴스 수정 ====================
    
    /**
     * EC2 인스턴스 정보를 수정합니다.
     * 
     * @param request 수정 요청 정보
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 수정 권한 없음
     */
    void updateInstance(Ec2UpdateRequest request);
    
    // ==================== 태그 관리 ====================
    
    /**
     * EC2 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그 (키-값 쌍)
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 태그 권한 없음
     */
    void addTags(String instanceId, Map<String, String> tags);
    
    /**
     * EC2 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키 목록
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 태그 권한 없음
     */
    void removeTags(String instanceId, Map<String, String> tagKeys);
    
    /**
     * EC2 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 태그 맵 (빈 맵 가능, null 반환 금지)
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 조회 권한 없음
     */
    Map<String, String> getTags(String instanceId);
    
    // ==================== 상태 확인 ====================
    
    /**
     * EC2 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태 (예: "running", "stopped", "pending")
     * @throws com.agenticcp.core.common.exception.BusinessException 인스턴스 없음, 조회 권한 없음
     */
    String getInstanceStatus(String instanceId);
    
    /**
     * EC2 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     * @throws com.agenticcp.core.common.exception.BusinessException 타임아웃, 인스턴스 없음
     */
    boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds);
}
