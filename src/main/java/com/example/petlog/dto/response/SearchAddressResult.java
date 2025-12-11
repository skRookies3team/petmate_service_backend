package com.example.petlog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주소 검색 결과 DTO (Frontend 반환용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAddressResult {
    private String addressName; // 전체 주소
    private String roadAddress; // 도로명 주소
    private Double latitude; // 위도 (y)
    private Double longitude; // 경도 (x)
    private String region1; // 시/도
    private String region2; // 구/군
    private String region3; // 동/읍/면
    private String zoneNo; // 우편번호
}
