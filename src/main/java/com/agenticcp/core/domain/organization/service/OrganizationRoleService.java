package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRoleRepository;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 조직별 역할 관리 서비스
 * 조직-역할 매핑의 조회/할당/제거/기본 설정/우선순위를 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationRoleService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final RoleRepository roleRepository;

    public List<OrganizationRole> listRoles(Long organizationId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        return organizationRoleRepository.findByOrganizationOrderByPriority(organization);
    }

    @Transactional
    public OrganizationRole assignRole(Long organizationId, Long roleId, boolean makeDefault, Integer priority) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);

        if (organizationRoleRepository.existsByOrganizationAndRole(organization, role)) {
            throw new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.BAD_REQUEST, "이미 조직에 할당된 역할입니다");
        }

        OrganizationRole mapping = OrganizationRole.builder()
                .organization(organization)
                .role(role)
                .isDefault(false)
                .priority(priority == null ? 0 : priority)
                .status(Status.ACTIVE)
                .build();

        OrganizationRole saved = organizationRoleRepository.save(mapping);

        if (makeDefault) {
            setDefaultRoleInternal(organization, saved);
        }

        return saved;
    }

    @Transactional
    public void removeRole(Long organizationId, Long roleId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);

        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));

        mapping.setIsDeleted(true);
        organizationRoleRepository.save(mapping);
    }

    @Transactional
    public OrganizationRole setDefaultRole(Long organizationId, Long roleId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);
        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));
        setDefaultRoleInternal(organization, mapping);
        return mapping;
    }

    @Transactional
    public OrganizationRole updatePriority(Long organizationId, Long roleId, Integer priority) {
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);
        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));
        mapping.setPriority(priority == null ? 0 : priority);
        return organizationRoleRepository.save(mapping);
    }

    private void setDefaultRoleInternal(Organization organization, OrganizationRole newDefault) {
        organizationRoleRepository.findDefaultByOrganization(organization)
                .ifPresent(existing -> {
                    boolean sameMapping = existing.getId() != null && existing.getId().equals(newDefault.getId());
                    String existingRoleKey = existing.getRole() != null ? existing.getRole().getRoleKey() : null;
                    String newRoleKey = newDefault.getRole() != null ? newDefault.getRole().getRoleKey() : null;
                    boolean sameRole = Objects.equals(existingRoleKey, newRoleKey);
                    if (!(sameMapping || sameRole)) {
                        existing.setIsDefault(false);
                        organizationRoleRepository.save(existing);
                    }
                });
        newDefault.setIsDefault(true);
        organizationRoleRepository.save(newDefault);
    }

    private Organization getOrganizationOrThrow(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));
    }

    private Role getRoleOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "역할을 찾을 수 없습니다"));
    }
}


