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
@RequestMapping("/api/messages") // Gateway 경로와 일치시킴
@RequiredArgsConstructor
// @CrossOrigin("*") // 로컬 테스트 시 CORS 문제가 생기면 주석 해제
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

        // 1. DB 저장 (Service에서 DTO 변환 및 Null 체크 완료됨)
        MessageResponse response = messageService.sendMessage(request);

        // 2. 해당 채팅방 구독자들에게 실시간 브로드캐스트
        // 구독 경로: /sub/chat/room/{chatRoomId}
        messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);
    }

    // ==========================================
    //  2. REST API (HTTP) Section
    // ==========================================

    /**
     * 1. 채팅방 생성 또는 조회
     * 요청: POST /api/messages/room?userId1=1&userId2=2
     */
    @PostMapping("/room")
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
            @RequestParam Long userId1,
            @RequestParam Long userId2) {
        return ResponseEntity.ok(messageService.createOrGetChatRoom(userId1, userId2));
    }

    /**
     * 2. 내 채팅방 목록 조회
     * 요청: GET /api/messages/rooms/{userId}
     */
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getChatRooms(userId));
    }

    /**
     * 3. 특정 채팅방 메시지 내역 조회
     * 요청: GET /api/messages/room/{chatRoomId}?userId={userId}
     */
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {

        // 메시지 내역 조회
        List<MessageResponse> messages = messageService.getMessages(chatRoomId, userId);

        // 입장했으므로 읽음 처리까지 같이 수행 (선택사항, 프론트 로직에 따라 다름)
        try {
            messageService.markMessagesAsRead(chatRoomId, userId);
        } catch (Exception e) {
            log.error("메시지 읽음 처리 중 오류 (무시 가능): {}", e.getMessage());
        }

        return ResponseEntity.ok(messages);
    }

    /**
     * 4. 최근 메시지 조회 (페이징 필요 시 사용)
     */
    @GetMapping("/room/{chatRoomId}/recent")
    public ResponseEntity<List<MessageResponse>> getRecentMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(messageService.getRecentMessages(chatRoomId, userId, limit));
    }

    /**
     * 5. 메시지 전송 (REST API 버전 - 파일 전송 등 대비)
     * 요청: POST /api/messages/send (Body: JSON)
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessageRest(@RequestBody MessageRequest request) {
        // 1. DB 저장
        MessageResponse response = messageService.sendMessage(request);

        // 2. ★ 중요: HTTP로 요청이 왔어도, 소켓 연결된 다른 사용자에게 실시간으로 보여야 함
        try {
            messagingTemplate.convertAndSend("/sub/chat/room/" + response.getChatRoomId(), response);
        } catch (Exception e) {
            log.error("소켓 전송 실패 (DB는 저장됨): {}", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 6. 메시지 읽음 처리 (명시적 호출)
     * 요청: PUT /api/messages/room/{chatRoomId}/read?userId={userId}
     */
    @PutMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        messageService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 7. 읽지 않은 메시지 수 조회 (특정 방)
     */
    @GetMapping("/room/{chatRoomId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(messageService.getUnreadCount(chatRoomId, userId));
    }

    /**
     * 8. 전체 읽지 않은 메시지 수 조회 (네비게이션 바 배지용)
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<Long> getTotalUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getTotalUnreadCount(userId));
    }
}