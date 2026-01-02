package com.example.petlog.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // 필수 추가
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // [핵심] 모르는 필드가 와도 에러 안 나게 무시
public class UserInfoResponse {

    private Long id; // [추가] 혹시 ID가 넘어올 수 있으므로 추가
    private String username;
    private String genderType;
    private String profileImage;
    private String social;
    private String statusMessage;
    private Integer age;
    private Integer currentLat;
    private Integer currentLng;
    private Long petCoin;
    private List<PetInfo> pets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true) // [핵심] 여기도 추가
    public static class PetInfo {
        private Long petId;
        private String petName;
        private String species;
        private String breed;
        private String genderType;
        private boolean neutered;
        private String profileImage;
        private Integer age;
        private String status;
        private boolean vaccinated;
    }
}