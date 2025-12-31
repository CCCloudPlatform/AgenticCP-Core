package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import com.agenticcp.core.domain.organization.enums.OrganizationErrorCode;
import com.agenticcp.core.domain.organization.repository.OrganizationMemberRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OrganizationMember 서비스
 * 
 * <p>조직과 사용자 간의 관계를 관리하는 서비스입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationMemberService {
    
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    
    /**
     * 조직에 멤버 추가
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @param role 조직 내 역할 (선택적)
     * @return 생성된 OrganizationMember
     * @throws BusinessException 조직 또는 사용자를 찾을 수 없거나 이미 멤버로 등록된 경우
     */
    @Transactional
    public OrganizationMember addMember(Long organizationId, Long userId, String role) {
        log.info("[OrganizationMemberService] addMember - organizationId={}, userId={}, role={}", 
                organizationId, userId, role);
        
        // 조직 존재 확인
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 이미 멤버로 등록되어 있는지 확인
        if (organizationMemberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_ALREADY_EXISTS, 
                    "이미 멤버로 등록된 사용자입니다.");
        }
        
        // OrganizationMember 생성 (설계 B: status 필드 제거됨)
        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(role)
                .build();
        
        OrganizationMember savedMember = organizationMemberRepository.save(member);
        
        log.info("[OrganizationMemberService] addMember - success organizationId={}, userId={}", 
                organizationId, userId);
        
        return savedMember;
    }
    
    /**
     * 조직에서 멤버 제거
     * 
     * @param organizationId 조직 ID
     * @param userId 사용자 ID
     * @throws BusinessException 멤버를 찾을 수 없는 경우
     */
    @Transactional
    public void removeMember(Long organizationId, Long userId) {
        log.info("[OrganizationMemberService] removeMember - organizationId={}, userId={}", 
                organizationId, userId);
        
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND, 
                        "멤버를 찾을 수 없습니다."));
        
        organizationMemberRepository.delete(member);
        
        log.info("[OrganizationMemberService] removeMember - success organizationId={}, userId={}", 
                organizationId, userId);
    }
    
    /**
     * 조직의 멤버 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return 멤버 목록
     */
    public List<OrganizationMember> getMembers(Long organizationId) {
        log.info("[OrganizationMemberService] getMembers - organizationId={}", organizationId);
        return organizationMemberRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * 사용자가 속한 조직 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 조직 멤버십 목록
     */
    public List<OrganizationMember> getOrganizationsByUserId(Long userId) {
        log.info("[OrganizationMemberService] getOrganizationsByUserId - userId={}", userId);
        return organizationMemberRepository.findByUserId(userId);
    }
}

