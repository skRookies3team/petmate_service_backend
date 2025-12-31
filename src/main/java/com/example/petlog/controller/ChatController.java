package com.example.petlog.controller;

import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    // 클라이언트가 '/app/chat/send'로 전송하면 실행됨
    @MessageMapping("/chat/send")
    public void sendMessage(MessageRequest request) {
        log.info("Socket 메시지 수신: room={}, sender={}", request.getChatRoomId(), request.getSenderId());

        // 1. DB 저장
        MessageResponse response = messageService.saveMessage(request);

        // 2. 해당 방을 구독 중인 사용자들에게 브로드캐스팅 (/topic/chat/{roomId})
        messagingTemplate.convertAndSend("/topic/chat/" + request.getChatRoomId(), response);
    }
}