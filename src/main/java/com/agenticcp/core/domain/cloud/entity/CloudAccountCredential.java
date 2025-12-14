package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 클라우드 계정 자격증명 엔티티 (암호화 저장)
 * 보안을 위해 별도 테이블로 분리하여 암호화된 자격증명을 저장합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "cloud_account_credentials", indexes = {
    @Index(name = "idx_cloud_credentials_key", columnList = "credential_key", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudAccountCredential extends BaseEntity {

    /**
     * 자격증명 고유 키 (UUID)
     * 외부에서 자격증명을 참조할 때 사용
     */
    @Column(name = "credential_key", nullable = false, unique = true, length = 100)
    private String credentialKey;

    /**
     * 암호화된 Access Key ID
     * AWS: Access Key ID, Azure: Client ID, GCP: Service Account Key
     */
    @Column(name = "access_key_id_encrypted", columnDefinition = "TEXT", nullable = false)
    private String accessKeyIdEncrypted;

    /**
     * 암호화된 Secret Access Key
     * AWS: Secret Access Key, Azure: Client Secret, GCP: Service Account Secret
     */
    @Column(name = "secret_access_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String secretAccessKeyEncrypted;

    /**
     * 기본 리전
     * 프로바이더별 기본 리전 정보
     */
    @Column(name = "region", length = 50)
    private String region;

    /**
     * 추가 메타데이터 (JSON)
     * 프로바이더별 추가 정보 저장
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}

