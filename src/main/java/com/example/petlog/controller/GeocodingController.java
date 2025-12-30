package com.example.petlog.controller;

import com.example.petlog.dto.response.AddressResponse;
import com.example.petlog.dto.response.SearchAddressResult;
import com.example.petlog.service.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 좌표 ↔ 주소 변환 API Controller
 * Kakao Maps Geocoding API 사용
 */
@RestController
@RequestMapping("/geocoding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Geocoding", description = "좌표-주소 변환 API")
public class GeocodingController {

    private final GeocodingService geocodingService;

    /**
     * 라우팅 테스트용 간단한 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("========== /geocoding/test 요청 도착! ==========");
        System.out.println("========== /geocoding/test 요청 도착! (sysout) ==========");
        return ResponseEntity.ok("Geocoding Controller is working! Path: /api/geocoding/test");
    }

    /**
     * 좌표를 주소로 변환 (Reverse Geocoding)
     * 
     * @param x 경도 (longitude)
     * @param y 위도 (latitude)
     * @return 변환된 주소 정보
     */
    @GetMapping("/reverse")
    @Operation(summary = "좌표 → 주소 변환", description = "GPS 좌표(경도, 위도)를 주소로 변환합니다.")
    public ResponseEntity<?> reverseGeocode(
            @Parameter(description = "경도 (longitude)", example = "127.028610") @RequestParam Double x,
            @Parameter(description = "위도 (latitude)", example = "37.498095") @RequestParam Double y) {

        try {
            AddressResponse response = geocodingService.getAddressFromCoords(x, y);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 상세 에러 로깅 및 반환 (디버깅용)
            String errorMessage = String.format("지오코딩 실패 - 좌표: (%s, %s), 원인: %s", x, y, e.getMessage());
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", errorMessage,
                    "cause", e.getClass().getSimpleName()));
        }
    }

    /**
     * 주소를 검색하여 좌표로 변환 (Geocoding)
     * 
     * @param query 검색할 주소 문자열
     * @return 검색 결과 목록 (좌표 포함)
     */
    @GetMapping("/search")
    @Operation(summary = "주소 검색 → 좌표 변환", description = "주소를 검색하여 좌표(경도, 위도)로 변환합니다.")
    public ResponseEntity<List<SearchAddressResult>> searchAddress(
            @Parameter(description = "검색할 주소", example = "서울 강남구 역삼동") @RequestParam String query) {

        List<SearchAddressResult> results = geocodingService.searchAddress(query);
        return ResponseEntity.ok(results);
    }
}
