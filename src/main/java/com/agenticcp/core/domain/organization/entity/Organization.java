package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends BaseEntity {

    @Column(name = "org_key", nullable = false, unique = true)
    private String orgKey;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "description")
    private String description;

    // 테넌트들과의 관계 (1:N)
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Tenant> tenants;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parentOrganization;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Column(name = "org_type")
    @Enumerated(EnumType.STRING)
    private OrganizationType orgType;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "website")
    private String website;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON for organization-specific settings

    @Column(name = "established_date")
    private LocalDateTime establishedDate;

    public enum OrganizationType {
        COMPANY,
        DEPARTMENT,
        TEAM,
        PROJECT,
        DIVISION
    }
}
