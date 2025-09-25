package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 관리 서비스
 *
 * 사용자 조회/생성/수정/상태변경 등 사용자 수명주기 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        log.info("[UserService] getAllUsers");
        List<User> result = userRepository.findAll();
        log.info("[UserService] getAllUsers - success count={}", result.size());
        return result;
    }

    public List<User> getActiveUsers() {
        log.info("[UserService] getActiveUsers");
        List<User> result = userRepository.findByStatus(Status.ACTIVE);
        log.info("[UserService] getActiveUsers - success count={}", result.size());
        return result;
    }

    public Optional<User> getUserByUsername(String username) {
        log.info("[UserService] getUserByUsername - username={}", LogMaskingUtils.mask(username, 2, 2));
        Optional<User> result = userRepository.findByUsername(username);
        log.info("[UserService] getUserByUsername - found={} username={}", result.isPresent(), LogMaskingUtils.mask(username, 2, 2));
        return result;
    }

    public User getUserByUsernameOrThrow(String username) {
        log.info("[UserService] getUserByUsernameOrThrow - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));
        log.info("[UserService] getUserByUsernameOrThrow - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return user;
    }

    public Optional<User> getUserByEmail(String email) {
        log.info("[UserService] getUserByEmail - email={}", LogMaskingUtils.mask(email, 2, 2));
        Optional<User> result = userRepository.findByEmail(email);
        log.info("[UserService] getUserByEmail - found={} email={}", result.isPresent(), LogMaskingUtils.mask(email, 2, 2));
        return result;
    }

    public List<User> getUsersByTenant(Tenant tenant) {
        log.info("[UserService] getUsersByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        List<User> result = userRepository.findByTenant(tenant);
        log.info("[UserService] getUsersByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return result;
    }

    public List<User> getActiveUsersByTenant(Tenant tenant) {
        log.info("[UserService] getActiveUsersByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        List<User> result = userRepository.findActiveUsersByTenant(tenant, Status.ACTIVE);
        log.info("[UserService] getActiveUsersByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return result;
    }

    public List<User> getUsersByRole(UserRole role) {
        log.info("[UserService] getUsersByRole - role={}", role);
        List<User> result = userRepository.findByRole(role);
        log.info("[UserService] getUsersByRole - success count={} role={}", result.size(), role);
        return result;
    }

    public List<User> getInactiveUsers(int daysSinceLastLogin) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysSinceLastLogin);
        log.info("[UserService] getInactiveUsers - daysSinceLastLogin={}", daysSinceLastLogin);
        List<User> result = userRepository.findInactiveUsers(before, Status.ACTIVE);
        log.info("[UserService] getInactiveUsers - success count={} daysSinceLastLogin={}", result.size(), daysSinceLastLogin);
        return result;
    }

    public List<User> getLockedUsers(int maxFailedAttempts) {
        log.info("[UserService] getLockedUsers - maxFailedAttempts={}", maxFailedAttempts);
        List<User> result = userRepository.findLockedUsers(maxFailedAttempts, Status.ACTIVE);
        log.info("[UserService] getLockedUsers - success count={} maxFailedAttempts={}", result.size(), maxFailedAttempts);
        return result;
    }

    public Long getActiveUserCountByTenant(Tenant tenant) {
        log.info("[UserService] getActiveUserCountByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        Long count = userRepository.countActiveUsersByTenant(tenant, Status.ACTIVE);
        log.info("[UserService] getActiveUserCountByTenant - success count={} tenantKey={}", count, LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return count;
    }

    public List<User> searchUsers(String keyword) {
        log.info("[UserService] searchUsers - keyword={}", LogMaskingUtils.mask(keyword, 2, 1));
        List<User> result = userRepository.searchUsers(keyword);
        log.info("[UserService] searchUsers - success count={}", result.size());
        return result;
    }

    @Transactional
    public User createUser(User user) {
        log.info("[UserService] createUser - username={} email={}",
                LogMaskingUtils.mask(user.getUsername(), 2, 2),
                LogMaskingUtils.mask(user.getEmail(), 2, 2));
        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        user.setPasswordChangedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("[UserService] createUser - success username={}", LogMaskingUtils.mask(saved.getUsername(), 2, 2));
        return saved;
    }

    @Transactional
    public User updateUser(String username, User updatedUser) {
        log.info("[UserService] updateUser - username={}", LogMaskingUtils.mask(username, 2, 2));
        User existingUser = getUserByUsernameOrThrow(username);
        
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setStatus(updatedUser.getStatus());
        existingUser.setTenant(updatedUser.getTenant());
        existingUser.setOrganization(updatedUser.getOrganization());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setDepartment(updatedUser.getDepartment());
        existingUser.setJobTitle(updatedUser.getJobTitle());
        existingUser.setTimezone(updatedUser.getTimezone());
        existingUser.setLanguage(updatedUser.getLanguage());
        existingUser.setPreferences(updatedUser.getPreferences());
        existingUser.setProfileImageUrl(updatedUser.getProfileImageUrl());
        
        User saved = userRepository.save(existingUser);
        log.info("[UserService] updateUser - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User changePassword(String username, String newPassword) {
        log.info("[UserService] changePassword - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] changePassword - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User updateLastLogin(String username) {
        log.info("[UserService] updateLastLogin - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setLastLogin(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] updateLastLogin - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User handleFailedLogin(String username) {
        log.info("[UserService] handleFailedLogin - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.incrementFailedLoginAttempts();
        
        // Lock account after 5 failed attempts for 30 minutes
        if (user.getFailedLoginAttempts() >= 5) {
            user.lockAccount(30);
        }
        
        log.warn("[UserService] handleFailedLogin - attempts={} username={}", user.getFailedLoginAttempts(), LogMaskingUtils.mask(username, 2, 2));
        return userRepository.save(user);
    }

    @Transactional
    public User unlockUser(String username) {
        log.info("[UserService] unlockUser - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] unlockUser - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User suspendUser(String username) {
        log.info("[UserService] suspendUser - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.SUSPENDED);
        User saved = userRepository.save(user);
        log.info("[UserService] suspendUser - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User activateUser(String username) {
        log.info("[UserService] activateUser - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.ACTIVE);
        User saved = userRepository.save(user);
        log.info("[UserService] activateUser - success username={}", LogMaskingUtils.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public void deleteUser(String username) {
        log.info("[UserService] deleteUser - username={}", LogMaskingUtils.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("[UserService] deleteUser - success username={}", LogMaskingUtils.mask(username, 2, 2));
    }
}
