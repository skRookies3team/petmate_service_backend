# Pull Request - Backend (petmate_service)

## 📌 제목
`feat(petmate): 키워드 검색 및 건물명 반환 기능 추가`

---

## 📝 설명

펫메이트 위치 검색 시 건물명/장소명으로도 검색할 수 있도록 Kakao 키워드 검색 API를 연동하고, 검색 결과에 건물명을 포함하여 반환하도록 개선했습니다.

---

## ✨ 변경사항

### `src/main/java/com/example/petlog/client/KakaoGeoClient.java`
- `searchKeyword()` 메서드 추가 - Kakao 키워드 검색 API 연동

### `src/main/java/com/example/petlog/dto/response/KakaoKeywordSearchResponse.java` (신규)
- Kakao 키워드 검색 API 응답 DTO 생성

### `src/main/java/com/example/petlog/dto/response/SearchAddressResult.java`
- `buildingName` 필드 추가

### `src/main/java/com/example/petlog/service/GeocodingService.java`
- 주소 검색 결과가 없을 때 키워드 검색으로 fallback
- `searchByKeyword()` private 메서드 추가
- 키워드 검색 시 `place_name`을 `buildingName`으로 반환

### `src/main/resources/application.yaml`
- 서버 포트 변경: `8084` → `8089`

---

## 🎯 주요 기능

| 기능 | 설명 |
|------|------|
| 키워드 검색 지원 | "스타벅스", "롯데타워" 등 건물명/장소명으로 검색 가능 |
| 건물명 반환 | 검색 결과에 `buildingName` 필드 포함하여 반환 |
| Fallback 검색 | 주소 검색 결과가 없으면 자동으로 키워드 검색 시도 |

---

## 🔧 API 변경사항

### GET `/api/geocoding/search`

**응답 변경사항:**
```json
{
  "addressName": "서울 강남구 역삼동 123-45",
  "roadAddress": "서울 강남구 테헤란로 123",
  "latitude": 37.5007,
  "longitude": 127.0365,
  "buildingName": "스타벅스 강남역점"  // 추가됨
}
```

---

## 🧪 테스트

- [x] 주소 검색 (예: "서울 강남구") 정상 작동 확인
- [x] 키워드 검색 (예: "스타벅스") 정상 작동 확인
- [x] 주소 검색 실패 시 키워드 검색 fallback 확인
- [x] buildingName 필드 정상 반환 확인

---

## 📋 의존성

- Spring Cloud OpenFeign (기존)
- Kakao Maps API (기존)
