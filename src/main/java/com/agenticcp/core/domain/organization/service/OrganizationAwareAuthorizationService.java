package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
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
    private final UserService userService;
    private final OrganizationRoleService organizationRoleService;

    /**
     * 사용자가 해당 조직에서 주어진 roleKey를 보유하는지 확인
     * 
     * @param username 사용자명
     * @param organizationId 조직 ID
     * @param roleKey 역할 키
     * @return 역할 보유 여부
     */
    public boolean hasRoleInOrganization(String username, Long organizationId, String roleKey) {
        log.info("[OrganizationAwareAuthorizationService] hasRoleInOrganization - username={}, organizationId={}, roleKey={}", 
                username, organizationId, roleKey);
        
        User user = userService.getUserByUsernameOrThrow(username);
        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null || user.getOrganization() == null) {
            log.debug("[OrganizationAwareAuthorizationService] hasRoleInOrganization - organization or user organization is null");
            return false;
        }

        if (!Objects.equals(organization.getId(), user.getOrganization().getId())) {
            log.debug("[OrganizationAwareAuthorizationService] hasRoleInOrganization - organization mismatch");
            return false;
        }

        boolean hasRole = organizationRoleService.listRoles(organizationId).stream()
                .map(OrganizationRole::getRole)
                .anyMatch(r -> roleKey.equals(r.getRoleKey()));
        
        log.info("[OrganizationAwareAuthorizationService] hasRoleInOrganization - success username={}, organizationId={}, roleKey={}, hasRole={}", 
                username, organizationId, roleKey, hasRole);
        return hasRole;
    }

    /**
     * 사용자가 해당 조직에서 주어진 permissionKey를 보유하는지 확인
     * - 조직에 매핑된 역할들의 권한 합집합을 통해 검증
     * 
     * @param username 사용자명
     * @param organizationId 조직 ID
     * @param permissionKey 권한 키
     * @return 권한 보유 여부
     */
    public boolean hasPermissionInOrganization(String username, Long organizationId, String permissionKey) {
        log.info("[OrganizationAwareAuthorizationService] hasPermissionInOrganization - username={}, organizationId={}, permissionKey={}", 
                username, organizationId, permissionKey);
        
        User user = userService.getUserByUsernameOrThrow(username);
        Organization organization = organizationRepository.findById(organizationId)
                .orElse(null);
        if (organization == null || user.getOrganization() == null) {
            log.debug("[OrganizationAwareAuthorizationService] hasPermissionInOrganization - organization or user organization is null");
            return false;
        }
        if (!Objects.equals(organization.getId(), user.getOrganization().getId())) {
            log.debug("[OrganizationAwareAuthorizationService] hasPermissionInOrganization - organization mismatch");
            return false;
        }

        boolean hasPermission = organizationRoleService.listRoles(organizationId).stream()
                .flatMap(or -> or.getRole().getPermissions().stream())
                .anyMatch(p -> permissionKey.equals(p.getPermissionKey()));
        
        log.info("[OrganizationAwareAuthorizationService] hasPermissionInOrganization - success username={}, organizationId={}, permissionKey={}, hasPermission={}", 
                username, organizationId, permissionKey, hasPermission);
        return hasPermission;
    }
}


