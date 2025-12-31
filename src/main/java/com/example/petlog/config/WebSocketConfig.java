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
        // 프론트엔드에서 연결할 엔드포인트: ws://localhost:8089/ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 모든 출처 허용 (보안 필요시 프론트 주소로 변경)
                .withSockJS(); // SockJS 지원 (브라우저 호환성)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 구독 경로 (클라이언트가 메시지를 받는 곳)
        // /topic/chat/{roomId} 형태로 사용
        registry.enableSimpleBroker("/topic");

        // 메시지 발행 경로 (클라이언트가 메시지를 보내는 곳)
        // /app/chat/send 로 보내면 @MessageMapping이 처리
        registry.setApplicationDestinationPrefixes("/app");
    }
}