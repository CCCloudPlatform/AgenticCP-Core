package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends TenantAwareRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByTenant(Tenant tenant);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(Status status);

    @Query("SELECT u FROM User u WHERE u.tenant = :tenant AND u.status = :status AND u.isDeleted = false")
    List<User> findActiveUsersByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status AND u.isDeleted = false")
    List<User> findActiveUsersByRole(@Param("role") UserRole role, @Param("status") Status status);

    @Query("SELECT u FROM User u WHERE u.lastLogin < :before AND u.status = :status")
    List<User> findInactiveUsers(@Param("before") LocalDateTime before, @Param("status") Status status);

    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :maxAttempts AND u.status = :status")
    List<User> findLockedUsers(@Param("maxAttempts") Integer maxAttempts, @Param("status") Status status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant = :tenant AND u.status = :status AND u.isDeleted = false")
    Long countActiveUsersByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.name LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);
}
