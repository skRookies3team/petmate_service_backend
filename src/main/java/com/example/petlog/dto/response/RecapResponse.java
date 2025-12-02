package com.example.petlog.dto.response;

import com.example.petlog.entity.Recap;
import com.example.petlog.entity.RecapHighlight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RecapResponse {

    // [Detail] 상세 조회용 (모든 정보 포함)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long recapId;
        private Long petId;
        private String title;
        private String summary;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String mainImageUrl;
        private Integer momentCount;
        private String status;

        // 건강 리포트 (필드는 유지 -> Service에서 채움)
        private Integer avgHeartRate;
        private Integer avgStepCount;
        private Double avgSleepTime;
        private Double avgWeight;

        // 하이라이트 목록
        private List<Highlight> highlights;

        // Entity -> DTO 변환
        public static Detail from(Recap recap) {
            return Detail.builder()
                    .recapId(recap.getRecapId())
                    .petId(recap.getPetId())
                    .title(recap.getTitle())
                    .summary(recap.getSummary())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .mainImageUrl(recap.getMainImageUrl())
                    .momentCount(recap.getMomentCount())
                    .status(recap.getStatus().name())
                    // [삭제됨] 엔티티에 없는 필드이므로 여기서 set 하지 않음
                    // .avgHeartRate(recap.getAvgHeartRate())
                    // .avgStepCount(recap.getAvgStepCount())
                    // .avgSleepTime(recap.getAvgSleepTime())
                    // .avgWeight(recap.getAvgWeight())
                    .highlights(recap.getHighlights().stream()
                            .map(Highlight::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // [Simple] 리스트(카드) 조회용 (간략 정보)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Simple {
        private Long recapId;
        private String title;        // "2024년 1-2월"
        private String mainImageUrl; // 썸네일
        private Integer momentCount; // "45개의 순간"
        private String status;       // "GENERATED" or "WAITING"
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private LocalDateTime createdAt; // "2024.03.01 생성"

        public static Simple from(Recap recap) {
            return Simple.builder()
                    .recapId(recap.getRecapId())
                    .title(recap.getTitle())
                    .mainImageUrl(recap.getMainImageUrl())
                    .momentCount(recap.getMomentCount())
                    .status(recap.getStatus().name())
                    .periodStart(recap.getPeriodStart())
                    .periodEnd(recap.getPeriodEnd())
                    .createdAt(recap.getCreatedAt())
                    .build();
        }
    }

    // [Inner] 하이라이트
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Highlight {
        private String title;
        private String content;

        public static Highlight from(RecapHighlight highlight) {
            return Highlight.builder()
                    .title(highlight.getTitle())
                    .content(highlight.getContent())
                    .build();
        }
    }
}