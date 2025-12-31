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
        // Gateway(8000)를 통해 들어오므로, AllowedOrigins 패턴을 넓게 잡거나 Gateway 주소를 명시
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독(Subscribe)할 prefix (1:1 채팅방)
        registry.enableSimpleBroker("/sub");
        // 클라이언트가 메시지 보낼(Publish) prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }
}