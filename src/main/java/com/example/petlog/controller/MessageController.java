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
@RequestMapping("/api/messages") // 기본 경로 확인: /api/messages
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    // ==========================================
    //  HTTP API Endpoints
    // ==========================================

    /**
     * 1. 내 채팅방 목록 조회
     * URL: GET /api/messages/rooms/{userId}
     * [중요] 'rooms' (복수형)
     */
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@PathVariable("userId") Long userId) {
        System.out.println("🔍 [API] 채팅방 목록 조회 요청 - UserID: {}"+ userId.toString());
        return ResponseEntity.ok(messageService.getChatRooms(userId));
    }

    /**
     * 2. 특정 채팅방 메시지 내역 조회
     * URL: GET /api/messages/room/{chatRoomId}
     * [중요] 'room' (단수형)
     */
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam("userId") Long userId) {

        log.info("🔍 [API] 메시지 내역 조회 요청 - RoomID: {}, UserID: {}", chatRoomId, userId);
        return ResponseEntity.ok(messageService.getMessages(chatRoomId, userId));
    }

    /**
     * 3. 채팅방 생성 또는 조회
     * URL: POST /api/messages/room
     */
    @PostMapping("/room")
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
            @RequestParam("userId1") Long userId1,
            @RequestParam("userId2") Long userId2) {
        log.info("➕ [API] 채팅방 생성/조회 - User1: {}, User2: {}", userId1, userId2);
        return ResponseEntity.ok(messageService.createOrGetChatRoom(userId1, userId2));
    }

    /**
     * 4. 메시지 읽음 처리
     * URL: PUT /api/messages/room/{chatRoomId}/read
     */
    @PutMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam("userId") Long userId) {
        messageService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. 메시지 전송 (REST)
     * URL: POST /api/messages/send
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessageRest(@RequestBody MessageRequest request) {
        MessageResponse response = messageService.sendMessage(request);
        try {
            messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);
        } catch (Exception e) {
            log.error("소켓 전송 실패", e);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 6. 안 읽은 메시지 수 (방 별)
     */
    @GetMapping("/room/{chatRoomId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(messageService.getUnreadCount(chatRoomId, userId));
    }

    /**
     * 7. 전체 안 읽은 메시지 수 (배지)
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<Long> getTotalUnreadCount(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(messageService.getTotalUnreadCount(userId));
    }

    /**
     * 8. 채팅방 나가기 (삭제)
     * URL: DELETE /api/messages/room/{chatRoomId}
     */
    @DeleteMapping("/room/{chatRoomId}")
    public ResponseEntity<Void> leaveChatRoom(
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam("userId") Long userId) {
        log.info("🗑️ [API] 채팅방 나가기 요청 - RoomID: {}, UserID: {}", chatRoomId, userId);
        messageService.leaveChatRoom(chatRoomId, userId);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    //  WebSocket Handler
    // ==========================================

    @MessageMapping("/chat/message")
    public void handleSocketMessage(MessageRequest request) {
        log.info("📨 [Socket] 메시지 수신: {}", request.getContent());
        MessageResponse response = messageService.sendMessage(request);
        messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);
    }
}