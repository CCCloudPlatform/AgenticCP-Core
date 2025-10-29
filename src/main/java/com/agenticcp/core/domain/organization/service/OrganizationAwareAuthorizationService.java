package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRoleRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * 조직 컨텍스트 기반 인가 확인 서비스
 * 사용자(orgId 기준)의 역할/권한 보유 여부를 검증하는 진입점입니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationAwareAuthorizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final UserService userService;
    private final OrganizationRoleService organizationRoleService;

    /**
     * 사용자가 해당 조직에서 주어진 roleKey를 보유하는지 확인
     */
    public boolean hasRoleInOrganization(String username, Long organizationId, String roleKey) {
        User user = userService.getUserByUsernameOrThrow(username);
        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null || user.getOrganization() == null) {
            return false;
        }

        if (!Objects.equals(organization.getId(), user.getOrganization().getId())) {
            return false;
        }

        return organizationRoleService.listRoles(organizationId).stream()
                .map(OrganizationRole::getRole)
                .anyMatch(r -> roleKey.equals(r.getRoleKey()));
    }

    /**
     * 사용자가 해당 조직에서 주어진 permissionKey를 보유하는지 확인
     * - 조직에 매핑된 역할들의 권한 합집합을 통해 검증
     */
    public boolean hasPermissionInOrganization(String username, Long organizationId, String permissionKey) {
        User user = userService.getUserByUsernameOrThrow(username);
        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null || user.getOrganization() == null) {
            return false;
        }
        if (!Objects.equals(organization.getId(), user.getOrganization().getId())) {
            return false;
        }

        return organizationRoleService.listRoles(organizationId).stream()
                .flatMap(or -> or.getRole().getPermissions().stream())
                .anyMatch(p -> permissionKey.equals(p.getPermissionKey()));
    }
}


