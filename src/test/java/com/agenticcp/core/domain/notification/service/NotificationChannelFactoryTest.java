package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.service.impl.SlackNotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 알림 채널 팩토리 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationChannelFactory 단위 테스트")
class NotificationChannelFactoryTest {

    @Mock
    private SlackNotificationChannel slackChannel;

    private NotificationChannelFactory channelFactory;

    @BeforeEach
    void setUp() {
        when(slackChannel.getChannelType()).thenReturn(ChannelType.SLACK.toString());
        
        // Mock 채널 리스트로 팩토리 생성
        List<NotificationChannel> channelList = List.of(slackChannel);
        channelFactory = new NotificationChannelFactory(channelList);
    }

    @Nested
    @DisplayName("채널 조회 테스트")
    class GetChannelTest {

        @Test
        @DisplayName("지원하는 채널 타입 조회 시 채널 반환")
        void getChannel_WhenSupportedChannelType_ReturnsChannel() {
            // Given
            String channelType = "SLACK";

            // When
            NotificationChannel result = channelFactory.getChannel(channelType);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChannelType()).isEqualTo("SLACK");
        }

        @Test
        @DisplayName("지원하지 않는 채널 타입 조회 시 null 반환")
        void getChannel_WhenUnsupportedChannelType_ReturnsNull() {
            // Given
            String channelType = "UNSUPPORTED";

            // When
            NotificationChannel result = channelFactory.getChannel(channelType);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 채널 타입 조회 시 null 반환")
        void getChannel_WhenNullChannelType_ReturnsNull() {
            // When
            NotificationChannel result = channelFactory.getChannel(null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("지원 채널 타입 목록 테스트")
    class GetSupportedChannelTypesTest {

        @Test
        @DisplayName("지원 채널 타입 목록 조회 시 목록 반환")
        void getSupportedChannelTypes_WhenCalled_ReturnsChannelTypes() {
            // When
            List<String> supportedTypes = channelFactory.getSupportedChannelTypes();

            // Then
            assertThat(supportedTypes).isNotNull();
            assertThat(supportedTypes).contains("SLACK");
        }
    }

    @Nested
    @DisplayName("채널 지원 여부 확인 테스트")
    class IsChannelSupportedTest {

        @Test
        @DisplayName("지원하는 채널 타입 확인 시 true 반환")
        void isChannelSupported_WhenSupportedChannelType_ReturnsTrue() {
            // Given
            String channelType = "SLACK";

            // When
            boolean isSupported = channelFactory.isChannelSupported(channelType);

            // Then
            assertThat(isSupported).isTrue();
        }

        @Test
        @DisplayName("지원하지 않는 채널 타입 확인 시 false 반환")
        void isChannelSupported_WhenUnsupportedChannelType_ReturnsFalse() {
            // Given
            String channelType = "UNSUPPORTED";

            // When
            boolean isSupported = channelFactory.isChannelSupported(channelType);

            // Then
            assertThat(isSupported).isFalse();
        }

        @Test
        @DisplayName("null 채널 타입 확인 시 false 반환")
        void isChannelSupported_WhenNullChannelType_ReturnsFalse() {
            // When
            boolean isSupported = channelFactory.isChannelSupported(null);

            // Then
            assertThat(isSupported).isFalse();
        }
    }

    @Nested
    @DisplayName("팩토리 초기화 테스트")
    class FactoryInitializationTest {

        @Test
        @DisplayName("팩토리 초기화 시 정상 동작 확인")
        void channelFactory_WhenInitialized_WorksCorrectly() {
            // Then
            assertThat(channelFactory).isNotNull();
            assertThat(channelFactory.getSupportedChannelTypes()).contains("SLACK");
        }
    }
}
