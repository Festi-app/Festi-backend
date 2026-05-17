# Festi Backend

Festi는 대학교 축제 정보를 한곳에서 조회하고, 부스 운영 및 웨이팅을 관리하기 위한 축제 통합 플랫폼입니다. 이 저장소는 Festi 서비스의 Spring Boot 기반 백엔드 API를 담당합니다.

## Project Docs

- [프로젝트 기획서](docs/FESTI-PROJECT.md)
- [API 엔드포인트 문서](docs/API-ENDPOINTS.md)

## Tech Stack

- Java 25
- Spring Boot 4.0.x
- Gradle 9.x
- Spring Web
- Spring Validation
- Spring Security
- Spring OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL
- Flyway
- JUnit 5 / Spring Boot Test / Spring Security Test

## Main Features

- 사용자 회원가입, 로그인, 본인 정보 조회/수정/탈퇴
- JWT 기반 인증 및 권한별 API 접근 제어
- 축제 부스 목록/상세 조회 및 관리자용 부스 등록/수정/삭제
- 날짜와 주간/야간 타입 기준 배치도 조회 및 관리
- 야간 부스 메뉴 조회 및 부스 관리자용 메뉴 관리
- 일반 사용자 웨이팅 등록/취소/조회
- 부스 관리자용 웨이팅 호출, 입장 처리, 오픈/마감
- 축제 정보 및 공지사항 조회/관리

## Roles

| Role | Description |
| --- | --- |
| `USER` | 일반 사용자. 부스 조회와 웨이팅 등록/취소를 수행합니다. |
| `BOOTH_MANAGER` | 부스 관리자. 담당 부스 정보, 메뉴, 웨이팅을 관리합니다. |
| `FESTIVAL_ADMIN` | 축제 관리자. 부스 등록/삭제, 배치도, 축제 정보, 공지사항을 관리합니다. |

## Getting Started

### Prerequisites

- JDK 25
- Docker 또는 로컬 PostgreSQL

Gradle Wrapper가 포함되어 있으므로 별도 Gradle 설치는 필요하지 않습니다.

### Environment Variables

| Name | Default | Description |
| --- | --- | --- |
| `FESTI_DATABASE_URL` | `jdbc:postgresql://localhost:5432/festi` | PostgreSQL JDBC URL |
| `FESTI_DATABASE_USERNAME` | `festi` | DB 사용자 이름 |
| `FESTI_DATABASE_PASSWORD` | `festi` | DB 비밀번호 |
| `FESTI_JWT_SECRET` | `local-dev-secret-change-me-at-least-32-bytes` | JWT HS256 서명 secret |
| `FESTI_JWT_ACCESS_TOKEN_EXPIRATION` | `3600` | Access token 만료 시간, 초 단위 |
| `FESTI_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 브라우저 요청을 허용할 origin 목록, 쉼표 구분 |

### Run Tests

```bash
./gradlew test
```

### Run Application

```bash
./gradlew bootRun
```

기본 실행 포트는 Spring Boot 기본값인 `8080`입니다.

## API Base Path

모든 API는 `/api` prefix를 사용합니다.

대표 엔드포인트:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/booths`
- `GET /api/locations`
- `POST /api/booths/{boothId}/waitings`
- `GET /api/festival`

전체 API 목록은 [API 엔드포인트 문서](docs/API-ENDPOINTS.md)를 기준으로 관리합니다.

## Development Notes

- DB schema는 PostgreSQL, UUID, `TIMESTAMPTZ`, enum 타입을 기준으로 Flyway migration에서 관리합니다.
- 비밀번호는 평문 저장하지 않고 BCrypt hash로 저장합니다.
- JWT는 `Authorization: Bearer <token>` 형식으로 전달합니다.
- 사용자 `email`, `name`, `phone`은 필수값이며, 본인 정보 수정에서도 빈 값으로 바꿀 수 없습니다.
- 본인 정보 수정은 `PATCH /api/users/me`에서 `email`, `name`, `phone`을 부분 수정할 수 있고, 성공 시 갱신된 사용자 정보와 새 access token을 함께 반환합니다.
- 부스 관리자 권한은 단순 role뿐 아니라 담당 부스 여부까지 확인합니다.

## Implementation Order

1. Spring Boot/Gradle 프로젝트 생성, Java 25 설정, 테스트 워크플로우 수정
2. 공통 설정: PostgreSQL, Flyway, JPA auditing, 공통 예외 응답, validation
3. ERD 기반 Entity, enum, Repository, migration 작성
4. User/Auth/JWT 및 기본 인증/인가 구현
5. 도메인별 세부 권한 정책 적용
6. 공개 조회 API 구현
7. 축제 관리자 API 구현
8. 부스 관리자 API 구현
9. 웨이팅 API 구현
10. controller/service/repository 테스트 보강

## CI

GitHub Actions는 pull request와 `main` 브랜치 push 시 `./gradlew test`를 실행합니다. CI는 JDK 25 기준으로 동작해야 합니다.
