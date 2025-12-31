package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatRoomResponse {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}