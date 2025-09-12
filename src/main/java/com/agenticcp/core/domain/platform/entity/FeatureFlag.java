package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag extends BaseEntity {

    @Column(name = "flag_key", nullable = false, unique = true)
    private String flagKey;

    @Column(name = "flag_name", nullable = false)
    private String flagName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "target_tenants", columnDefinition = "TEXT")
    private String targetTenants; // JSON array of tenant IDs

    @Column(name = "target_users", columnDefinition = "TEXT")
    private String targetUsers; // JSON array of user IDs

    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional configuration
}
