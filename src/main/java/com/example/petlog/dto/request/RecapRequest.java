package com.example.petlog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class RecapRequest {

    // [Request] 리캡 임시 생성
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotNull
        private Long petId;
        @NotNull
        private Long userId;

        private String title;
        private String summary;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String mainImageUrl;
        private Integer momentCount; // 분석된 순간의 개수

        // 건강 리포트 데이터
        private Integer avgHeartRate;
        private Integer avgStepCount;
        private Double avgSleepTime;
        private Double avgWeight;

        // 하이라이트 목록
        private List<HighlightDto> highlights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightDto {
        private String title;
        private String content;
    }
}