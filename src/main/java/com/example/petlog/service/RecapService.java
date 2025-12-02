package com.example.petlog.service;

import com.example.petlog.dto.request.RecapRequest;
import com.example.petlog.dto.response.RecapResponse;

import java.util.List;

public interface RecapService {
    // 리캡 생성
    Long createRecap(RecapRequest.Create request);

    // 상세 조회
    RecapResponse.Detail getRecap(Long recapId);

    // 사용자별 리캡 전체 목록 조회
    List<RecapResponse.Simple> getAllRecaps(Long userId);

    // [추가] 펫별 리캡 목록 조회
    List<RecapResponse.Simple> getRecapsByPet(Long petId);
}