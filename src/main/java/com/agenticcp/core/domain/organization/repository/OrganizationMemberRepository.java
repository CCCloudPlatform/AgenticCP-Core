package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import com.agenticcp.core.domain.organization.entity.OrganizationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OrganizationMember Repository
 * 
 * <p>OrganizationMember 엔티티에 대한 데이터 접근을 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, OrganizationMemberId> {
    
    /**
     * 조직 ID로 멤버 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return 멤버 목록
     */
    List<OrganizationMember> findByOrganizationId(Long organizationId);
    
    /**
     * 사용자 ID로 멤버 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 멤버 목록
     */
    List<OrganizationMember> findByUserId(Long userId);
    
    /**
     * 조직 ID와 사용자 ID로 멤버 조회
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @return 멤버 (Optional)
     */
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    
    /**
     * 조직 ID와 사용자 ID로 멤버 존재 여부 확인
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
    
    /**
     * 조직 ID와 사용자 ID로 멤버 삭제
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     */
    void deleteByOrganizationIdAndUserId(Long organizationId, Long userId);
}

