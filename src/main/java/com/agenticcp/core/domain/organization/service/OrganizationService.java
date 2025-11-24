package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
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
import java.util.Optional;
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
    
    /**
     * 조직 생성
     * 
     * @param request 조직 생성 요청 정보
     * @return 생성된 조직 정보
     * @throws BusinessException 조직명이 중복되거나 상위 조직을 찾을 수 없는 경우
     */
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("[OrganizationService] createOrganization - orgName={}", request.getOrgName());
        
        // 조직명 중복 검사
        validateOrgNameUnique(request.getOrgName());
        
        // 조직 키 생성 (orgName 기반)
        String orgKey = generateOrgKey(request.getOrgName());
        
        // 조직 생성
        Organization organization = Organization.builder()
                .orgKey(orgKey)
                .orgName(request.getOrgName())
                .description(request.getDescription())
                .status(Status.ACTIVE)
                .orgType(request.getOrgType() != null ? 
                    Organization.OrganizationType.valueOf(request.getOrgType()) : null)
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .website(request.getWebsite())
                .maxUsers(request.getMaxUsers())
                .settings(request.getSettings())
                .build();
        
        // 상위 조직 설정
        if (request.getParentOrganizationId() != null) {
            Organization parentOrg = organizationRepository.findById(request.getParentOrganizationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상위 조직입니다: " + request.getParentOrganizationId()));
            organization.setParentOrganization(parentOrg);
        }
        
        Organization savedOrganization = organizationRepository.save(organization);
        log.info("[OrganizationService] createOrganization - success id={}, orgName={}", 
                savedOrganization.getId(), savedOrganization.getOrgName());
        
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
        log.info("[OrganizationService] updateOrganization - id={}, orgName={}", id, request.getOrgName());
        
        // 조직 존재 여부 확인
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + id));
        
        // 조직명 중복 검사 (자신 제외)
        if (!organization.getOrgName().equals(request.getOrgName())) {
            validateOrgNameUnique(request.getOrgName());
        }
        
        // 조직 정보 수정
        organization.setOrgName(request.getOrgName());
        organization.setDescription(request.getDescription());
        organization.setOrgType(request.getOrgType() != null ? 
            Organization.OrganizationType.valueOf(request.getOrgType()) : null);
        organization.setContactEmail(request.getContactEmail());
        organization.setContactPhone(request.getContactPhone());
        organization.setAddress(request.getAddress());
        organization.setWebsite(request.getWebsite());
        organization.setMaxUsers(request.getMaxUsers());
        organization.setSettings(request.getSettings());
        
        // 상위 조직 변경
        if (request.getParentOrganizationId() != null) {
            Organization parentOrg = organizationRepository.findById(request.getParentOrganizationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상위 조직입니다: " + request.getParentOrganizationId()));
            organization.setParentOrganization(parentOrg);
        } else {
            organization.setParentOrganization(null);
        }
        
        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("[OrganizationService] updateOrganization - success id={}, orgName={}", 
                updatedOrganization.getId(), updatedOrganization.getOrgName());
        
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
        
        // 하위 조직 존재 여부 확인
        if (organizationRepository.existsByParentOrganizationId(id)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "하위 조직이 존재하는 조직은 삭제할 수 없습니다: " + id);
        }
        
        // 조직 삭제
        organizationRepository.delete(organization);
        log.info("[OrganizationService] deleteOrganization - success id={}, orgName={}", 
                id, organization.getOrgName());
    }
    
    /**
     * 조직명 중복 검증
     * 
     * @param orgName 조직명
     * @throws BusinessException 조직명이 중복되는 경우
     */
    private void validateOrgNameUnique(String orgName) {
        if (organizationRepository.existsByOrgName(orgName)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "이미 존재하는 조직명입니다: " + orgName);
        }
    }
    
    /**
     * 조직 키 생성
     */
    private String generateOrgKey(String orgName) {
        String baseKey = orgName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        String orgKey = baseKey;
        int counter = 1;
        
        while (organizationRepository.existsByOrgKey(orgKey)) {
            orgKey = baseKey + "_" + counter;
            counter++;
        }
        
        return orgKey;
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
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 목록
     * @throws BusinessException 상위 조직을 찾을 수 없는 경우
     */
    public List<OrganizationResponse> getChildOrganizations(Long parentOrgId) {
        log.info("[OrganizationService] getChildOrganizations - parentOrgId={}", parentOrgId);
        
        // 상위 조직 존재 여부 확인
        organizationRepository.findById(parentOrgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상위 조직입니다: " + parentOrgId));
        
        List<Organization> childOrganizations = organizationRepository.findByParentOrganizationId(parentOrgId);
        List<OrganizationResponse> result = childOrganizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
        
        log.info("[OrganizationService] getChildOrganizations - success parentOrgId={}, count={}", 
                parentOrgId, result.size());
        return result;
    }
    
    /**
     * 전체 조직 트리 조회
     * 
     * @return 조직 계층 구조 목록
     */
    public List<OrganizationHierarchyResponse> getOrganizationTree() {
        log.info("[OrganizationService] getOrganizationTree");
        
        List<Organization> organizations = organizationRepository.findAll();
        Map<Long, List<Organization>> childrenMap = organizations.stream()
                .filter(org -> org.getParentOrganization() != null)
                .collect(Collectors.groupingBy(org -> org.getParentOrganization().getId()));
        
        List<Organization> rootOrganizations = organizationRepository.findRootOrganizations();
        
        List<OrganizationHierarchyResponse> result = rootOrganizations.stream()
                .map(org -> buildHierarchyResponse(org, childrenMap, 0, org.getOrgName()))
                .collect(Collectors.toList());
        
        log.info("[OrganizationService] getOrganizationTree - success count={}", result.size());
        return result;
    }
    
    /**
     * 조직 경로 조회
     * 
     * @param orgId 조직 ID
     * @return 조직 경로 정보
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public OrganizationPathResponse getOrganizationPath(Long orgId) {
        log.info("[OrganizationService] getOrganizationPath - orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> path = new ArrayList<>();
        Organization current = organization;
        
        while (current != null) {
            path.add(0, OrganizationResponse.from(current));
            current = current.getParentOrganization();
        }
        
        OrganizationPathResponse result = OrganizationPathResponse.from(path);
        log.info("[OrganizationService] getOrganizationPath - success orgId={}, path={}", 
                orgId, result.getFullPath());
        return result;
    }
    
    /**
     * 상위 조직 목록 조회
     * 
     * @param orgId 조직 ID
     * @return 상위 조직 목록
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public List<OrganizationResponse> getAncestors(Long orgId) {
        log.info("[OrganizationService] getAncestors - orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> ancestors = new ArrayList<>();
        Organization current = organization.getParentOrganization();
        
        while (current != null) {
            ancestors.add(0, OrganizationResponse.from(current));
            current = current.getParentOrganization();
        }
        
        log.info("[OrganizationService] getAncestors - success orgId={}, count={}", 
                orgId, ancestors.size());
        return ancestors;
    }
    
    /**
     * 하위 조직 목록 조회 (모든 레벨)
     * 
     * @param orgId 조직 ID
     * @return 하위 조직 목록
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public List<OrganizationResponse> getDescendants(Long orgId) {
        log.info("[OrganizationService] getDescendants - orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> descendants = new ArrayList<>();
        collectDescendants(organization, descendants);
        
        log.info("[OrganizationService] getDescendants - success orgId={}, count={}", 
                orgId, descendants.size());
        return descendants;
    }
    
    /**
     * 조직 이동
     * 
     * @param orgId 조직 ID
     * @param request 조직 이동 요청 정보
     * @return 이동된 조직 정보
     * @throws BusinessException 조직을 찾을 수 없거나 순환 참조가 발생하는 경우
     */
    @Transactional
    public OrganizationResponse moveOrganization(Long orgId, MoveOrganizationRequest request) {
        log.info("[OrganizationService] moveOrganization - orgId={}, newParentId={}", 
                orgId, request.getNewParentId());
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + orgId));
        
        // 새로운 상위 조직 검증
        Organization newParent = null;
        if (request.getNewParentId() != null) {
            newParent = organizationRepository.findById(request.getNewParentId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 상위 조직입니다: " + request.getNewParentId()));
            
            // 순환 참조 방지
            validateNoCircularReference(organization, newParent);
        }
        
        organization.setParentOrganization(newParent);
        Organization savedOrganization = organizationRepository.save(organization);
        
        log.info("[OrganizationService] moveOrganization - success orgId={}, newParentId={}", 
                savedOrganization.getId(), newParent != null ? newParent.getId() : null);
        return OrganizationResponse.from(savedOrganization);
    }
    
    /**
     * 조직 통계 조회
     * 
     * @return 조직 통계 정보
     */
    public OrganizationStatsResponse getOrganizationStats() {
        log.info("[OrganizationService] getOrganizationStats");
        
        List<Organization> organizations = organizationRepository.findAll();
        
        long totalOrganizations = organizations.size();
        long activeOrganizations = organizations.stream()
                .filter(org -> Status.ACTIVE.equals(org.getStatus()))
                .count();
        long inactiveOrganizations = totalOrganizations - activeOrganizations;
        
        // 계층별 통계
        Map<Integer, Long> levelStats = new HashMap<>();
        int maxDepth = 0;
        
        for (Organization org : organizations) {
            int level = calculateLevel(org);
            levelStats.merge(level, 1L, Long::sum);
            maxDepth = Math.max(maxDepth, level);
        }
        
        List<OrganizationStatsResponse.LevelStats> levelStatsList = levelStats.entrySet().stream()
                .map(entry -> OrganizationStatsResponse.LevelStats.builder()
                        .level(entry.getKey())
                        .count(entry.getValue())
                        .description("Level " + entry.getKey())
                        .build())
                .sorted(Comparator.comparing(OrganizationStatsResponse.LevelStats::getLevel))
                .collect(Collectors.toList());
        
        OrganizationStatsResponse result = OrganizationStatsResponse.builder()
                .totalOrganizations(totalOrganizations)
                .activeOrganizations(activeOrganizations)
                .inactiveOrganizations(inactiveOrganizations)
                .maxDepth(maxDepth)
                .levelStats(levelStatsList)
                .build();
        
        log.info("[OrganizationService] getOrganizationStats - success total={}, active={}, maxDepth={}", 
                totalOrganizations, activeOrganizations, maxDepth);
        return result;
    }
    
    // Helper methods
    private OrganizationHierarchyResponse buildHierarchyResponse(Organization org, 
                                                               Map<Long, List<Organization>> childrenMap, 
                                                               int level, 
                                                               String path) {
        List<Organization> children = childrenMap.getOrDefault(org.getId(), List.of());
        List<OrganizationHierarchyResponse> childResponses = children.stream()
                .map(child -> buildHierarchyResponse(child, childrenMap, level + 1, path + " > " + child.getOrgName()))
                .collect(Collectors.toList());
        
        OrganizationHierarchyResponse response = OrganizationHierarchyResponse.from(
                OrganizationResponse.from(org), level, path);
        response.setChildren(childResponses);
        response.setChildrenCount(childResponses.size());
        
        return response;
    }
    
    private void collectDescendants(Organization parent, List<OrganizationResponse> descendants) {
        List<Organization> children = organizationRepository.findByParentOrganizationId(parent.getId());
        for (Organization child : children) {
            descendants.add(OrganizationResponse.from(child));
            collectDescendants(child, descendants);
        }
    }
    
    private void validateNoCircularReference(Organization org, Organization newParent) {
        Organization current = newParent;
        while (current != null) {
            if (current.getId().equals(org.getId())) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "순환 참조가 발생합니다: " + org.getOrgName());
            }
            current = current.getParentOrganization();
        }
    }
    
    private int calculateLevel(Organization org) {
        int level = 0;
        Organization current = org.getParentOrganization();
        while (current != null) {
            level++;
            current = current.getParentOrganization();
        }
        return level;
    }
    
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
        
        // 조직의 사용자 목록 조회
        List<User> users = userRepository.findByOrganizationId(organizationId);
        List<UserResponse> result = users.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
        
        log.info("[OrganizationService] getOrganizationUsers - success organizationId={}, count={}", 
                organizationId, result.size());
        return result;
    }
    
    /**
     * 사용자를 조직에 추가
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
        
        // 조직 존재 확인
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));
        
        // 사용자 존재 확인
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다: " + request.getUserId()));
        
        // 이미 조직에 속한 사용자인지 확인
        if (user.getOrganization() != null && user.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "이미 해당 조직에 속한 사용자입니다.");
        }
        
        // 사용자를 조직에 추가
        user.setOrganization(organization);
        User savedUser = userRepository.save(user);
        
        log.info("[OrganizationService] addUserToOrganization - success userId={}, organizationId={}", 
                savedUser.getId(), organizationId);
        
        return convertToUserResponse(savedUser);
    }
    
    /**
     * 사용자를 조직에서 제거
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @throws BusinessException 조직 또는 사용자를 찾을 수 없거나 사용자가 해당 조직에 속하지 않는 경우
     */
    @Transactional
    public void removeUserFromOrganization(Long organizationId, Long userId) {
        log.info("[OrganizationService] removeUserFromOrganization - organizationId={}, userId={}", 
                organizationId, userId);
        
        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 조직입니다: " + organizationId));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다: " + userId));
        
        // 사용자가 해당 조직에 속하는지 확인
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "사용자가 해당 조직에 속하지 않습니다: userId=" + userId + ", organizationId=" + organizationId);
        }
        
        // 사용자를 조직에서 제거
        user.setOrganization(null);
        userRepository.save(user);
        
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
     * 조직에 속한 테넌트 조회 (1:1 관계)
     * 
     * @param organizationId 조직 ID
     * @return 조직에 속한 테넌트 (Optional)
     * @throws ResourceNotFoundException 조직을 찾을 수 없는 경우
     */
    public Optional<Tenant> getOrganizationTenant(Long organizationId) {
        log.info("[OrganizationService] getOrganizationTenant - organizationId={}", organizationId);

        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId.toString()));

        // 조직의 테넌트 조회 (1:1 관계)
        Optional<Tenant> tenant = organizationRepository.findTenantByOrganizationId(organizationId);

        log.info("[OrganizationService] getOrganizationTenant - success organizationId={}, tenantPresent={}", 
                organizationId, tenant.isPresent());
        return tenant;
    }

    /**
     * 조직에 속한 테넌트 조회 (1:1 관계, 없으면 예외)
     * 
     * @param organizationId 조직 ID
     * @return 조직에 속한 테넌트
     * @throws ResourceNotFoundException 조직 또는 테넌트를 찾을 수 없는 경우
     */
    public Tenant getOrganizationTenantOrThrow(Long organizationId) {
        log.info("[OrganizationService] getOrganizationTenantOrThrow - organizationId={}", organizationId);

        // 조직 존재 확인
        organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId.toString()));

        // 조직의 테넌트 조회 (1:1 관계)
        Tenant tenant = organizationRepository.findTenantByOrganizationId(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "organizationId", organizationId.toString()));

        log.info("[OrganizationService] getOrganizationTenantOrThrow - success organizationId={}, tenantId={}", 
                organizationId, tenant.getId());
        return tenant;
    }
}