package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.platform.entity.License;
import com.agenticcp.core.common.enums.Status;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRepository extends TenantAwareRepository<License, Long> {

    Optional<License> findByLicenseKey(String licenseKey);

    List<License> findByStatus(Status status);

    List<License> findByLicenseType(License.LicenseType licenseType);

    @Query("SELECT l FROM License l WHERE l.status = :status AND l.expiryDate > :now")
    List<License> findActiveLicenses(@Param("status") Status status, @Param("now") LocalDateTime now);

    @Query("SELECT l FROM License l WHERE l.licenseKey = :licenseKey AND l.status = :status " +
           "AND l.expiryDate > :now")
    Optional<License> findActiveLicenseByKey(@Param("licenseKey") String licenseKey, 
                                           @Param("status") Status status, 
                                           @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(l) FROM License l WHERE l.status = :status AND l.expiryDate > :now")
    Long countActiveLicenses(@Param("status") Status status, @Param("now") LocalDateTime now);
}
