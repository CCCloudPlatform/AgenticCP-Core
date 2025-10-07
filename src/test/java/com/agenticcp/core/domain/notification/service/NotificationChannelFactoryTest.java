package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.service.impl.EmailNotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 알림 채널 팩토리 테스트
 */
@ExtendWith(MockitoExtension.class)
class NotificationChannelFactoryTest {

    @Mock
    private EmailNotificationChannel emailChannel;

    private NotificationChannelFactory channelFactory;

    @BeforeEach
    void setUp() {
        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL.toString());
        
        // Mock 채널 리스트로 팩토리 생성
        List<NotificationChannel> channelList = List.of(emailChannel);
        channelFactory = new NotificationChannelFactory(channelList);
    }

    /**
     * 이메일 채널 조회 성공 테스트
     * 
     * Given: EMAIL 타입의 채널이 팩토리에 등록됨
     * When: EMAIL 채널 타입으로 채널 조회 요청
     * Then: 해당 이메일 채널 인스턴스가 반환됨
     */
    @Test
    void testGetChannel_EmailChannel_Success() {
        // Given
        String channelType = "EMAIL";

        // When
        NotificationChannel result = channelFactory.getChannel(channelType);

        // Then
        assertNotNull(result);
        assertEquals("EMAIL", result.getChannelType());
    }

    /**
     * 지원하지 않는 채널 타입 조회 테스트
     * 
     * Given: SMS 타입의 채널이 팩토리에 등록되지 않음
     * When: SMS 채널 타입으로 채널 조회 요청
     * Then: null이 반환됨
     */
    @Test
    void testGetChannel_UnsupportedChannel_ReturnsNull() {
        // Given
        String channelType = "UNSUPPORTED";

        // When
        NotificationChannel result = channelFactory.getChannel(channelType);

        // Then
        assertNull(result);
    }

    /**
     * null 채널 타입 조회 테스트
     * 
     * Given: null 채널 타입
     * When: 채널 조회 요청
     * Then: null이 반환됨
     */
    @Test
    void testGetChannel_NullChannelType_ReturnsNull() {
        // When
        NotificationChannel result = channelFactory.getChannel(null);

        // Then
        assertNull(result);
    }

    /**
     * 지원하는 채널 타입 목록 조회 테스트
     * 
     * Given: 팩토리에 등록된 채널들
     * When: 지원하는 채널 타입 목록 조회
     * Then: EMAIL을 포함한 지원 채널 타입 목록 반환
     */
    @Test
    void testGetSupportedChannelTypes_Success() {
        // When
        List<String> supportedTypes = channelFactory.getSupportedChannelTypes();

        // Then
        assertNotNull(supportedTypes);
        assertTrue(supportedTypes.contains("EMAIL"));
    }

    /**
     * 지원하는 채널 타입 확인 테스트
     * 
     * Given: EMAIL 채널 타입
     * When: 채널 지원 여부 확인
     * Then: true 반환
     */
    @Test
    void testIsChannelSupported_SupportedChannel_ReturnsTrue() {
        // Given
        String channelType = "EMAIL";

        // When
        boolean isSupported = channelFactory.isChannelSupported(channelType);

        // Then
        assertTrue(isSupported);
    }

    /**
     * 지원하지 않는 채널 타입 확인 테스트
     * 
     * Given: UNSUPPORTED 채널 타입
     * When: 채널 지원 여부 확인
     * Then: false 반환
     */
    @Test
    void testIsChannelSupported_UnsupportedChannel_ReturnsFalse() {
        // Given
        String channelType = "UNSUPPORTED";

        // When
        boolean isSupported = channelFactory.isChannelSupported(channelType);

        // Then
        assertFalse(isSupported);
    }

    /**
     * null 채널 타입 지원 여부 확인 테스트
     * 
     * Given: null 채널 타입
     * When: 채널 지원 여부 확인
     * Then: false 반환
     */
    @Test
    void testIsChannelSupported_NullChannelType_ReturnsFalse() {
        // When
        boolean isSupported = channelFactory.isChannelSupported(null);

        // Then
        assertFalse(isSupported);
    }

    /**
     * 채널 팩토리 초기화 테스트
     * 
     * Given: 이메일 채널이 포함된 채널 리스트
     * When: NotificationChannelFactory 초기화
     * Then: 팩토리가 정상적으로 초기화되고 EMAIL 채널 지원
     */
    @Test
    void testChannelFactory_Initialization() {
        // Then
        assertNotNull(channelFactory);
        assertTrue(channelFactory.getSupportedChannelTypes().contains("EMAIL"));
    }
}
