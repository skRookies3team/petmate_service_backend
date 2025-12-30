package com.example.petlog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * user-service에서 가져온 사용자 정보를 매핑하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
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
