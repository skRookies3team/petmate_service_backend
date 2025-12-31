package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MessageResponse {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}