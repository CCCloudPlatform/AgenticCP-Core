package com.agenticcp.core.domain.cloud.port.model.account;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CloudSessionCredential 인터페이스 테스트
 */
@DisplayName("CloudSessionCredential 테스트")
class CloudSessionCredentialTest {

    @Nested
    @DisplayName("isValid 테스트")
    class IsValidTest {

        @Test
        @DisplayName("만료 시간이 미래면 유효")
        void isValid_ExpiresAtFuture_ReturnsTrue() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            // when
            boolean result = session.isValid();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 과거면 무효")
        void isValid_ExpiresAtPast_ReturnsFalse() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            // when
            boolean result = session.isValid();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 null이면 무효")
        void isValid_ExpiresAtNull_ReturnsFalse() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(null)
                    .build();

            // when
            boolean result = session.isValid();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpiringSoon 테스트")
    class IsExpiringSoonTest {

        @Test
        @DisplayName("버퍼 시간 내에 만료되면 true")
        void isExpiringSoon_ExpiresWithinBuffer_ReturnsTrue() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5분 후 만료
                    .build();
            int bufferMinutes = 10; // 10분 버퍼

            // when
            boolean result = session.isExpiringSoon(bufferMinutes);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("버퍼 시간 이후에 만료되면 false")
        void isExpiringSoon_ExpiresAfterBuffer_ReturnsFalse() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(LocalDateTime.now().plusMinutes(20)) // 20분 후 만료
                    .build();
            int bufferMinutes = 10; // 10분 버퍼

            // when
            boolean result = session.isExpiringSoon(bufferMinutes);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 null이면 true")
        void isExpiringSoon_ExpiresAtNull_ReturnsTrue() {
            // given
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(null)
                    .build();

            // when
            boolean result = session.isExpiringSoon(10);

            // then
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("프로바이더 타입 반환 확인")
    void getProviderType_ReturnsCorrectType() {
        // given
        AwsSessionCredential awsSession = AwsSessionCredential.builder()
                .accessKeyId("test-key")
                .build();

        // when
        ProviderType providerType = awsSession.getProviderType();

        // then
        assertThat(providerType).isEqualTo(ProviderType.AWS);
    }
}

