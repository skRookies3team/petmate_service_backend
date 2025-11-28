package com.example.petlog.dto.request;

import com.example.petlog.entity.Visibility;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class DiaryRequest {

    // [Request] 일기 생성
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "펫 ID는 필수입니다.")
        private Long petId;

        private String content;
        private Visibility visibility;
        private Boolean isAiGen;
        private String weather;
        private String mood;

        // 이미지 리스트 (아래 정의된 Image 클래스 사용)
        private List<Image> images;
    }

    // [Request] 일기 수정
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        private String content;
        private Visibility visibility;
        private String weather;
        private String mood;

        // 수정 시 이미지는 별도 API로 관리하거나 필요 시 여기에 추가
    }

    // [Inner DTO] 이미지 요청용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {

        private String imageUrl;
        private Integer imgOrder;
        private Boolean mainImage;
    }
}