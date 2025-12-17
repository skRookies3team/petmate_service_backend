package com.example.petlog.controller;

import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.MatchResponse;
import com.example.petlog.dto.response.PetMateResponse;
import com.example.petlog.service.PetMateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/petmate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PetMateController {

    private final PetMateService petMateService;

    /**
     * 펫메이트 프로필 생성/수정
     */
    @PostMapping("/profile")
    public ResponseEntity<PetMateResponse> createOrUpdateProfile(@RequestBody PetMateRequest request) {
        return ResponseEntity.ok(petMateService.createOrUpdateProfile(request));
    }

    /**
     * 매칭 후보자 목록 조회 (필터 적용)
     */
    @PostMapping("/candidates/{userId}")
    public ResponseEntity<List<PetMateResponse>> getCandidates(
            @PathVariable Long userId,
            @RequestBody(required = false) PetMateFilterRequest filter) {
        if (filter == null) {
            filter = new PetMateFilterRequest();
        }
        return ResponseEntity.ok(petMateService.getCandidates(userId, filter));
    }

    /**
     * 좋아요 보내기
     */
    @PostMapping("/like")
    public ResponseEntity<MatchResponse> like(@RequestBody LikeRequest request) {
        return ResponseEntity.ok(petMateService.like(request));
    }

    /**
     * 매칭된 목록 조회
     */
    @GetMapping("/matches/{userId}")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getMatches(userId));
    }

    /**
     * 좋아요 취소하기
     */
    @DeleteMapping("/like")
    public ResponseEntity<Boolean> unlike(@RequestBody LikeRequest request) {
        return ResponseEntity.ok(petMateService.unlike(request));
    }

    /**
     * 좋아요 보낸 사용자 ID 목록 조회
     */
    @GetMapping("/liked/{userId}")
    public ResponseEntity<List<Long>> getLikedUserIds(@PathVariable Long userId) {
        return ResponseEntity.ok(petMateService.getLikedUserIds(userId));
    }

    /**
     * 온라인 상태 업데이트
     */
    @PutMapping("/status/{userId}")
    public ResponseEntity<Void> updateOnlineStatus(
            @PathVariable Long userId,
            @RequestParam boolean isOnline) {
        petMateService.updateOnlineStatus(userId, isOnline);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 위치 업데이트
     */
    @PutMapping("/location/{userId}")
    public ResponseEntity<Boolean> updateLocation(
            @PathVariable Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String location) {
        boolean updated = petMateService.updateLocation(userId, latitude, longitude, location);
        return ResponseEntity.ok(updated);
    }

    /**
     * 사용자 저장된 위치 조회
     */
    @GetMapping("/location/{userId}")
    public ResponseEntity<PetMateResponse> getSavedLocation(@PathVariable Long userId) {
        PetMateResponse location = petMateService.getSavedLocation(userId);
        if (location != null) {
            return ResponseEntity.ok(location);
        }
        return ResponseEntity.notFound().build();
    }
}
