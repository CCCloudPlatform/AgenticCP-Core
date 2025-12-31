package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationHierarchyResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationPathResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationStatsResponse;
import com.agenticcp.core.domain.organization.dto.MoveOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.UserResponse;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 조직 관리 서비스
 * 
 * <p>조직의 생성, 조회, 수정, 삭제 및 계층 구조 관리를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberService organizationMemberService;
    
    /**
     * 조직 생성
     * 
     * @param request 조직 생성 요청 정보
     * @return 생성된 조직 정보
     * @throws BusinessException 조직명이 중복되거나 상위 조직을 찾을 수 없는 경우
     */
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("[OrganizationService] createOrganization - name={}", request.getOrgName());
        
        // 조직명 중복 검사 (설계 B: name 필드만 사용)
        validateOrgNameUnique(request.getOrgName());
        
        // 조직 생성 (설계 B: name 필드만 사용)
        Organization organization = Organization.builder()
                .name(request.getOrgName())
                .build();
        
        Organization savedOrganization = organizationRepository.save(organization);
        log.info("[OrganizationService] createOrganization - success id={}, name={}", 
                savedOrganization.getId(), savedOrganization.getName());
        
        return OrganizationResponse.from(savedOrganization);
    }
    
    /**
     * 조직 조회 (단일)
     * 
     * @param id 조직 ID
     * @return 조직 정보
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public OrganizationResponse getOrganization(Long id) {
        log.info("[OrganizationService] getOrganization - id={}", id);
        
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + id));
        
        log.info("[OrganizationService] getOrganization - success id={}", id);
        return OrganizationResponse.from(organization);
    }
    
    /**
     * 조직 목록 조회
     * 
     * @return 조직 목록
     */
    public List<OrganizationResponse> getOrganizations() {
        log.info("[OrganizationService] getOrganizations");
        
        List<Organization> organizations = organizationRepository.findAll();
        List<OrganizationResponse> result = organizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
        
        log.info("[OrganizationService] getOrganizations - success count={}", result.size());
        return result;
    }
    
    /**
     * 조직 수정
     * 
     * @param id 조직 ID
     * @param request 조직 수정 요청 정보
     * @return 수정된 조직 정보
     * @throws BusinessException 조직을 찾을 수 없거나 조직명이 중복되거나 상위 조직을 찾을 수 없는 경우
     */
    @Transactional
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request) {
        log.info("[OrganizationService] updateOrganization - id={}, name={}", id, request.getOrgName());
        
        // 조직 존재 여부 확인
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + id));
        
        // 조직명 중복 검사 (자신 제외) - 설계 B: name 필드만 사용
        if (!organization.getName().equals(request.getOrgName())) {
            validateOrgNameUnique(request.getOrgName());
        }
        
        // 조직 정보 수정 (설계 B: name 필드만 사용)
        organization.setName(request.getOrgName());
        
        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("[OrganizationService] updateOrganization - success id={}, name={}", 
                updatedOrganization.getId(), updatedOrganization.getName());
        
        return OrganizationResponse.from(updatedOrganization);
    }
    
    /**
     * 조직 삭제
     * 
     * @param id 조직 ID
     * @throws BusinessException 조직을 찾을 수 없거나 하위 조직이 존재하는 경우
     */
    @Transactional
    public void deleteOrganization(Long id) {
        log.info("[OrganizationService] deleteOrganization - id={}", id);
        
        // 조직 존재 여부 확인
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + id));
        
        // 설계 B: Organization에 계층 구조가 없으므로 하위 조직 확인 불필요
        
        // 조직 삭제
        organizationRepository.delete(organization);
        log.info("[OrganizationService] deleteOrganization - success id={}, name={}", 
                id, organization.getName());
    }
    
    /**
     * 조직명 중복 검증 (설계 B: name 필드만 사용)
     * 
     * @param name 조직명
     * @throws BusinessException 조직명이 중복되는 경우
     */
    private void validateOrgNameUnique(String name) {
        // 설계 B: name 필드 기반으로 중복 검사
        // Repository에 existsByName 메서드가 필요하거나, findAll로 확인
        List<Organization> existing = organizationRepository.findAll();
        boolean exists = existing.stream()
                .anyMatch(org -> name.equals(org.getName()));
        if (exists) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "이미 존재하는 조직명입니다: " + name);
        }
    }
    
    /**
     * 조직 수 조회
     * 
     * @return 조직 수
     */
    public long getOrganizationCount() {
        log.info("[OrganizationService] getOrganizationCount");
        long count = organizationRepository.count();
        log.info("[OrganizationService] getOrganizationCount - success count={}", count);
        return count;
    }
    
    /**
     * 특정 조직의 하위 조직 목록 조회
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 빈 리스트 반환
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 목록 (항상 빈 리스트)
     */
    @Deprecated
    public List<OrganizationResponse> getChildOrganizations(Long parentOrgId) {
        log.warn("[OrganizationService] getChildOrganizations - Deprecated: 설계 B에는 계층 구조가 없습니다");
        return List.of();
    }
    
    /**
     * 전체 조직 트리 조회
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 단순 목록 반환
     * @return 조직 목록
     */
    @Deprecated
    public List<OrganizationHierarchyResponse> getOrganizationTree() {
        log.warn("[OrganizationService] getOrganizationTree - Deprecated: 설계 B에는 계층 구조가 없습니다. 단순 목록 반환");
        List<Organization> organizations = organizationRepository.findAll();
        return organizations.stream()
                .map(org -> {
                    OrganizationHierarchyResponse response = OrganizationHierarchyResponse.from(
                            OrganizationResponse.from(org), 0, org.getName());
                    response.setChildren(List.of());
                    response.setChildrenCount(0);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 조직 경로 조회
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 단일 조직만 반환
     * @param orgId 조직 ID
     * @return 조직 경로 정보 (단일 조직만 포함)
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    @Deprecated
    public OrganizationPathResponse getOrganizationPath(Long orgId) {
        log.warn("[OrganizationService] getOrganizationPath - Deprecated: 설계 B에는 계층 구조가 없습니다");
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> path = List.of(OrganizationResponse.from(organization));
        OrganizationPathResponse result = OrganizationPathResponse.from(path);
        log.info("[OrganizationService] getOrganizationPath - success orgId={}, path={}", 
                orgId, result.getFullPath());
        return result;
    }
    
    /**
     * 상위 조직 목록 조회
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 빈 리스트 반환
     * @param orgId 조직 ID
     * @return 상위 조직 목록 (항상 빈 리스트)
     */
    @Deprecated
    public List<OrganizationResponse> getAncestors(Long orgId) {
        log.warn("[OrganizationService] getAncestors - Deprecated: 설계 B에는 계층 구조가 없습니다");
        return List.of();
    }
    
    /**
     * 하위 조직 목록 조회 (모든 레벨)
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 빈 리스트 반환
     * @param orgId 조직 ID
     * @return 하위 조직 목록 (항상 빈 리스트)
     */
    @Deprecated
    public List<OrganizationResponse> getDescendants(Long orgId) {
        log.warn("[OrganizationService] getDescendants - Deprecated: 설계 B에는 계층 구조가 없습니다");
        return List.of();
    }
    
    /**
     * 조직 이동
     * 
     * @deprecated 설계 B: Organization에 계층 구조가 없으므로 동작하지 않음
     * @param orgId 조직 ID
     * @param request 조직 이동 요청 정보
     * @return 조직 정보 (변경 없음)
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    @Deprecated
    @Transactional
    public OrganizationResponse moveOrganization(Long orgId, MoveOrganizationRequest request) {
        log.warn("[OrganizationService] moveOrganization - Deprecated: 설계 B에는 계층 구조가 없습니다");
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        return OrganizationResponse.from(organization);
    }
    
    /**
     * 조직 통계 조회
     * 
     * @deprecated 설계 B: Organization에 status 필드가 없으므로 단순 통계만 반환
     * @return 조직 통계 정보
     */
    @Deprecated
    public OrganizationStatsResponse getOrganizationStats() {
        log.warn("[OrganizationService] getOrganizationStats - Deprecated: 설계 B에는 status 필드가 없습니다");
        List<Organization> organizations = organizationRepository.findAll();
        
        long totalOrganizations = organizations.size();
        
        OrganizationStatsResponse result = OrganizationStatsResponse.builder()
                .totalOrganizations(totalOrganizations)
                .activeOrganizations(totalOrganizations) // status 필드가 없으므로 모두 active로 간주
                .inactiveOrganizations(0L)
                .maxDepth(0) // 계층 구조 없음
                .levelStats(List.of())
                .build();
        
        log.info("[OrganizationService] getOrganizationStats - success total={}", totalOrganizations);
        return result;
    }
    
    // Helper methods - 설계 B에서는 계층 구조가 없으므로 사용되지 않음
    
    /**
     * 조직별 사용자 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return 조직에 속한 사용자 목록
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public List<UserResponse> getOrganizationUsers(Long organizationId) {
        log.info("[OrganizationService] getOrganizationUsers - organizationId={}", organizationId);
        
        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));
        
        // OrganizationMember를 통해 사용자 목록 조회
        List<OrganizationMember> members = organizationMemberService.getMembers(organizationId);
        List<UserResponse> result = members.stream()
            .map(member -> convertToUserResponse(member.getUser()))
            .collect(Collectors.toList());
        
        log.info("[OrganizationService] getOrganizationUsers - success organizationId={}, count={}", 
                organizationId, result.size());
        return result;
    }
    
    /**
     * 사용자를 조직에 추가
     * 
     * <p>설계 B: OrganizationMemberService를 통해 User-Organization 관계를 관리합니다.</p>
     * 
     * @param organizationId 조직 ID
     * @param request 사용자 추가 요청 정보
     * @return 추가된 사용자 정보
     * @throws BusinessException 조직 또는 사용자를 찾을 수 없거나 이미 해당 조직에 속한 사용자인 경우
     */
    @Transactional
    public UserResponse addUserToOrganization(Long organizationId, AddUserToOrganizationRequest request) {
        log.info("[OrganizationService] addUserToOrganization - organizationId={}, userId={}", 
                organizationId, request.getUserId());
        
        // OrganizationMemberService를 통해 멤버 추가
        OrganizationMember member = organizationMemberService.addMember(
                organizationId, request.getUserId(), null); // role은 선택적이므로 null 전달
        
        log.info("[OrganizationService] addUserToOrganization - success userId={}, organizationId={}", 
                request.getUserId(), organizationId);
        
        return convertToUserResponse(member.getUser());
    }
    
    /**
     * 사용자를 조직에서 제거
     * 
     * <p>설계 B: OrganizationMemberService를 통해 User-Organization 관계를 관리합니다.</p>
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @throws BusinessException 조직 또는 사용자를 찾을 수 없거나 사용자가 해당 조직에 속하지 않는 경우
     */
    @Transactional
    public void removeUserFromOrganization(Long organizationId, Long userId) {
        log.info("[OrganizationService] removeUserFromOrganization - organizationId={}, userId={}", 
                organizationId, userId);
        
        // OrganizationMemberService를 통해 멤버 제거
        organizationMemberService.removeMember(organizationId, userId);
        
        log.info("[OrganizationService] removeUserFromOrganization - success userId={}, organizationId={}", 
                userId, organizationId);
    }
    
    /**
     * User 엔티티를 UserResponse DTO로 변환
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .name(user.getName())
            .role(user.getRole())
            .status(user.getStatus())
            .lastLogin(user.getLastLogin())
            .department(user.getDepartment())
            .jobTitle(user.getJobTitle())
            .phoneNumber(user.getPhoneNumber())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    // ========== 조직-테넌트 관계 관리 (1:1) ==========

    /**
     * 조직의 테넌트 조회 (1:1 관계)
     * 
     * @param organizationId 조직 ID
     * @return 조직에 연결된 테넌트
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public Tenant getOrganizationTenant(Long organizationId) {
        log.info("[OrganizationService] getOrganizationTenant - organizationId={}", organizationId);

        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));

        // 조직의 테넌트 조회
        Tenant tenant = organizationRepository.findTenantByOrganizationId(organizationId)
            .orElse(null);

        log.info("[OrganizationService] getOrganizationTenant - success organizationId={}, hasTenant={}", 
                organizationId, tenant != null);
        return tenant;
    }

    /**
     * 조직에 테넌트가 존재하는지 확인
     * 
     * @param organizationId 조직 ID
     * @return 테넌트 존재 여부
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public boolean hasTenant(Long organizationId) {
        log.info("[OrganizationService] hasTenant - organizationId={}", organizationId);

        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));

        // 테넌트 존재 여부 확인
        boolean exists = organizationRepository.existsTenantByOrganizationId(organizationId);
        
        log.info("[OrganizationService] hasTenant - success organizationId={}, hasTenant={}", 
                organizationId, exists);
        return exists;
    }

    /**
     * 조직의 테넌트가 활성 상태인지 확인
     * 
     * @param organizationId 조직 ID
     * @return 활성 테넌트 여부
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public boolean hasActiveTenant(Long organizationId) {
        log.info("[OrganizationService] hasActiveTenant - organizationId={}", organizationId);

        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));

        // 테넌트 조회 및 활성 상태 확인
        Tenant tenant = organizationRepository.findTenantByOrganizationId(organizationId)
            .orElse(null);
        boolean isActive = tenant != null && tenant.getStatus() == Status.ACTIVE;
        
        log.info("[OrganizationService] hasActiveTenant - success organizationId={}, isActive={}", 
                organizationId, isActive);
        return isActive;
    }

    /**
     * 조직별 테넌트 정보 조회 (1:1)
     * 
     * @param organizationId 조직 ID
     * @return 조직의 테넌트 정보
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public Map<String, Object> getOrganizationTenantInfo(Long organizationId) {
        log.info("[OrganizationService] getOrganizationTenantInfo - organizationId={}", organizationId);

        // 조직 존재 확인
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));

        // 조직의 테넌트 조회
        Tenant tenant = organizationRepository.findTenantByOrganizationId(organizationId)
            .orElse(null);

        Map<String, Object> info = new HashMap<>();
        info.put("organizationId", organizationId);
        info.put("organizationName", organization.getName()); // 설계 B: name 필드 사용
        info.put("hasTenant", tenant != null);
        
        if (tenant != null) {
            info.put("tenantId", tenant.getId());
            info.put("tenantKey", tenant.getTenantKey());
            info.put("tenantName", tenant.getTenantName());
            info.put("tenantStatus", tenant.getStatus());
            info.put("maxUsers", tenant.getMaxUsers());
        }

        log.info("[OrganizationService] getOrganizationTenantInfo - success organizationId={}, hasTenant={}", 
                organizationId, tenant != null);
        return info;
    }
}