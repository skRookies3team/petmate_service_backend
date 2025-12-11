package com.example.petlog.service;

import com.example.petlog.client.KakaoGeoClient;
import com.example.petlog.dto.response.AddressResponse;
import com.example.petlog.dto.response.KakaoAddressResponse;
import com.example.petlog.dto.response.KakaoSearchAddressResponse;
import com.example.petlog.dto.response.SearchAddressResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 좌표 ↔ 주소 변환 서비스
 * Kakao Maps Geocoding API 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final KakaoGeoClient kakaoGeoClient;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * 경도/위도 좌표를 주소로 변환 (Reverse Geocoding)
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

    /**
     * 주소를 검색하여 좌표로 변환 (Geocoding)
     * 
     * @param query 검색할 주소 문자열
     * @return 검색 결과 목록 (좌표 포함)
     */
    public List<SearchAddressResult> searchAddress(String query) {
        log.info("주소 검색 요청: query={}", query);

        if (query == null || query.trim().isEmpty()) {
            log.warn("검색어가 비어있습니다");
            return new ArrayList<>();
        }

        String authorization = "KakaoAK " + kakaoRestApiKey;

        try {
            KakaoSearchAddressResponse kakaoResponse = kakaoGeoClient.searchAddress(
                    authorization, query.trim());

            if (kakaoResponse == null || kakaoResponse.getDocuments() == null
                    || kakaoResponse.getDocuments().isEmpty()) {
                log.info("주소 검색 결과가 없습니다: query={}", query);
                return new ArrayList<>();
            }

            List<SearchAddressResult> results = kakaoResponse.getDocuments().stream()
                    .map(doc -> {
                        try {
                            SearchAddressResult.SearchAddressResultBuilder builder = SearchAddressResult.builder()
                                    .addressName(doc.getAddress_name())
                                    .longitude(doc.getX() != null ? Double.parseDouble(doc.getX()) : 0.0)
                                    .latitude(doc.getY() != null ? Double.parseDouble(doc.getY()) : 0.0);

                            // 지번 주소 정보
                            if (doc.getAddress() != null) {
                                builder.region1(doc.getAddress().getRegion_1depth_name())
                                        .region2(doc.getAddress().getRegion_2depth_name())
                                        .region3(doc.getAddress().getRegion_3depth_name());
                            }

                            // 도로명 주소 정보
                            if (doc.getRoad_address() != null) {
                                builder.roadAddress(doc.getRoad_address().getAddress_name())
                                        .zoneNo(doc.getRoad_address().getZone_no());
                            }

                            return builder.build();
                        } catch (Exception e) {
                            log.warn("주소 파싱 실패: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            log.info("주소 검색 완료: {}개 결과", results.size());
            return results;

        } catch (Exception e) {
            log.error("Kakao API 호출 실패: {}", e.getMessage(), e);
            // 예외 발생시 빈 리스트 반환 (500 에러 대신)
            return new ArrayList<>();
        }
    }
}
