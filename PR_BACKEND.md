# Pull Request - Backend (petmate_service)

## 📌 제목
`feat(infra): Docker 컨테이너화 및 환경별 설정 분리`

---

## 📝 설명

petmate_service_backend를 Docker로 컨테이너화하고, 개발(dev)/운영(prod) 환경별 설정 파일을 분리했습니다.

---

## ✨ 변경사항

### `Dockerfile` (신규)
- Eclipse Temurin JRE 17 기반 경량 Docker 이미지 구성
- 컨테이너 환경에 최적화된 JVM 옵션 적용 (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75`)

### `.dockerignore` (신규)
- Docker 빌드 시 불필요한 파일 제외 (`.git`, `.idea`, `.gradle`, `.env` 등)
- 빌드 컨텍스트 크기 최소화

### `src/main/resources/application-dev.yaml` (신규)
- 로컬 개발 환경 설정 분리
- `.env` 파일에서 DB 연결 정보 로드
- PostgreSQL 연결 및 Hibernate 설정
- 외부 서비스 URL 로컬호스트 기본값 설정

### `src/main/resources/application-prod.yaml` (수정)
- 운영 환경 전용 설정으로 리팩토링
- 필수 환경 변수 검증 추가 (`?DB_URL is required` 등)
- HikariCP 커넥션 풀 설정 추가
- MongoDB 연결 설정 추가
- Health check 엔드포인트 활성화 (`/actuator/health`)

---

## 🎯 환경별 설정

| 환경 | 프로파일 | 활성화 방법 |
|------|----------|------------|
| 개발 | `dev` | `-Dspring.profiles.active=dev` |
| 운영 | `prod` | `-Dspring.profiles.active=prod` |

---

## 🐳 Docker 빌드 및 실행

```bash
# JAR 빌드
./gradlew bootJar

# Docker 이미지 빌드
docker build -t petmate-service:latest .

# Docker 컨테이너 실행
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host:5432/petmate \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  -e MONGO_URI=mongodb://host:27017/petmate \
  -e API_GATEWAY=https://api.example.com \
  -e USER_SERVICE_URL=http://user-service:8080 \
  -e PET_SERVICE_URL=http://pet-service:8080 \
  -p 8089:8089 \
  petmate-service:latest
```

---

## 🔧 필수 환경 변수 (prod)

| 변수명 | 설명 |
|--------|------|
| `DB_URL` | PostgreSQL 연결 URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `MONGO_URI` | MongoDB 연결 URI |
| `API_GATEWAY` | API Gateway URL |
| `USER_SERVICE_URL` | User Service URL |
| `PET_SERVICE_URL` | Pet Service URL |

---

## 🧪 테스트

- [ ] `dev` 프로파일로 로컬 실행 확인
- [ ] Docker 이미지 빌드 확인
- [ ] Docker 컨테이너 실행 및 health check 확인

---

## 📋 의존성

- Spring Boot 3.5.7
- PostgreSQL Driver
- Spring Cloud OpenFeign
- Spring Boot Actuator
