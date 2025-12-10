package com.example.petlog.client;

import com.example.petlog.dto.response.KakaoAddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Kakao Maps Reverse Geocoding API Feign Client
 * 좌표를 주소로 변환하는 API 호출
 */
@FeignClient(name = "kakao-geo", url = "https://dapi.kakao.com")
public interface KakaoGeoClient {

    /**
     * 좌표를 지번 주소와 도로명 주소로 변환
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
}
