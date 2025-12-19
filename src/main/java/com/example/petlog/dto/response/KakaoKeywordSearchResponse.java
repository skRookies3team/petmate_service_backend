package com.example.petlog.dto.response;

import lombok.Data;
import java.util.List;

/**
 * Kakao Maps 키워드 검색 API 응답 DTO
 * API: GET https://dapi.kakao.com/v2/local/search/keyword.json
 */
@Data
public class KakaoKeywordSearchResponse {
    private Meta meta;
    private List<Document> documents;

    @Data
    public static class Meta {
        private Integer total_count;
        private Integer pageable_count;
        private Boolean is_end;
        private SameName same_name;
    }

    @Data
    public static class SameName {
        private List<String> region;
        private String keyword;
        private String selected_region;
    }

    @Data
    public static class Document {
        private String id; // 장소 ID
        private String place_name; // 장소명 (건물명)
        private String category_name; // 카테고리 이름
        private String category_group_code;
        private String category_group_name;
        private String phone; // 전화번호
        private String address_name; // 지번 주소
        private String road_address_name; // 도로명 주소
        private String x; // 경도 (longitude)
        private String y; // 위도 (latitude)
        private String place_url; // 장소 상세 페이지 URL
        private String distance; // 거리 (중심 좌표 기준)
    }
}
