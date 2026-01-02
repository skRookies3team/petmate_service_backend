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
import java.util.ArrayList;
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
        try {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findByUser1IdAndUser2Id(userId1, userId2);
            if (roomOpt.isEmpty()) {
                roomOpt = chatRoomRepository.findByUser2IdAndUser1Id(userId1, userId2);
            }

            return roomOpt
                    .map(room -> convertToChatRoomResponse(room, userId1))
                    .orElseGet(() -> {
                        ChatRoom room = ChatRoom.builder()
                                .user1Id(userId1)
                                .user2Id(userId2)
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        ChatRoom savedRoom = chatRoomRepository.save(room);
                        return convertToChatRoomResponse(savedRoom, userId1);
                    });
        } catch (Exception e) {
            log.error("채팅방 생성 에러: ", e);
            throw new RuntimeException("채팅방 생성 실패");
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getChatRooms(Long userId) {
        System.out.println("============ [Debug] getChatRooms 시작 ============");
        try {
            // 1. DB 조회
            System.out.println("[Debug] 1. DB 조회 시도 (UserId: " + userId + ")");
            List<ChatRoom> rooms = chatRoomRepository.findMyChatRooms(userId);
            System.out.println("[Debug] 2. DB 조회 성공. 방 개수: " + (rooms != null ? rooms.size() : "NULL"));

            if (rooms == null) return new ArrayList<>();

            // 2. 정렬
            System.out.println("[Debug] 3. 정렬 시작");
            rooms.sort((r1, r2) -> {
                LocalDateTime t1 = r1.getLastMessageAt() != null ? r1.getLastMessageAt() : r1.getCreatedAt();
                LocalDateTime t2 = r2.getLastMessageAt() != null ? r2.getLastMessageAt() : r2.getCreatedAt();
                if (t1 == null) t1 = LocalDateTime.now();
                if (t2 == null) t2 = LocalDateTime.now();
                return t2.compareTo(t1);
            });

            // 3. 변환
            System.out.println("[Debug] 4. DTO 변환 시작");
            List<ChatRoomResponse> responseList = new ArrayList<>();
            for (ChatRoom room : rooms) {
                try {
                    System.out.println("   -> 방 ID " + room.getId() + " 변환 중...");
                    responseList.add(convertToChatRoomResponse(room, userId));
                } catch (Exception innerEx) {
                    System.err.println("   -> [ERROR] 방 ID " + room.getId() + " 변환 실패: " + innerEx.getMessage());
                    innerEx.printStackTrace(); // 에러 스택 출력
                }
            }

            System.out.println("============ [Debug] getChatRooms 종료 (성공) ============");
            return responseList;

        } catch (Exception e) {
            System.err.println("============ [CRITICAL ERROR] getChatRooms 터짐 ============");
            e.printStackTrace(); // 콘솔에 빨간 글씨로 에러 원인 출력
            return new ArrayList<>(); // 죽지 않고 빈 리스트 반환
        }
    }

    // 3. 메시지 전송
    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        chatRoom.setLastMessage(request.getContent());
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .messageType(Message.MessageType.TEXT)
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

    // 기타 메서드들
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentMessages(Long chatRoomId, Long userId, int limit) {
        return messageRepository.findTop50ByChatRoomIdOrderByCreatedAtDesc(chatRoomId).stream()
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        List<Message> unreadMessages = messageRepository.findByChatRoomIdAndIsReadFalse(chatRoomId);
        List<Message> toUpdate = unreadMessages.stream()
                .filter(m -> !m.getSenderId().equals(userId))
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
        try {
            // [수정] 여기도 @Query 메서드 사용
            return chatRoomRepository.findMyChatRooms(userId).stream()
                    .mapToLong(room -> getUnreadCount(room.getId(), userId))
                    .sum();
        } catch (Exception e) {
            return 0L;
        }
    }

    // DTO 변환 로직 (안전 장치 포함)
    private ChatRoomResponse convertToChatRoomResponse(ChatRoom room, Long currentUserId) {
        Long otherUserId = room.getUser1Id().equals(currentUserId) ? room.getUser2Id() : room.getUser1Id();

        String otherUserName = "User " + otherUserId;
        String otherUserAvatar = null;
        String petName = "반려동물";

        try {
            UserInfoResponse userInfo = userServiceClient.getUserInfo(otherUserId);
            if (userInfo != null) {
                otherUserName = userInfo.getUsername();
                otherUserAvatar = userInfo.getProfileImage();
                if (userInfo.getPets() != null && !userInfo.getPets().isEmpty()) {
                    petName = userInfo.getPets().get(0).getPetName();
                }
            }
        } catch (Exception e) {
            // User Service 장애 시 로그만 남기고 진행
            log.warn("UserService 연동 실패 (ID: {}).", otherUserId);
        }

        String lastMessageAtStr = null;
        if (room.getLastMessageAt() != null) {
            lastMessageAtStr = room.getLastMessageAt().toString();
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

        return MessageResponse.builder()
                .id(msg.getId())
                .chatRoomId(msg.getChatRoom().getId())
                .senderId(msg.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null)
                .build();
    }
}