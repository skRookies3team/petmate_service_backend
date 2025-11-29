package com.example.petlog.service;

import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;

public interface DiaryService {

    // 일기 생성
    Long createDiary(DiaryRequest.Create request);

    // 일기 단건 조회
    DiaryResponse getDiary(Long diaryId);

    // 일기 수정
    void updateDiary(Long diaryId, DiaryRequest.Update request);

    // 일기 삭제
    void deleteDiary(Long diaryId);
}