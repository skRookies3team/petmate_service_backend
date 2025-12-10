package com.example.petlog.service;

import com.example.petlog.client.KakaoGeoClient;
import com.example.petlog.dto.response.AddressResponse;
import com.example.petlog.dto.response.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 좌표 → 주소 변환 서비스
 * Kakao Maps Reverse Geocoding API 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final KakaoGeoClient kakaoGeoClient;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * 경도/위도 좌표를 주소로 변환
     * 
     * @param longitude 경도 (x 좌표)
     * @param latitude  위도 (y 좌표)
     * @return 변환된 주소 정보
     */
    public AddressResponse getAddressFromCoords(Double longitude, Double latitude) {
        log.info("좌표 → 주소 변환 요청: longitude={}, latitude={}", longitude, latitude);

        String authorization = "KakaoAK " + kakaoRestApiKey;

        try {
            KakaoAddressResponse kakaoResponse = kakaoGeoClient.coord2Address(
                    authorization, longitude, latitude);

            if (kakaoResponse == null || kakaoResponse.getDocuments() == null
                    || kakaoResponse.getDocuments().isEmpty()) {
                log.warn("해당 좌표에 대한 주소 정보를 찾을 수 없습니다: longitude={}, latitude={}",
                        longitude, latitude);
                return AddressResponse.builder()
                        .fullAddress("주소를 찾을 수 없습니다")
                        .build();
            }

            KakaoAddressResponse.Document document = kakaoResponse.getDocuments().get(0);

            AddressResponse.AddressResponseBuilder builder = AddressResponse.builder();

            // 지번 주소 정보
            if (document.getAddress() != null) {
                KakaoAddressResponse.Address address = document.getAddress();
                builder.fullAddress(address.getAddress_name())
                        .region1(address.getRegion_1depth_name())
                        .region2(address.getRegion_2depth_name())
                        .region3(address.getRegion_3depth_name());
            }

            // 도로명 주소 정보
            if (document.getRoad_address() != null) {
                KakaoAddressResponse.RoadAddress roadAddr = document.getRoad_address();
                builder.roadAddress(roadAddr.getAddress_name())
                        .zoneNo(roadAddr.getZone_no())
                        .buildingName(roadAddr.getBuilding_name());

                // 지번 주소가 없는 경우 도로명 주소의 region 정보 사용
                if (document.getAddress() == null) {
                    builder.region1(roadAddr.getRegion_1depth_name())
                            .region2(roadAddr.getRegion_2depth_name())
                            .region3(roadAddr.getRegion_3depth_name());
                }
            }

            AddressResponse response = builder.build();
            log.info("주소 변환 완료: {}", response.getFullAddress());
            return response;

        } catch (Exception e) {
            log.error("Kakao API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주소 변환에 실패했습니다: " + e.getMessage());
        }
    }
}
