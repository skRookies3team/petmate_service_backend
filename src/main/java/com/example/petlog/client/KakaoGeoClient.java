package com.example.petlog.client;

import com.example.petlog.dto.response.KakaoAddressResponse;
import com.example.petlog.dto.response.KakaoKeywordSearchResponse;
import com.example.petlog.dto.response.KakaoSearchAddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Kakao Maps Geocoding API Feign Client
 * - 좌표 → 주소 변환 (Reverse Geocoding)
 * - 주소 → 좌표 변환 (Geocoding)
 */
@FeignClient(name = "kakao-geo", url = "https://dapi.kakao.com")
public interface KakaoGeoClient {

        /**
         * 좌표를 지번 주소와 도로명 주소로 변환 (Reverse Geocoding)
         * 
         * @param authorization "KakaoAK {REST_API_KEY}" 형식
         * @param longitude     경도 (x 좌표)
         * @param latitude      위도 (y 좌표)
         * @return 주소 정보
         */
        @GetMapping("/v2/local/geo/coord2address.json")
        KakaoAddressResponse coord2Address(
                        @RequestHeader("Authorization") String authorization,
                        @RequestParam("x") Double longitude,
                        @RequestParam("y") Double latitude);

        /**
         * 주소를 좌표로 변환 (Geocoding / 주소 검색)
         * 
         * @param authorization "KakaoAK {REST_API_KEY}" 형식
         * @param query         검색할 주소 문자열
         * @return 좌표 정보
         */
        @GetMapping("/v2/local/search/address.json")
        KakaoSearchAddressResponse searchAddress(
                        @RequestHeader("Authorization") String authorization,
                        @RequestParam("query") String query);

        /**
         * 키워드로 장소 검색 (건물명, 상호명 등)
         * 
         * @param authorization "KakaoAK {REST_API_KEY}" 형식
         * @param query         검색할 키워드 (건물명, 상호명 등)
         * @return 장소 검색 결과
         */
        @GetMapping("/v2/local/search/keyword.json")
        KakaoKeywordSearchResponse searchKeyword(
                        @RequestHeader("Authorization") String authorization,
                        @RequestParam("query") String query);
}
