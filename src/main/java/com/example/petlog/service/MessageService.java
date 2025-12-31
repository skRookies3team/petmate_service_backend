package com.example.petlog.service;

import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.entity.ChatRoom;
import com.example.petlog.entity.Message;
import com.example.petlog.entity.PetMate;
import com.example.petlog.repository.ChatRoomRepository;
import com.example.petlog.repository.MessageRepository;
import com.example.petlog.repository.PetMateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final PetMateRepository petMateRepository;

    // 1:1 채팅방 생성 또는 조회 (매칭 시 호출됨)
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long userId1, Long userId2) {
        ChatRoom chatRoom = chatRoomRepository.findByUsers(userId1, userId2)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .user1Id(userId1)
                        .user2Id(userId2)
                        .isActive(true)
                        .lastMessage("새로운 매칭이 시작되었습니다! 👋")
                        .lastMessageAt(LocalDateTime.now())
                        .build()));

        return convertToChatRoomResponse(chatRoom, userId1);
    }

    // 메시지 저장 (DB 저장 후 리턴)
    @Transactional
    public MessageResponse saveMessage(MessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .messageType(Message.MessageType.valueOf(request.getMessageType() != null ? request.getMessageType() : "TEXT"))
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);

        // 채팅방의 마지막 메시지 업데이트
        chatRoom.setLastMessage(request.getContent());
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        return convertToMessageResponse(saved);
    }

    // 내 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        return chatRoomRepository.findActiveByUserId(userId).stream()
                .map(room -> convertToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    // 특정 채팅방의 메시지 내역 조회
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long chatRoomId, Long userId) {
        // (선택) 여기서 읽음 처리 로직을 호출하거나, 별도 API로 분리할 수 있음
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId).stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    // 메시지 읽음 처리
    @Transactional
    public void markAsRead(Long chatRoomId, Long userId) {
        messageRepository.markAsRead(chatRoomId, userId);
    }

    // DTO 변환 메서드 (채팅방)
    private ChatRoomResponse convertToChatRoomResponse(ChatRoom room, Long myId) {
        Long otherId = room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();
        PetMate otherUser = petMateRepository.findFirstByUserIdOrderByIdAsc(otherId).orElse(null);

        return ChatRoomResponse.builder()
                .id(room.getId())
                .otherUserId(otherId)
                .otherUserName(otherUser != null ? otherUser.getUserName() : "Unknown")
                .otherUserAvatar(otherUser != null ? otherUser.getUserAvatar() : null)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(messageRepository.countUnreadMessages(room.getId(), myId))
                .build();
    }

    // DTO 변환 메서드 (메시지)
    private MessageResponse convertToMessageResponse(Message msg) {
        PetMate sender = petMateRepository.findFirstByUserIdOrderByIdAsc(msg.getSenderId()).orElse(null);
        return MessageResponse.builder()
                .id(msg.getId())
                .chatRoomId(msg.getChatRoom().getId())
                .senderId(msg.getSenderId())
                .senderName(sender != null ? sender.getUserName() : "Unknown")
                .senderAvatar(sender != null ? sender.getUserAvatar() : null)
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}