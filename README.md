# 🐾 PetMate Service Backend

반려동물 친구 매칭 및 실시간 채팅 서비스를 제공하는 Spring Boot 기반 백엔드 API 서버입니다.

## 📋 목차

- [개요](#개요)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
- [API 엔드포인트](#api-엔드포인트)
- [설치 및 실행](#설치-및-실행)
- [환경 변수](#환경-변수)
- [배포](#배포)

---

## 개요

PetMate Service는 반려동물을 키우는 사용자들이 근처의 다른 반려동물 보호자들과 연결될 수 있도록 돕는 서비스입니다. 위치 기반 매칭, 좋아요/요청 시스템, 실시간 채팅 기능을 제공합니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.7 |
| **Database** | PostgreSQL |
| **ORM** | Spring Data JPA / Hibernate |
| **Security** | Spring Security + JWT |
| **Messaging** | Spring WebSocket (STOMP) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Build Tool** | Gradle |
| **Service Communication** | Spring Cloud OpenFeign |
| **External API** | Kakao Maps Geocoding API |

## 프로젝트 구조

```
src/main/java/com/example/petlog/
├── PetlogApplication.java          # 메인 애플리케이션
├── client/                          # 외부 서비스 Feign 클라이언트
│   ├── KakaoGeoClient.java          # Kakao Geocoding API 클라이언트
│   ├── UserServiceClient.java       # User Service 연동
│   ├── PetServiceClient.java        # Pet Service 연동
│   ├── NotificationServiceClient.java
│   └── StorageServiceClient.java
├── config/                          # 설정 클래스
│   ├── SecurityConfig.java          # Spring Security 설정
│   ├── SwaggerConfig.java           # Swagger UI 설정
│   └── WebSocketConfig.java         # WebSocket STOMP 설정
├── controller/                      # REST 컨트롤러
│   ├── PetMateController.java       # 매칭 관련 API
│   ├── GeocodingController.java     # 위치 변환 API
│   └── MessageController.java       # 채팅/메시지 API
├── dto/                             # 데이터 전송 객체
│   ├── request/                     # 요청 DTO
│   └── response/                    # 응답 DTO
├── entity/                          # JPA 엔티티
│   ├── PetMate.java
│   ├── PetMateMatch.java
│   ├── ChatRoom.java
│   └── Message.java
├── exception/                       # 예외 처리
│   ├── BusinessException.java
│   ├── ErrorCode.java
│   └── advice/GlobalExceptionHandler.java
├── repository/                      # JPA 레포지토리
└── service/                         # 비즈니스 로직
    ├── PetMateService.java
    ├── GeocodingService.java
    └── MessageService.java
```

## 주요 기능

### 🎯 PetMate 매칭
- **후보 추천**: 위치 기반으로 주변 반려동물 보호자 추천
- **좋아요/요청**: 관심 있는 사용자에게 친구 요청
- **매칭 관리**: 보낸/받은 요청 조회 및 수락/거절
- **친구 목록**: 매칭된 친구 관리

### 📍 위치 서비스 (Geocoding)
- **좌표 → 주소 변환**: GPS 좌표를 주소로 변환 (Reverse Geocoding)
- **주소 검색**: 주소/키워드로 좌표 검색 (Geocoding)
- **Kakao Maps API 연동**

### 💬 실시간 채팅
- **채팅방 관리**: 1:1 채팅방 생성/조회/삭제
- **메시지 전송**: REST API 및 WebSocket 지원
- **읽음 처리**: 메시지 읽음 상태 관리
- **안 읽은 메시지 카운트**: 배지 표시용

## API 엔드포인트

### PetMate API (`/api/petmate`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/candidates/{userId}` | 후보 추천 조회 |
| POST | `/like` | 좋아요 요청 |
| POST | `/unlike` | 좋아요 취소 |
| POST | `/requests/{matchId}/respond` | 요청 수락/거절 |
| GET | `/requests/{userId}` | 받은 요청 목록 |
| GET | `/requests/{userId}/sent` | 보낸 요청 목록 |
| GET | `/requests/{userId}/count` | 받은 요청 개수 |
| GET | `/matches/{userId}` | 매칭된 친구 목록 |
| GET | `/liked/{userId}` | 좋아요한 유저 목록 |
| DELETE | `/matches/{userId}/{matchedUserId}` | 친구 끊기 |
| GET | `/location/{userId}` | 저장된 위치 조회 |
| PUT | `/location/{userId}` | 위치 업데이트 |
| POST | `/status/{userId}` | 온라인 상태 업데이트 |

### Geocoding API (`/api/geocoding`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/test` | 라우팅 테스트 |
| GET | `/reverse` | 좌표 → 주소 변환 |
| GET | `/search` | 주소 검색 |

### Message API (`/api/messages`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/rooms/{userId}` | 내 채팅방 목록 |
| GET | `/room/{chatRoomId}` | 메시지 내역 조회 |
| POST | `/room` | 채팅방 생성/조회 |
| POST | `/send` | 메시지 전송 (REST) |
| PUT | `/room/{chatRoomId}/read` | 읽음 처리 |
| GET | `/room/{chatRoomId}/unread` | 안 읽은 메시지 수 |
| GET | `/unread/{userId}` | 전체 안 읽은 메시지 수 |
| DELETE | `/room/{chatRoomId}` | 채팅방 나가기 |

### WebSocket

| Endpoint | 설명 |
|----------|------|
| `/ws` | WebSocket 연결 |
| `/pub/chat/message` | 메시지 발행 |
| `/sub/chat/room/{roomId}` | 채팅방 구독 |

## 설치 및 실행

### 사전 요구사항
- Java 17+
- Gradle 8+
- PostgreSQL

### 로컬 실행

1. **저장소 클론**
   ```bash
   git clone https://github.com/skRookies3team/petmate_service_backend.git
   cd petmate_service_backend
   ```

2. **환경 변수 설정**
   
   `.env` 파일을 프로젝트 루트에 생성:
   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/petmate_db
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   KAKAO_REST_API_KEY=your_kakao_api_key
   ```

3. **빌드 및 실행**
   ```bash
   # 빌드
   ./gradlew build

   # 실행 (dev 프로필)
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

4. **API 문서 확인**
   
   서버 실행 후 Swagger UI 접속: `http://localhost:8089/swagger-ui.html`

## 환경 변수

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/petmate_db` |
| `DB_USERNAME` | DB 사용자명 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | `password` |
| `KAKAO_REST_API_KEY` | Kakao REST API 키 | `abc123...` |

## 배포

### Docker

```bash
# 빌드
./gradlew build

# Docker 이미지 빌드
docker build -t petmate-service .

# 컨테이너 실행
docker run -d -p 8089:8089 \
  -e DB_URL=jdbc:postgresql://host:5432/db \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  -e KAKAO_REST_API_KEY=key \
  petmate-service
```

### Kubernetes (Helm)

별도의 Helm 차트를 사용하여 배포합니다. 자세한 내용은 `helm/` 디렉토리를 참조하세요.

---

## 📝 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

## 👥 팀

SK Rookies 3팀
