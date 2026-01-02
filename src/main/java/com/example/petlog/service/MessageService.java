package com.example.petlog.service;

import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.entity.ChatRoom;
import com.example.petlog.entity.Message;
import com.example.petlog.entity.PetMate;
import com.example.petlog.repository.ChatRoomRepository;
import com.example.petlog.repository.MessageRepository;
import com.example.petlog.repository.PetMateMatchRepository;
import com.example.petlog.repository.PetMateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final PetMateMatchRepository petMateMatchRepository;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long userId1, Long userId2) {
        // 이미 존재하는 채팅방 확인
        return chatRoomRepository.findByUsers(userId1, userId2)
                .map(room -> convertToChatRoomResponse(room, userId1))
                .orElseGet(() -> {
                    // 매칭 여부 확인 (매칭된 유저끼리만 채팅 가능)
                    if (!petMateMatchRepository.isMatched(userId1, userId2)) {
                        throw new IllegalArgumentException("채팅을 시작할 수 없습니다. 서로 매칭된 친구가 아닙니다.");
                    }

                    ChatRoom newRoom = ChatRoom.builder()
                            .user1Id(userId1)
                            .user2Id(userId2)
                            .isActive(true)
                            .build();
                    return convertToChatRoomResponse(chatRoomRepository.save(newRoom), userId1);
                });
    }

    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // 권한 확인
        if (!chatRoom.getUser1Id().equals(userId) && !chatRoom.getUser2Id().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this chat room");
        }

        chatRoomRepository.delete(chatRoom);
    }

    public List<ChatRoomResponse> getChatRooms(Long userId) {
        return chatRoomRepository.findActiveByUserId(userId).stream()
                .map(room -> convertToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getMessages(Long chatRoomId, Long userId) {
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId).stream()
                .map(msg -> convertToMessageResponse(msg, userId))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getRecentMessages(Long chatRoomId, Long userId, int limit) {
        return messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(msg -> convertToMessageResponse(msg, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .messageType(Message.MessageType.valueOf(
                        request.getMessageType() != null ? request.getMessageType() : "TEXT"))
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);

        // Update chat room's last message
        chatRoom.setLastMessage(request.getContent());
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        return convertToMessageResponse(saved, request.getSenderId());
    }

    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        messageRepository.markAsRead(chatRoomId, userId);
    }

    public Long getUnreadCount(Long chatRoomId, Long userId) {
        return messageRepository.countUnreadMessages(chatRoomId, userId);
    }

    public Long getTotalUnreadCount(Long userId) {
        return chatRoomRepository.findActiveByUserId(userId).stream()
                .mapToLong(room -> messageRepository.countUnreadMessages(room.getId(), userId))
                .sum();
    }

    private ChatRoomResponse convertToChatRoomResponse(ChatRoom room, Long currentUserId) {
        Long otherUserId = room.getUser1Id().equals(currentUserId) ? room.getUser2Id() : room.getUser1Id();
        PetMate otherUser = petMateRepository.findFirstByUserIdOrderByIdAsc(otherUserId).orElse(null);

        return ChatRoomResponse.builder()
                .id(room.getId())
                .otherUserId(otherUserId)
                .otherUserName(otherUser != null ? otherUser.getUserName() : "Unknown")
                .otherUserAvatar(otherUser != null ? otherUser.getUserAvatar() : null)
                .petName(otherUser != null ? otherUser.getPetName() : null)
                .petPhoto(otherUser != null ? otherUser.getPetPhoto() : null)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(messageRepository.countUnreadMessages(room.getId(), currentUserId))
                .build();
    }

    private MessageResponse convertToMessageResponse(Message message, Long currentUserId) {
        PetMate sender = petMateRepository.findFirstByUserIdOrderByIdAsc(message.getSenderId()).orElse(null);

        return MessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getUserName() : "Unknown")
                .senderAvatar(sender != null ? sender.getUserAvatar() : null)
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
