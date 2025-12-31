package com.example.petlog.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestRespondRequest {
    private Long userId;   // 응답하는 사용자 ID (로그인한 사용자)
    private Boolean accept; // true: 수락, false: 거절
}