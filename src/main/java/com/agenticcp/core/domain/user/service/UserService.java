package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getActiveUsers() {
        return userRepository.findByStatus(Status.ACTIVE);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getUsersByTenant(Tenant tenant) {
        return userRepository.findByTenant(tenant);
    }

    public List<User> getActiveUsersByTenant(Tenant tenant) {
        return userRepository.findActiveUsersByTenant(tenant, Status.ACTIVE);
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> getInactiveUsers(int daysSinceLastLogin) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysSinceLastLogin);
        return userRepository.findInactiveUsers(before, Status.ACTIVE);
    }

    public List<User> getLockedUsers(int maxFailedAttempts) {
        return userRepository.findLockedUsers(maxFailedAttempts, Status.ACTIVE);
    }

    public Long getActiveUserCountByTenant(Tenant tenant) {
        return userRepository.countActiveUsersByTenant(tenant, Status.ACTIVE);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    @Transactional
    public User createUser(User user) {
        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        user.setPasswordChangedAt(LocalDateTime.now());
        log.info("Creating user: {}", user.getUsername());
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(String username, User updatedUser) {
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
        
        log.info("Updating user: {}", username);
        return userRepository.save(existingUser);
    }

    @Transactional
    public User changePassword(String username, String newPassword) {
        User user = getUserByUsernameOrThrow(username);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        log.info("Password changed for user: {}", username);
        return userRepository.save(user);
    }

    @Transactional
    public User updateLastLogin(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.setLastLogin(LocalDateTime.now());
        user.resetFailedLoginAttempts();
        log.info("Last login updated for user: {}", username);
        return userRepository.save(user);
    }

    @Transactional
    public User handleFailedLogin(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.incrementFailedLoginAttempts();
        
        // Lock account after 5 failed attempts for 30 minutes
        if (user.getFailedLoginAttempts() >= 5) {
            user.lockAccount(30);
        }
        
        log.warn("Failed login attempt for user: {} (attempts: {})", username, user.getFailedLoginAttempts());
        return userRepository.save(user);
    }

    @Transactional
    public User unlockUser(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.resetFailedLoginAttempts();
        log.info("User unlocked: {}", username);
        return userRepository.save(user);
    }

    @Transactional
    public User suspendUser(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.SUSPENDED);
        log.info("User suspended: {}", username);
        return userRepository.save(user);
    }

    @Transactional
    public User activateUser(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.setStatus(Status.ACTIVE);
        log.info("User activated: {}", username);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("Soft deleted user: {}", username);
    }

    // 인증 서비스를 위한 추가 메서드들

    @Transactional
    public void updateFailedLoginAttempts(String username, int attempts) {
        User user = getUserByUsernameOrThrow(username);
        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedLoginAttempts(String username) {
        User user = getUserByUsernameOrThrow(username);
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }

    @Transactional
    public void lockUserAccount(String username, LocalDateTime lockedUntil) {
        User user = getUserByUsernameOrThrow(username);
        user.setLockedUntil(lockedUntil);
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        log.info("Updating user: {}", user.getUsername());
        return userRepository.save(user);
    }
}
