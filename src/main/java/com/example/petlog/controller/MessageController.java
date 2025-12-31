package com.example.petlog.controller;

import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/messages") // Gateway 설정에 따라 경로는 유동적 (/api/petmate/messages 등)
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송 도구

    // ==========================================
    //  1. WebSocket (Real-time) Section
    // ==========================================

    /**
     * WebSocket 메시지 전송 핸들러
     * 클라이언트가 '/pub/chat/message'로 전송하면 이 메서드가 실행됨
     */
    @MessageMapping("/chat/message")
    public void handleSocketMessage(MessageRequest request) {
        log.info("WebSocket Message Received: {}", request.getContent());

        // 1. DB 저장 (기존 Service 로직 재사용)
        MessageResponse response = messageService.sendMessage(request);

        // 2. 해당 채팅방 구독자들에게 실시간 브로드캐스트
        // 구독 경로: /sub/chat/room/{chatRoomId}
        messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);
    }

    // ==========================================
    //  2. REST API (HTTP) Section
    // ==========================================

    /**
     * 채팅방 생성 또는 조회
     */
    @PostMapping("/room")
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
            @RequestParam Long userId1,
            @RequestParam Long userId2) {
        return ResponseEntity.ok(messageService.createOrGetChatRoom(userId1, userId2));
    }

    /**
     * 사용자의 모든 채팅방 목록 조회 (화면 진입 시 로딩용)
     */
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getChatRooms(userId));
    }

    /**
     * 채팅방 메시지 목록 조회 (채팅방 입장 시 과거 대화 로딩)
     */
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        // 메시지 내역 조회
        List<MessageResponse> messages = messageService.getMessages(chatRoomId, userId);

        // 입장했으므로 읽음 처리까지 같이 수행 (선택사항)
        messageService.markMessagesAsRead(chatRoomId, userId);

        return ResponseEntity.ok(messages);
    }

    /**
     * 최근 메시지 조회 (페이징)
     */
    @GetMapping("/room/{chatRoomId}/recent")
    public ResponseEntity<List<MessageResponse>> getRecentMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(messageService.getRecentMessages(chatRoomId, userId, limit));
    }

    /**
     * 메시지 전송 (REST API 버전)
     * - 파일 업로드나, 소켓 연결이 불안정한 경우 HTTP로 전송할 때 사용
     * - 여기서 보내도 소켓 구독자들에게 실시간 알림이 가도록 처리함
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessageRest(@RequestBody MessageRequest request) {
        // 1. DB 저장
        MessageResponse response = messageService.sendMessage(request);

        // 2. ★ 중요: HTTP로 요청이 왔어도, 소켓 연결된 다른 사용자에게 실시간으로 보여야 함
        messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);

        return ResponseEntity.ok(response);
    }

    /**
     * 메시지 읽음 처리
     */
    @PutMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        messageService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 읽지 않은 메시지 수 조회
     */
    @GetMapping("/room/{chatRoomId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(messageService.getUnreadCount(chatRoomId, userId));
    }

    /**
     * 전체 읽지 않은 메시지 수 조회 (네비게이션 바 배지용)
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<Long> getTotalUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getTotalUnreadCount(userId));
    }
}