package com.example.petlog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // [수정] setAllowedOriginPatterns("*")로 모든 출처 허용 (Gateway 뒤에 있으므로 안전)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 여기가 핵심입니다.
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 구독 요청 url -> /sub
        registry.enableSimpleBroker("/sub");
        // 메시지 발행 요청 url -> /pub
        registry.setApplicationDestinationPrefixes("/pub");
    }
}