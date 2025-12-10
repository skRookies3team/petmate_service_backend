package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {

    private Long matchId;
    private Long matchedUserId;
    private String matchedUserName;
    private String matchedUserAvatar;
    private String petName;
    private String petPhoto;
    private Integer matchScore;
    private Boolean isMatched;
    private LocalDateTime matchedAt;
    private Long chatRoomId;
    private Boolean alreadyLiked;
}
