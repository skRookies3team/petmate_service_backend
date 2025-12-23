package com.example.petlog.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    private Long chatRoomId;
    private Long senderId;
    private String content;
    private String messageType; // TEXT, IMAGE, EMOJI
}
