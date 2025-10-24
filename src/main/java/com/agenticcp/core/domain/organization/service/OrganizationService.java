package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final TenantRepository tenantRepository;
    
    /**
     * 조직 생성
     */
    public OrganizationResponse createOrganization(CreateOrganizationRequest request, Long tenantId) {
        log.info("조직 생성 요청: orgName={}, tenantId={}", request.getOrgName(), tenantId);
        
        // 테넌트 존재 여부 확인
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다: " + tenantId));
        
        // 조직명 중복 검사
        validateOrgNameUnique(request.getOrgName(), tenantId);
        
        // 조직 생성
        Organization organization = Organization.builder()
                .orgName(request.getOrgName())
                .description(request.getDescription())
                .tenant(tenant)
                .status(Status.ACTIVE)
                .build();
        
        Organization savedOrganization = organizationRepository.save(organization);
        log.info("조직 생성 완료: id={}, orgName={}", savedOrganization.getId(), savedOrganization.getOrgName());
        
        return OrganizationResponse.from(savedOrganization);
    }
    
    /**
     * 조직 조회 (단일)
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long id, Long tenantId) {
        log.info("조직 조회 요청: id={}, tenantId={}", id, tenantId);
        
        Organization organization = organizationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + id));
        
        return OrganizationResponse.from(organization);
    }
    
    /**
     * 조직 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizations(Long tenantId) {
        log.info("조직 목록 조회 요청: tenantId={}", tenantId);
        
        List<Organization> organizations = organizationRepository.findByTenantId(tenantId);
        
        return organizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 조직 수정
     */
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request, Long tenantId) {
        log.info("조직 수정 요청: id={}, orgName={}, tenantId={}", id, request.getOrgName(), tenantId);
        
        // 조직 존재 여부 및 테넌트 검증
        Organization organization = organizationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다: " + id));
        
        // 조직명 중복 검사 (자신 제외)
        if (!organization.getOrgName().equals(request.getOrgName())) {
            validateOrgNameUnique(request.getOrgName(), tenantId);
        }
        
        // 조직 정보 수정
        organization.setOrgName(request.getOrgName());
        organization.setDescription(request.getDescription());
        
        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("조직 수정 완료: id={}, orgName={}", updatedOrganization.getId(), updatedOrganization.getOrgName());
        
        return OrganizationResponse.from(updatedOrganization);
    }
    
    /**
     * 조직 삭제
     */
    public void deleteOrganization(Long id, Long tenantId) {
        log.info("조직 삭제 요청: id={}, tenantId={}", id, tenantId);
        
        // 조직 존재 여부 및 테넌트 검증
        Organization organization = organizationRepository.findByIdAndTenantId(id, tenantId)
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
    private void validateOrgNameUnique(String orgName, Long tenantId) {
        if (organizationRepository.existsByOrgNameAndTenantId(orgName, tenantId)) {
            throw new IllegalArgumentException("이미 존재하는 조직명입니다: " + orgName);
        }
    }
    
    /**
     * 테넌트별 조직 수 조회
     */
    @Transactional(readOnly = true)
    public long getOrganizationCount(Long tenantId) {
        return organizationRepository.countByTenantId(tenantId);
    }
    
    /**
     * 특정 조직의 하위 조직 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getChildOrganizations(Long parentOrgId, Long tenantId) {
        log.info("하위 조직 목록 조회 요청: parentOrgId={}, tenantId={}", parentOrgId, tenantId);
        
        // 상위 조직 존재 여부 및 테넌트 검증
        organizationRepository.findByIdAndTenantId(parentOrgId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 조직입니다: " + parentOrgId));
        
        List<Organization> childOrganizations = organizationRepository.findByParentOrganizationId(parentOrgId);
        
        return childOrganizations.stream()
                .map(OrganizationResponse::from)
                .collect(Collectors.toList());
    }
}
