package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.enums.ChannelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 채널 팩토리
 * 확장 가능한 채널 관리
 */
@Component
public class NotificationChannelFactory {

    private final Map<String, NotificationChannel> channels;

    @Autowired
    public NotificationChannelFactory(List<NotificationChannel> channelList) {
        this.channels = channelList.stream()
                .collect(Collectors.toMap(
                        NotificationChannel::getChannelType,
                        Function.identity()
                ));
    }

    /**
     * 채널 타입에 따른 알림 채널 반환
     */
    public NotificationChannel getChannel(String channelType) {
        return channels.get(channelType);
    }

    /**
     * 지원하는 채널 타입 목록 반환
     */
    public List<String> getSupportedChannelTypes() {
        return channels.keySet().stream().collect(Collectors.toList());
    }

    /**
     * 채널이 지원되는지 확인
     */
    public boolean isChannelSupported(String channelType) {
        return channels.containsKey(channelType);
    }
}
