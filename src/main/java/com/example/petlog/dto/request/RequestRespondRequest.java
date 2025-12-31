package com.example.petlog.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestRespondRequest {

    private Long matchId;
    private Long userId;
    private Boolean accept;
}
