package com.example.petlog.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Kakao Maps Reverse Geocoding API 응답 DTO
 * API: GET https://dapi.kakao.com/v2/local/geo/coord2address.json
 */
@Data
public class KakaoAddressResponse {
    private Meta meta;
    private List<Document> documents;

    @Data
    public static class Meta {
        private Integer total_count;
    }

    @Data
    public static class Document {
        private Address address;
        private RoadAddress road_address;
    }

    @Data
    public static class Address {
        private String address_name; // 전체 지번 주소
        private String region_1depth_name; // 시/도
        private String region_2depth_name; // 구/군
        private String region_3depth_name; // 동/읍/면
        private String mountain_yn; // 산 여부
        private String main_address_no; // 지번 주번지
        private String sub_address_no; // 지번 부번지
    }

    @Data
    public static class RoadAddress {
        private String address_name; // 전체 도로명 주소
        private String region_1depth_name; // 시/도
        private String region_2depth_name; // 구/군
        private String region_3depth_name; // 동/읍/면
        private String road_name; // 도로명
        private String underground_yn; // 지하 여부
        private String main_building_no; // 건물 본번
        private String sub_building_no; // 건물 부번
        private String building_name; // 건물 이름
        private String zone_no; // 우편번호
    }
}
