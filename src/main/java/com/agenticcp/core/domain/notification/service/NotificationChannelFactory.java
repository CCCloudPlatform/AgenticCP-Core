package com.agenticcp.core.domain.notification.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 채널 팩토리
 * 
 * <p>확장 가능한 채널 관리를 위한 팩토리 클래스입니다.</p>
 * 
 * <p>Spring의 의존성 주입을 통해 등록된 모든 NotificationChannel 구현체를
 * 자동으로 수집하여 채널 타입별로 관리합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * NotificationChannel channel = factory.getChannel("SLACK");
 * NotificationResponse response = channel.send(request);
 * }</pre>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Component
public class NotificationChannelFactory {

    private final Map<String, NotificationChannel> channels;

    /**
     * 알림 채널 팩토리 생성자
     * 
     * @param channelList 등록된 알림 채널 목록
     */
    public NotificationChannelFactory(List<NotificationChannel> channelList) {
        this.channels = channelList.stream()
                .collect(Collectors.toMap(
                        NotificationChannel::getChannelType,
                        Function.identity()
                ));
    }

    /**
     * 채널 타입에 따른 알림 채널 반환
     * 
     * @param channelType 채널 타입 (예: "SLACK", "EMAIL", "DISCORD")
     * @return 알림 채널 인스턴스 (없으면 null)
     */
    public NotificationChannel getChannel(String channelType) {
        return channels.get(channelType);
    }

    /**
     * 지원하는 채널 타입 목록 반환
     * 
     * @return 지원하는 채널 타입 목록
     */
    public List<String> getSupportedChannelTypes() {
        return channels.keySet().stream().collect(Collectors.toList());
    }

    /**
     * 채널이 지원되는지 확인
     * 
     * @param channelType 채널 타입
     * @return 지원 여부
     */
    public boolean isChannelSupported(String channelType) {
        return channels.containsKey(channelType);
    }
}
