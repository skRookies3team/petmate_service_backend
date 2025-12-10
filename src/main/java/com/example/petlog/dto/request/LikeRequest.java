package com.example.petlog.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeRequest {

    private Long fromUserId;
    private Long toUserId;
}
