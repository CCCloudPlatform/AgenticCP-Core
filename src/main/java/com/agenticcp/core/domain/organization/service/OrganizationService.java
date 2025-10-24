package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationHierarchyResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationPathResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationStatsResponse;
import com.agenticcp.core.domain.organization.dto.MoveOrganizationRequest;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    
    /**
     * 조직 생성
     */
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("조직 생성 요청: orgName={}", request.getOrgName());
        
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
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 조직입니다: " + request.getParentOrganizationId()));
            organization.setParentOrganization(parentOrg);
        }
        
        Organization savedOrganization = organizationRepository.save(organization);
        log.info("조직 생성 완료: id={}, orgName={}", savedOrganization.getId(), savedOrganization.getOrgName());
        
        return OrganizationResponse.from(savedOrganization);
    }
    
    /**
     * 조직 조회 (단일)
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long id) {
        log.info("조직 조회 요청: id={}", id);
        
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + id));
        
        return OrganizationResponse.from(organization);
    }
    
    /**
     * 조직 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizations() {
        log.info("조직 목록 조회 요청");
        
        List<Organization> organizations = organizationRepository.findAll();
        
        return organizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 조직 수정
     */
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request) {
        log.info("조직 수정 요청: id={}, orgName={}", id, request.getOrgName());
        
        // 조직 존재 여부 확인
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + id));
        
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
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 조직입니다: " + request.getParentOrganizationId()));
            organization.setParentOrganization(parentOrg);
        } else {
            organization.setParentOrganization(null);
        }
        
        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("조직 수정 완료: id={}, orgName={}", updatedOrganization.getId(), updatedOrganization.getOrgName());
        
        return OrganizationResponse.from(updatedOrganization);
    }
    
    /**
     * 조직 삭제
     */
    public void deleteOrganization(Long id) {
        log.info("조직 삭제 요청: id={}", id);
        
        // 조직 존재 여부 확인
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + id));
        
        // 하위 조직 존재 여부 확인
        if (organizationRepository.existsByParentOrganizationId(id)) {
            throw new IllegalStateException("하위 조직이 존재하는 조직은 삭제할 수 없습니다: " + id);
        }
        
        // 조직 삭제
        organizationRepository.delete(organization);
        log.info("조직 삭제 완료: id={}, orgName={}", id, organization.getOrgName());
    }
    
    /**
     * 조직명 중복 검증
     */
    private void validateOrgNameUnique(String orgName) {
        if (organizationRepository.existsByOrgName(orgName)) {
            throw new IllegalArgumentException("이미 존재하는 조직명입니다: " + orgName);
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
     */
    @Transactional(readOnly = true)
    public long getOrganizationCount() {
        return organizationRepository.count();
    }
    
    /**
     * 특정 조직의 하위 조직 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getChildOrganizations(Long parentOrgId) {
        log.info("하위 조직 목록 조회 요청: parentOrgId={}", parentOrgId);
        
        // 상위 조직 존재 여부 확인
        organizationRepository.findById(parentOrgId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 조직입니다: " + parentOrgId));
        
        List<Organization> childOrganizations = organizationRepository.findByParentOrganizationId(parentOrgId);
        
        return childOrganizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 전체 조직 트리 조회
     */
    public List<OrganizationHierarchyResponse> getOrganizationTree() {
        log.info("[OrganizationService] 전체 조직 트리 조회 요청");
        
        List<Organization> organizations = organizationRepository.findAll();
        Map<Long, List<Organization>> childrenMap = organizations.stream()
                .filter(org -> org.getParentOrganization() != null)
                .collect(Collectors.groupingBy(org -> org.getParentOrganization().getId()));
        
        List<Organization> rootOrganizations = organizationRepository.findRootOrganizations();
        
        List<OrganizationHierarchyResponse> result = rootOrganizations.stream()
                .map(org -> buildHierarchyResponse(org, childrenMap, 0, org.getOrgName()))
                .collect(Collectors.toList());
        
        log.info("[OrganizationService] 전체 조직 트리 조회 완료: count={}", result.size());
        return result;
    }
    
    /**
     * 조직 경로 조회
     */
    public OrganizationPathResponse getOrganizationPath(Long orgId) {
        log.info("[OrganizationService] 조직 경로 조회 요청: orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> path = new ArrayList<>();
        Organization current = organization;
        
        while (current != null) {
            path.add(0, OrganizationResponse.from(current));
            current = current.getParentOrganization();
        }
        
        OrganizationPathResponse result = OrganizationPathResponse.from(path);
        log.info("[OrganizationService] 조직 경로 조회 완료: path={}", result.getFullPath());
        return result;
    }
    
    /**
     * 상위 조직 목록 조회
     */
    public List<OrganizationResponse> getAncestors(Long orgId) {
        log.info("[OrganizationService] 상위 조직 목록 조회 요청: orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> ancestors = new ArrayList<>();
        Organization current = organization.getParentOrganization();
        
        while (current != null) {
            ancestors.add(0, OrganizationResponse.from(current));
            current = current.getParentOrganization();
        }
        
        log.info("[OrganizationService] 상위 조직 목록 조회 완료: count={}", ancestors.size());
        return ancestors;
    }
    
    /**
     * 하위 조직 목록 조회 (모든 레벨)
     */
    public List<OrganizationResponse> getDescendants(Long orgId) {
        log.info("[OrganizationService] 하위 조직 목록 조회 요청: orgId={}", orgId);
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + orgId));
        
        List<OrganizationResponse> descendants = new ArrayList<>();
        collectDescendants(organization, descendants);
        
        log.info("[OrganizationService] 하위 조직 목록 조회 완료: count={}", descendants.size());
        return descendants;
    }
    
    /**
     * 조직 이동
     */
    @Transactional
    public OrganizationResponse moveOrganization(Long orgId, MoveOrganizationRequest request) {
        log.info("[OrganizationService] 조직 이동 요청: orgId={}, newParentId={}", 
                orgId, request.getNewParentId());
        
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + orgId));
        
        // 새로운 상위 조직 검증
        Organization newParent = null;
        if (request.getNewParentId() != null) {
            newParent = organizationRepository.findById(request.getNewParentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 조직입니다: " + request.getNewParentId()));
            
            // 순환 참조 방지
            validateNoCircularReference(organization, newParent);
        }
        
        organization.setParentOrganization(newParent);
        Organization savedOrganization = organizationRepository.save(organization);
        
        log.info("[OrganizationService] 조직 이동 완료: orgId={}, newParentId={}", 
                savedOrganization.getId(), newParent != null ? newParent.getId() : null);
        return OrganizationResponse.from(savedOrganization);
    }
    
    /**
     * 조직 통계 조회
     */
    public OrganizationStatsResponse getOrganizationStats() {
        log.info("[OrganizationService] 조직 통계 조회 요청");
        
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
        
        log.info("[OrganizationService] 조직 통계 조회 완료: total={}, active={}, maxDepth={}", 
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
                throw new IllegalArgumentException("순환 참조가 발생합니다: " + org.getOrgName());
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
}