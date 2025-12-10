package com.example.petlog.controller;

import com.example.petlog.dto.response.AddressResponse;
import com.example.petlog.service.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 좌표 → 주소 변환 API Controller
 * Kakao Maps Reverse Geocoding API 사용
 */
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Geocoding", description = "좌표-주소 변환 API")
public class GeocodingController {

    private final GeocodingService geocodingService;

    /**
     * 좌표를 주소로 변환 (Reverse Geocoding)
     * 
     * @param x 경도 (longitude)
     * @param y 위도 (latitude)
     * @return 변환된 주소 정보
     */
    @GetMapping("/reverse")
    @Operation(summary = "좌표 → 주소 변환", description = "GPS 좌표(경도, 위도)를 주소로 변환합니다.")
    public ResponseEntity<AddressResponse> reverseGeocode(
            @Parameter(description = "경도 (longitude)", example = "127.028610") @RequestParam Double x,
            @Parameter(description = "위도 (latitude)", example = "37.498095") @RequestParam Double y) {

        AddressResponse response = geocodingService.getAddressFromCoords(x, y);
        return ResponseEntity.ok(response);
    }
}
