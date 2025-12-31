package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRequestResponse {

    private Long matchId;
    private Long fromUserId;
    private String fromUserName;
    private String fromUserAvatar;
    private String petName;
    private String petPhoto;
    private String petBreed;
    private Integer petAge;
    private String location;
    private LocalDateTime createdAt;
}
