package com.agenticcp.core.common.dto.auth;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String username;
    private String email;
    private String name;
    private UserRole role;
    private Status status;
    private String tenantKey;
    private String tenantName;
    private List<String> permissions;
    private LocalDateTime lastLogin;
    private Boolean twoFactorEnabled;

    public static UserInfoResponse from(User user) {
        List<String> permissions = null;
        
        // 사용자의 권한 목록 생성
        if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
            permissions = user.getPermissions().stream()
                    .map(permission -> permission.getPermissionName())
                    .collect(Collectors.toList());
        } else if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            // 역할 기반 권한 추출
            permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getPermissionName())
                    .distinct()
                    .collect(Collectors.toList());
        }
        
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .tenantKey(user.getTenant() != null ? user.getTenant().getTenantKey() : null)
                .tenantName(user.getTenant() != null ? user.getTenant().getTenantName() : null)
                .permissions(permissions)
                .lastLogin(user.getLastLogin())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .build();
    }
}
