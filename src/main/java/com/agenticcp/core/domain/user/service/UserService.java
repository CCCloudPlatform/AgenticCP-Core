package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.common.logging.masking.MaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final MaskingService maskingService;

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
        log.info("[UserService] getUserByUsername - username={}", maskingService.mask(username, 2, 2));
        Optional<User> result = userRepository.findByUsername(username);
        log.info("[UserService] getUserByUsername - found={} username={}", result.isPresent(), maskingService.mask(username, 2, 2));
        return result;
    }

    public User getUserByUsernameOrThrow(String username) {
        log.info("[UserService] getUserByUsernameOrThrow - username={}", maskingService.mask(username, 2, 2));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));
        log.info("[UserService] getUserByUsernameOrThrow - success username={}", maskingService.mask(username, 2, 2));
        return user;
    }

    public Optional<User> getUserByEmail(String email) {
        log.info("[UserService] getUserByEmail - email={}", maskingService.mask(email, 2, 2));
        Optional<User> result = userRepository.findByEmail(email);
        log.info("[UserService] getUserByEmail - found={} email={}", result.isPresent(), maskingService.mask(email, 2, 2));
        return result;
    }

    // 설계 B: User는 전역 계정이므로 tenant 필드 제거됨
    // Worker를 통해 테넌트별 사용자 조회 필요
    @Deprecated
    public List<User> getUsersByTenant(Tenant tenant) {
        log.warn("[UserService] getUsersByTenant - Deprecated: Worker를 통해 조회해야 함");
        // TODO: Worker를 통해 테넌트별 사용자 조회 구현
        return List.of();
    }

    // 설계 B: User는 전역 계정이므로 tenant 필드 제거됨
    // Worker를 통해 테넌트별 사용자 조회 필요
    @Deprecated
    public List<User> getActiveUsersByTenant(Tenant tenant) {
        log.warn("[UserService] getActiveUsersByTenant - Deprecated: Worker를 통해 조회해야 함");
        // TODO: Worker를 통해 테넌트별 활성 사용자 조회 구현
        return List.of();
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

    // 설계 B: User는 전역 계정이므로 tenant 필드 제거됨
    // Worker를 통해 테넌트별 사용자 수 조회 필요
    @Deprecated
    public Long getActiveUserCountByTenant(Tenant tenant) {
        log.warn("[UserService] getActiveUserCountByTenant - Deprecated: Worker를 통해 조회해야 함");
        // TODO: Worker를 통해 테넌트별 활성 사용자 수 조회 구현
        return 0L;
    }

    public List<User> searchUsers(String keyword) {
        log.info("[UserService] searchUsers - keyword={}", maskingService.mask(keyword, 2, 1));
        List<User> result = userRepository.searchUsers(keyword);
        log.info("[UserService] searchUsers - success count={}", result.size());
        return result;
    }

    @Transactional
    public User createUser(User user) {
        log.info("[UserService] createUser - username={} email={}",
                maskingService.mask(user.getUsername(), 2, 2),
                maskingService.mask(user.getEmail(), 2, 2));
        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        user.setPasswordChangedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("[UserService] createUser - success username={}", maskingService.mask(saved.getUsername(), 2, 2));
        return saved;
    }

    @Transactional
    public User updateUser(String username, User updatedUser) {
        log.info("[UserService] updateUser - username={}", maskingService.mask(username, 2, 2));
        User existingUser = getUserByUsernameOrThrow(username);
        
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setStatus(updatedUser.getStatus());
        // 설계 B: User는 전역 계정이므로 tenant, organization 필드 제거됨
        // existingUser.setTenant(updatedUser.getTenant());
        // existingUser.setOrganization(updatedUser.getOrganization());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setDepartment(updatedUser.getDepartment());
        existingUser.setJobTitle(updatedUser.getJobTitle());
        existingUser.setTimezone(updatedUser.getTimezone());
        existingUser.setLanguage(updatedUser.getLanguage());
        existingUser.setPreferences(updatedUser.getPreferences());
        existingUser.setProfileImageUrl(updatedUser.getProfileImageUrl());
        
        User saved = userRepository.save(existingUser);
        log.info("[UserService] updateUser - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User changePassword(String username, String newPassword) {
        log.info("[UserService] changePassword - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] changePassword - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User updateLastLogin(String username) {
        log.info("[UserService] updateLastLogin - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setLastLogin(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] updateLastLogin - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User handleFailedLogin(String username) {
        log.info("[UserService] handleFailedLogin - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.incrementFailedLoginAttempts();
        
        // Lock account after 5 failed attempts for 30 minutes
        if (user.getFailedLoginAttempts() >= 5) {
            user.lockAccount(30);
        }
        
        log.warn("[UserService] handleFailedLogin - attempts={} username={}", user.getFailedLoginAttempts(), maskingService.mask(username, 2, 2));
        return userRepository.save(user);
    }

    @Transactional
    public User unlockUser(String username) {
        log.info("[UserService] unlockUser - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.resetFailedLoginAttempts();
        User saved = userRepository.save(user);
        log.info("[UserService] unlockUser - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User suspendUser(String username) {
        log.info("[UserService] suspendUser - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.SUSPENDED);
        User saved = userRepository.save(user);
        log.info("[UserService] suspendUser - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public User activateUser(String username) {
        log.info("[UserService] activateUser - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.ACTIVE);
        User saved = userRepository.save(user);
        log.info("[UserService] activateUser - success username={}", maskingService.mask(username, 2, 2));
        return saved;
    }

    @Transactional
    public void deleteUser(String username) {
        log.info("[UserService] deleteUser - username={}", maskingService.mask(username, 2, 2));
        User user = getUserByUsernameOrThrow(username);
        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("[UserService] deleteUser - success username={}", maskingService.mask(username, 2, 2));
    }

    // 회원가입을 위한 추가 메서드들
    public boolean existsByUsername(String username) {
        log.info("[UserService] existsByUsername - username={}", maskingService.mask(username, 2, 2));
        boolean exists = getUserByUsername(username).isPresent();
        log.info("[UserService] existsByUsername - exists={} username={}", exists, maskingService.mask(username, 2, 2));
        return exists;
    }

    public boolean existsByEmail(String email) {
        log.info("[UserService] existsByEmail - email={}", maskingService.mask(email, 2, 2));
        boolean exists = getUserByEmail(email).isPresent();
        log.info("[UserService] existsByEmail - exists={} email={}", exists, maskingService.mask(email, 2, 2));
        return exists;
    }

    @Transactional
    public User saveUser(User user) {
        log.info("[UserService] saveUser - username={}", maskingService.mask(user.getUsername(), 2, 2));
        User saved = userRepository.save(user);
        log.info("[UserService] saveUser - success username={}", maskingService.mask(saved.getUsername(), 2, 2));
        return saved;
    }

    /**
     * 2FA 활성화
     * 
     * @param username 사용자명
     * @param secretKey TOTP 시크릿 키
     * @return 업데이트된 사용자
     */
    @Transactional
    public User enableTwoFactor(String username, String secretKey) {
        log.info("[UserService] enableTwoFactor - username={}", maskingService.mask(username, 2, 2));
        
        User user = getUserByUsernameOrThrow(username);
        user.enableTwoFactor(secretKey);
        
        User saved = userRepository.save(user);
        log.info("[UserService] enableTwoFactor - success username={} status={}", 
            maskingService.mask(saved.getUsername(), 2, 2), saved.getStatus());
        
        return saved;
    }

    /**
     * 2FA 비활성화
     * 
     * @param username 사용자명
     * @return 업데이트된 사용자
     */
    @Transactional
    public User disableTwoFactor(String username) {
        log.info("[UserService] disableTwoFactor - username={}", maskingService.mask(username, 2, 2));
        
        User user = getUserByUsernameOrThrow(username);
        user.disableTwoFactor();
        
        User saved = userRepository.save(user);
        log.info("[UserService] disableTwoFactor - success username={}", 
            maskingService.mask(saved.getUsername(), 2, 2));
        
        return saved;
    }

    /**
     * 상태별 사용자 조회
     * 
     * @param status 사용자 상태
     * @return 해당 상태의 사용자 목록
     */
    public List<User> getUsersByStatus(Status status) {
        log.info("[UserService] getUsersByStatus - status={}", status);
        List<User> users = userRepository.findAll().stream()
            .filter(user -> user.getStatus() == status)
            .collect(Collectors.toList());
        log.info("[UserService] getUsersByStatus - found {} users", users.size());
        return users;
    }
}
