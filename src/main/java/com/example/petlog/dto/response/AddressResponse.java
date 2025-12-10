package com.example.petlog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프론트엔드에 반환할 주소 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private String fullAddress; // 전체 주소 (지번)
    private String roadAddress; // 도로명 주소
    private String region1; // 시/도
    private String region2; // 구/군
    private String region3; // 동/읍/면
    private String zoneNo; // 우편번호
    private String buildingName; // 건물명
}
