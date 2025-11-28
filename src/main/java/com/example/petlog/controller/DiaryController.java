package com.example.petlog.controller;

import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // 생성 (POST)
    @PostMapping
    public ResponseEntity<Void> createDiary(@Valid @RequestBody DiaryRequest.Create request) {
        Long diaryId = diaryService.createDiary(request);
        return ResponseEntity.created(URI.create("/api/diaries/" + diaryId)).build();
    }

    // 조회 (GET)
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.getDiary(diaryId));
    }

    // 수정 (PUT)
    @PutMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(@PathVariable Long diaryId,
                                            @RequestBody DiaryRequest.Update request) {
        diaryService.updateDiary(diaryId, request);
        return ResponseEntity.noContent().build();
    }

    // 삭제 (DELETE)
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long diaryId) {
        diaryService.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}