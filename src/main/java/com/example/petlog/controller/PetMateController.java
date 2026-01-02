package com.example.petlog.controller;

import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.MatchResponse;
import com.example.petlog.dto.response.PendingRequestResponse;
import com.example.petlog.dto.response.PetMateResponse;
import com.example.petlog.service.PetMateService;
import lombok.Data; // [추가]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/petmate")
public class PetMateController {

    private final PetMateService petMateService;

    // --- [1. DTO 추가] 응답 요청 데이터를 받기 위한 그릇 ---
    @Data
    public static class RespondRequest {
        private Long userId;
        private boolean accept;
    }
    // ----------------------------------------------------

    // [수정됨] 경로를 /requests/{matchId}/respond 로 변경하고 @RequestBody 적용
    @PostMapping("/requests/{matchId}/respond")
    public ResponseEntity<MatchResponse> respondToRequest(
            @PathVariable Long matchId,
            @RequestBody RespondRequest request) { // @RequestParam -> @RequestBody로 변경

        return ResponseEntity.ok(
                petMateService.respondToRequest(matchId, request.getUserId(), request.isAccept())
        );
    }

    // 1. 후보 추천 조회
    @PostMapping("/candidates/{userId}")
    public ResponseEntity<List<PetMateResponse>> getCandidates(
            @PathVariable Long userId,
            @RequestBody PetMateFilterRequest filter) {
        return ResponseEntity.ok(petMateService.getCandidates(userId, filter));
    }

    // 2. 좋아요 요청
    @PostMapping("/like")
    public ResponseEntity<MatchResponse> like(@RequestBody LikeRequest request) {
        return ResponseEntity.ok(petMateService.like(request));
    }

    // 3. 좋아요 취소 (Unlike)
    @PostMapping("/unlike")
    public ResponseEntity<Boolean> unlike(@RequestBody LikeRequest request) {
        return ResponseEntity.ok(petMateService.unlike(request));
    }

    // 4. 내가 보낸 요청 목록
    @GetMapping("/requests/{userId}/sent")
    public ResponseEntity<List<PendingRequestResponse>> getSentRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getSentRequests(userId));
    }

    // 5. 나에게 온 요청 목록
    @GetMapping("/requests/{userId}")
    public ResponseEntity<List<PendingRequestResponse>> getPendingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getPendingRequests(userId));
    }

    // 6. 나에게 온 요청 개수 (배지용)
    @GetMapping("/requests/{userId}/count")
    public ResponseEntity<Long> getPendingRequestsCount(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getPendingRequestsCount(userId));
    }

    // 8. 매칭된 친구 목록
    @GetMapping("/matches/{userId}")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getMatches(userId));
    }

    // 9. 내가 좋아요한 유저 ID 목록
    @GetMapping("/liked/{userId}")
    public ResponseEntity<List<Long>> getLikedUserIds(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getLikedUserIds(userId));
    }

    // 10. 친구 끊기 (Unfriend)
    @DeleteMapping("/matches/{userId}/{matchedUserId}")
    public ResponseEntity<Boolean> unfriend(
            @PathVariable Long userId,
            @PathVariable Long matchedUserId) {
        return ResponseEntity.ok(petMateService.unfriend(userId, matchedUserId));
    }

    // 11. 저장된 위치 조회
    @GetMapping("/location/{userId}")
    public ResponseEntity<PetMateResponse> getSavedLocation(@PathVariable Long userId) {
        PetMateResponse response = petMateService.getSavedLocation(userId);
        return ResponseEntity.ok(response);
    }

    // 12. 위치 업데이트
    @PutMapping("/location/{userId}")
    public ResponseEntity<Boolean> updateLocation(
            @PathVariable Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(petMateService.updateLocation(userId, latitude, longitude, location));
    }

    // 13. 온라인 상태 업데이트
    @PostMapping("/status/{userId}")
    public ResponseEntity<Void> updateOnlineStatus(
            @PathVariable Long userId,
            @RequestParam boolean isOnline) {
        petMateService.updateOnlineStatus(userId, isOnline);
        return ResponseEntity.ok().build();
    }
}