package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 클라우드 계정 자격증명 Repository
 * 암호화된 자격증명 데이터의 영속성을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface CloudAccountCredentialRepository extends JpaRepository<CloudAccountCredential, Long> {

    /**
     * 자격증명 키로 자격증명을 조회합니다.
     * 
     * @param credentialKey 자격증명 고유 키 (UUID)
     * @return CloudAccountCredential Optional
     */
    Optional<CloudAccountCredential> findByCredentialKey(String credentialKey);

    /**
     * 자격증명 키가 존재하는지 확인합니다.
     * 
     * @param credentialKey 자격증명 고유 키 (UUID)
     * @return 존재하면 true
     */
    boolean existsByCredentialKey(String credentialKey);

    /**
     * 자격증명 키로 자격증명을 삭제합니다.
     * 
     * @param credentialKey 자격증명 고유 키 (UUID)
     */
    void deleteByCredentialKey(String credentialKey);
}

