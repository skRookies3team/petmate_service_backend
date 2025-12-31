package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRequestResponse {
    private Long matchId;
    private Long fromUserId;
    private String fromUserName;
    private String fromUserAvatar;
    private String petName;
    private String petPhoto;
    private Integer matchScore;
    private LocalDateTime createdAt;
}