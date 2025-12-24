package com.example.petlog.controller;

import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

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
     * 사용자의 모든 채팅방 목록 조회
     */
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getChatRooms(userId));
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(messageService.getMessages(chatRoomId, userId));
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
     * 메시지 전송
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@RequestBody MessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(request));
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
     * 전체 읽지 않은 메시지 수 조회
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<Long> getTotalUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getTotalUnreadCount(userId));
    }
}
