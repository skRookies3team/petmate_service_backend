package com.example.petlog.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Kakao Maps 주소 검색 API 응답 DTO
 * API: GET https://dapi.kakao.com/v2/local/search/address.json
 */
@Data
public class KakaoSearchAddressResponse {
    private Meta meta;
    private List<Document> documents;

    @Data
    public static class Meta {
        private Integer total_count;
        private Integer pageable_count;
        private Boolean is_end;
    }

    @Data
    public static class Document {
        private String address_name; // 전체 지번 주소
        private String address_type; // 주소 타입 (REGION, ROAD, REGION_ADDR, ROAD_ADDR)
        private String x; // 경도 (longitude)
        private String y; // 위도 (latitude)
        private Address address; // 지번 주소 상세
        private RoadAddress road_address; // 도로명 주소 상세
    }

    @Data
    public static class Address {
        private String address_name;
        private String region_1depth_name; // 시/도
        private String region_2depth_name; // 구/군
        private String region_3depth_name; // 동/읍/면
        private String region_3depth_h_name;// 행정동
        private String h_code; // 행정 코드
        private String b_code; // 법정 코드
        private String mountain_yn;
        private String main_address_no;
        private String sub_address_no;
        private String x;
        private String y;
    }

    @Data
    public static class RoadAddress {
        private String address_name;
        private String region_1depth_name;
        private String region_2depth_name;
        private String region_3depth_name;
        private String road_name;
        private String underground_yn;
        private String main_building_no;
        private String sub_building_no;
        private String building_name;
        private String zone_no;
        private String x;
        private String y;
    }
}
