package com.example.petlog.client;

import com.example.petlog.dto.response.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// User Service 호출
@FeignClient(name = "user-service", url = "${external.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}/exists")
    boolean checkUserExists(@PathVariable("userId") Long userId);

    /**
     * 사용자 상세 정보 조회 (펫 정보 포함)
     */
    @GetMapping("/api/users/{userId}")
    UserInfoResponse getUserInfo(@PathVariable("userId") Long userId);
}
