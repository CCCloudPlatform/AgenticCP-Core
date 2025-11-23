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

    /**
     * 조직별 역할 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return 조직에 할당된 역할 목록 (우선순위 순)
     * @throws BusinessException 조직을 찾을 수 없는 경우
     */
    public List<OrganizationRole> listRoles(Long organizationId) {
        log.info("[OrganizationRoleService] listRoles - organizationId={}", organizationId);
        Organization organization = getOrganizationOrThrow(organizationId);
        List<OrganizationRole> result = organizationRoleRepository.findByOrganizationOrderByPriority(organization);
        log.info("[OrganizationRoleService] listRoles - success organizationId={}, count={}", 
            organizationId, result.size());
        return result;
    }

    /**
     * 조직에 역할 할당
     * 
     * @param organizationId 조직 ID
     * @param roleId 역할 ID
     * @param makeDefault 기본 역할로 설정 여부
     * @param priority 우선순위
     * @return 할당된 조직-역할 매핑
     * @throws BusinessException 조직 또는 역할을 찾을 수 없거나 이미 할당된 역할인 경우
     */
    @Transactional
    public OrganizationRole assignRole(Long organizationId, Long roleId, boolean makeDefault, Integer priority) {
        log.info("[OrganizationRoleService] assignRole - organizationId={}, roleId={}, makeDefault={}, priority={}", 
            organizationId, roleId, makeDefault, priority);
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

        log.info("[OrganizationRoleService] assignRole - success organizationId={}, roleId={}, mappingId={}", 
            organizationId, roleId, saved.getId());
        return saved;
    }

    /**
     * 조직에서 역할 제거
     * 
     * @param organizationId 조직 ID
     * @param roleId 역할 ID
     * @throws BusinessException 조직 또는 역할을 찾을 수 없거나 할당되지 않은 역할인 경우
     */
    @Transactional
    public void removeRole(Long organizationId, Long roleId) {
        log.info("[OrganizationRoleService] removeRole - organizationId={}, roleId={}", organizationId, roleId);
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);

        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));

        mapping.setIsDeleted(true);
        organizationRoleRepository.save(mapping);
        log.info("[OrganizationRoleService] removeRole - success organizationId={}, roleId={}", 
            organizationId, roleId);
    }

    /**
     * 조직의 기본 역할 설정
     * 
     * @param organizationId 조직 ID
     * @param roleId 역할 ID
     * @return 기본 역할로 설정된 조직-역할 매핑
     * @throws BusinessException 조직 또는 역할을 찾을 수 없거나 할당되지 않은 역할인 경우
     */
    @Transactional
    public OrganizationRole setDefaultRole(Long organizationId, Long roleId) {
        log.info("[OrganizationRoleService] setDefaultRole - organizationId={}, roleId={}", 
            organizationId, roleId);
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);
        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));
        setDefaultRoleInternal(organization, mapping);
        log.info("[OrganizationRoleService] setDefaultRole - success organizationId={}, roleId={}", 
            organizationId, roleId);
        return mapping;
    }

    /**
     * 조직-역할 매핑의 우선순위 업데이트
     * 
     * @param organizationId 조직 ID
     * @param roleId 역할 ID
     * @param priority 우선순위
     * @return 우선순위가 업데이트된 조직-역할 매핑
     * @throws BusinessException 조직 또는 역할을 찾을 수 없거나 할당되지 않은 역할인 경우
     */
    @Transactional
    public OrganizationRole updatePriority(Long organizationId, Long roleId, Integer priority) {
        log.info("[OrganizationRoleService] updatePriority - organizationId={}, roleId={}, priority={}", 
            organizationId, roleId, priority);
        Organization organization = getOrganizationOrThrow(organizationId);
        Role role = getRoleOrThrow(roleId);
        OrganizationRole mapping = organizationRoleRepository.findByOrganizationAndRole(organization, role)
                .orElseThrow(() -> new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.NOT_FOUND, "조직에 할당되지 않은 역할입니다"));
        mapping.setPriority(priority == null ? 0 : priority);
        OrganizationRole saved = organizationRoleRepository.save(mapping);
        log.info("[OrganizationRoleService] updatePriority - success organizationId={}, roleId={}, priority={}", 
            organizationId, roleId, priority);
        return saved;
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


