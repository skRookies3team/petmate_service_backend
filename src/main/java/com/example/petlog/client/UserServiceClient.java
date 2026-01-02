package com.example.petlog.client;

import com.example.petlog.dto.response.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// [수정] url 설정에 기본값 추가 (설정 파일에 값이 없으면 localhost:8000 사용)
// 8000번(Gateway)으로 보내면 Gateway가 알아서 'user-service'로 라우팅해줍니다.
@FeignClient(name = "user-service", url = "${external.user-service.url:http://localhost:8000}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}/exists")
    boolean checkUserExists(@PathVariable("userId") Long userId);

    /**
     * 사용자 상세 정보 조회 (펫 정보 포함)
     */
    @GetMapping("/api/users/{userId}")
    UserInfoResponse getUserInfo(@PathVariable("userId") Long userId);
}