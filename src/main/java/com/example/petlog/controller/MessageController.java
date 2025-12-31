package com.example.petlog.controller;

import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/petmate/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 내 채팅방 목록 조회
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getMyChatRooms(userId));
    }

    // 특정 채팅방 메시지 내역 조회
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(messageService.getMessages(chatRoomId, userId));
    }

    // 메시지 읽음 처리 (채팅방 입장 시 호출)
    @PutMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {
        messageService.markAsRead(chatRoomId, userId);
        return ResponseEntity.ok().build();
    }
}