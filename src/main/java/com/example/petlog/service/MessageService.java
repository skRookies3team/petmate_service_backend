package com.example.petlog.service;

import com.example.petlog.client.UserServiceClient;
import com.example.petlog.dto.request.MessageRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MessageResponse;
import com.example.petlog.dto.response.UserInfoResponse;
import com.example.petlog.entity.ChatRoom;
import com.example.petlog.entity.Message;
import com.example.petlog.repository.ChatRoomRepository;
import com.example.petlog.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserServiceClient userServiceClient;

    // 1. 채팅방 생성 또는 조회
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long userId1, Long userId2) {
        // (A) user1-user2 순서로 찾기
        Optional<ChatRoom> roomOpt = chatRoomRepository.findByUser1IdAndUser2Id(userId1, userId2);

        // (B) 없으면 user2-user1 순서로 찾기
        if (roomOpt.isEmpty()) {
            roomOpt = chatRoomRepository.findByUser2IdAndUser1Id(userId1, userId2);
        }

        return roomOpt
                .map(room -> convertToChatRoomResponse(room, userId1))
                .orElseGet(() -> {
                    // (C) 둘 다 없으면 새로 생성 (테이블: chat_room)
                    ChatRoom room = ChatRoom.builder()
                            .user1Id(userId1)
                            .user2Id(userId2)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    chatRoomRepository.save(room);
                    return convertToChatRoomResponse(room, userId1);
                });
    }

    // 2. 내 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getChatRooms(Long userId) {
        // (A) 단순 조회 (테이블: chat_room)
        // isActive 체크 없이 일단 다 가져오거나, 필요하면 AndIsActiveTrue 메서드 사용
        List<ChatRoom> rooms = chatRoomRepository.findByUser1IdOrUser2Id(userId, userId);

        // (B) 자바에서 최신순 정렬 (Null Safe - DB 쿼리 에러 방지)
        rooms.sort((r1, r2) -> {
            LocalDateTime t1 = r1.getLastMessageAt() != null ? r1.getLastMessageAt() : r1.getCreatedAt();
            LocalDateTime t2 = r2.getLastMessageAt() != null ? r2.getLastMessageAt() : r2.getCreatedAt();

            // 혹시라도 createdAt조차 null인 경우 현재시간 처리 (방어 코드)
            if (t1 == null) t1 = LocalDateTime.now();
            if (t2 == null) t2 = LocalDateTime.now();

            return t2.compareTo(t1); // 내림차순
        });

        return rooms.stream()
                .map(room -> convertToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    // 3. 메시지 전송
    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        // 채팅방 정보 업데이트
        chatRoom.setLastMessage(request.getContent());
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // 메시지 저장 (테이블: chat_messages)
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .messageType(Message.MessageType.TEXT) // Enum 기본값 안전 처리
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);
        return convertToMessageResponse(saved);
    }

    // 4. 메시지 내역 조회
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long chatRoomId, Long userId) {
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId).stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    // 5. 최근 메시지 조회 (페이징)
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentMessages(Long chatRoomId, Long userId, int limit) {
        return messageRepository.findTop50ByChatRoomIdOrderByCreatedAtDesc(chatRoomId).stream()
                .sorted((m1, m2) -> {
                    LocalDateTime t1 = m1.getCreatedAt() != null ? m1.getCreatedAt() : LocalDateTime.now();
                    LocalDateTime t2 = m2.getCreatedAt() != null ? m2.getCreatedAt() : LocalDateTime.now();
                    return t1.compareTo(t2);
                })
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    // 6. 읽음 처리
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        List<Message> unreadMessages = messageRepository.findByChatRoomIdAndIsReadFalse(chatRoomId);

        List<Message> toUpdate = unreadMessages.stream()
                .filter(m -> !m.getSenderId().equals(userId)) // 내가 보낸 건 읽음처리 안 함
                .peek(m -> m.setIsRead(true))
                .collect(Collectors.toList());

        if (!toUpdate.isEmpty()) {
            messageRepository.saveAll(toUpdate);
        }
    }

    public Long getUnreadCount(Long chatRoomId, Long userId) {
        return messageRepository.countByChatRoomIdAndIsReadFalseAndSenderIdNot(chatRoomId, userId);
    }

    public Long getTotalUnreadCount(Long userId) {
        return chatRoomRepository.findByUser1IdOrUser2Id(userId, userId).stream()
                .mapToLong(room -> getUnreadCount(room.getId(), userId))
                .sum();
    }

    // =========================================================
    //  DTO 변환 (User Service 죽었을 때 500 에러 절대 방지)
    // =========================================================

    private ChatRoomResponse convertToChatRoomResponse(ChatRoom room, Long currentUserId) {
        Long otherUserId = room.getUser1Id().equals(currentUserId) ? room.getUser2Id() : room.getUser1Id();

        String otherUserName = "사용자 " + otherUserId;
        String otherUserAvatar = null;
        String petName = "반려동물";

        try {
            // ★ 중요: User Service가 죽어있어도 500 에러를 내지 않고 기본값으로 리턴
            UserInfoResponse userInfo = userServiceClient.getUserInfo(otherUserId);
            if (userInfo != null) {
                otherUserName = userInfo.getUsername();
                otherUserAvatar = userInfo.getProfileImage();
                if (userInfo.getPets() != null && !userInfo.getPets().isEmpty()) {
                    petName = userInfo.getPets().get(0).getPetName();
                }
            }
        } catch (Exception e) {
            log.warn("User Service 연동 실패 (ID: {}). 기본값 사용.", otherUserId);
        }

        String lastMessageAtStr = null;
        if (room.getLastMessageAt() != null) {
            lastMessageAtStr = room.getLastMessageAt().toString();
        } else if (room.getCreatedAt() != null) {
            lastMessageAtStr = room.getCreatedAt().toString();
        }

        return ChatRoomResponse.builder()
                .id(room.getId())
                .otherUserId(otherUserId)
                .otherUserName(otherUserName)
                .otherUserAvatar(otherUserAvatar)
                .petName(petName)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(lastMessageAtStr)
                .unreadCount(getUnreadCount(room.getId(), currentUserId))
                .build();
    }

    private MessageResponse convertToMessageResponse(Message msg) {
        String senderName = "User " + msg.getSenderId();
        String senderAvatar = null;

        try {
            UserInfoResponse userInfo = userServiceClient.getUserInfo(msg.getSenderId());
            if (userInfo != null) {
                senderName = userInfo.getUsername();
                senderAvatar = userInfo.getProfileImage();
            }
        } catch (Exception e) {
            // 무시
        }

        String createdAtStr = null;
        if (msg.getCreatedAt() != null) {
            createdAtStr = msg.getCreatedAt().toString();
        }

        return MessageResponse.builder()
                .id(msg.getId())
                .chatRoomId(msg.getChatRoom().getId())
                .senderId(msg.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead())
                .createdAt(createdAtStr)
                .build();
    }
}