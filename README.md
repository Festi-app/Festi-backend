# Festi Backend

Festi는 대학교 축제 정보를 한곳에서 조회하고, 부스 운영 및 웨이팅을 관리하기 위한 축제 통합 플랫폼입니다. 이 저장소는 Festi 서비스의 Spring Boot 기반 백엔드 API를 담당합니다.

## Project Docs

- [프로젝트 기획서](docs/FESTI-PROJECT.md)
- [API 엔드포인트 문서](docs/API-ENDPOINTS.md)
- [프론트엔드 API 컨텍스트](docs/FRONTEND_API_CONTEXT.md)

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

## Current Features

- 사용자 회원가입, 로그인, 본인 정보 조회/수정/탈퇴
- JWT 기반 인증 및 권한별 API 접근 제어
- 일반 사용자 이상 인증 후 가능한 조회 API
  - 부스 목록/상세
  - 부스 메뉴
  - 날짜와 주간/야간 타입 기준 배치도
  - 축제 정보와 공지사항
  - 본인 웨이팅 목록

관리자용 부스/메뉴/배치도 관리와 웨이팅 목록·호출·착석·오픈/마감 API, 승인 후 부스/메뉴 이미지 업로드, 사용자 Web Push 구독 저장 및 VAPID 기반 호출 알림 발송이 구현되어 있습니다.

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
| `FESTI_IMAGE_STORAGE_ROOT` | `./uploads/images` | 업로드 이미지가 영구 저장되는 디렉터리 |
| `FESTI_IMAGE_MAX_FILE_SIZE` | `5MB` | multipart 파일 업로드 상한 |
| `FESTI_IMAGE_MAX_REQUEST_SIZE` | `6MB` | multipart 요청 전체 상한 |
| `FESTI_WEB_PUSH_ENABLED` | `false` | VAPID Web Push 전송 활성화 여부 |
| `FESTI_VAPID_PUBLIC_KEY` | 빈 값 | Push 구독 및 발송에 사용하는 VAPID 공개키 |
| `FESTI_VAPID_PRIVATE_KEY` | 빈 값 | Web Push 발송에 사용하는 VAPID 비밀키 |
| `FESTI_VAPID_SUBJECT` | 빈 값 | VAPID contact subject, 예: `mailto:admin@example.com` |
| `FESTI_WEB_PUSH_TTL_SECONDS` | `300` | Push provider가 메시지를 보관할 시간, 초 단위 |
| `FESTI_WEB_PUSH_WORKER_ENABLED` | `true` | 저장된 Push outbox event를 발송하는 scheduler 활성화 여부 |
| `FESTI_WEB_PUSH_POLL_DELAY_MILLIS` | `1000` | worker polling 간격, 밀리초 단위 |
| `FESTI_WEB_PUSH_BATCH_SIZE` | `20` | 한 polling 주기에서 처리할 최대 event 수 |
| `FESTI_WEB_PUSH_MAX_ATTEMPTS` | `2` | 일시 실패 event의 최대 처리 횟수 |
| `FESTI_WEB_PUSH_RETRY_DELAY_SECONDS` | `30` | 일시 실패 후 재처리까지 대기 시간, 초 단위 |
| `FESTI_WEB_PUSH_PROCESSING_TIMEOUT_SECONDS` | `300` | 처리 중 중단된 event를 재회수할 lease 시간, 초 단위 |

### Run Tests

```bash
./gradlew test
```

### Run Application

```bash
./gradlew bootRun
```

기본 실행 포트는 Spring Boot 기본값인 `8080`입니다.

## Deployment

현재 저장소는 별도 컨테이너 설정 없이 **실행 가능한 Spring Boot JAR 배포**를 기준으로 준비되어 있습니다. 운영 환경에서는 기본값을 그대로 쓰지 말고, PostgreSQL과 보안 관련 환경 변수를 먼저 설정해야 합니다.

### 1. Prepare PostgreSQL

운영 DB는 애플리케이션 시작 전에 먼저 존재해야 합니다. 애플리케이션은 시작 시 Flyway migration을 실행하고, 이후 Hibernate가 schema를 검증합니다.

예시:

```sql
CREATE ROLE festi_app LOGIN PASSWORD 'change-me';
CREATE DATABASE festi OWNER festi_app;
```

권장 기준:

- CI 검증 기준은 PostgreSQL 16
- 애플리케이션 계정은 대상 DB에서 migration을 적용할 수 있어야 함
- 운영 DB에는 테스트용 H2 설정이 적용되지 않음

### 2. Configure Production Environment Variables

운영 환경에서는 최소한 아래 값들을 명시적으로 지정합니다.

| Name | Required in production | Example |
| --- | --- | --- |
| `FESTI_DATABASE_URL` | Yes | `jdbc:postgresql://db.example.com:5432/festi` |
| `FESTI_DATABASE_USERNAME` | Yes | `festi_app` |
| `FESTI_DATABASE_PASSWORD` | Yes | `strong-db-password` |
| `FESTI_JWT_SECRET` | Yes | 32바이트 이상 길이의 무작위 secret |
| `FESTI_CORS_ALLOWED_ORIGINS` | Yes | `https://app.example.com` |
| `FESTI_IMAGE_STORAGE_ROOT` | Yes when image uploads are used | `/srv/festi/uploads/images` |
| `FESTI_JWT_ACCESS_TOKEN_EXPIRATION` | Optional | `3600` |
| `FESTI_WEB_PUSH_ENABLED` | Yes when Web Push is used | `true` |
| `FESTI_VAPID_PUBLIC_KEY` | Yes when Web Push is enabled | VAPID 공개키 |
| `FESTI_VAPID_PRIVATE_KEY` | Yes when Web Push is enabled | VAPID 비밀키 |
| `FESTI_VAPID_SUBJECT` | Yes when Web Push is enabled | `mailto:admin@example.com` |
| `FESTI_WEB_PUSH_TTL_SECONDS` | Optional | `300` |
| `FESTI_WEB_PUSH_WORKER_ENABLED` | Optional | `true` |
| `FESTI_WEB_PUSH_POLL_DELAY_MILLIS` | Optional | `1000` |
| `FESTI_WEB_PUSH_BATCH_SIZE` | Optional | `20` |
| `FESTI_WEB_PUSH_MAX_ATTEMPTS` | Optional | `2` |
| `FESTI_WEB_PUSH_RETRY_DELAY_SECONDS` | Optional | `30` |
| `FESTI_WEB_PUSH_PROCESSING_TIMEOUT_SECONDS` | Optional | `300` |

`FESTI_JWT_SECRET`은 로컬 기본값을 운영에서 절대 사용하지 말아야 합니다. 여러 origin을 허용해야 한다면 `FESTI_CORS_ALLOWED_ORIGINS`에 쉼표로 구분해 넣습니다.

예시:

```bash
export FESTI_DATABASE_URL='jdbc:postgresql://db.example.com:5432/festi'
export FESTI_DATABASE_USERNAME='festi_app'
export FESTI_DATABASE_PASSWORD='strong-db-password'
export FESTI_JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes'
export FESTI_CORS_ALLOWED_ORIGINS='https://app.example.com'
export FESTI_IMAGE_STORAGE_ROOT='/srv/festi/uploads/images'
export FESTI_JWT_ACCESS_TOKEN_EXPIRATION='3600'
export FESTI_WEB_PUSH_ENABLED='true'
export FESTI_VAPID_PUBLIC_KEY='replace-with-vapid-public-key'
export FESTI_VAPID_PRIVATE_KEY='replace-with-vapid-private-key'
export FESTI_VAPID_SUBJECT='mailto:admin@example.com'
```

### 3. Build and Run

```bash
./gradlew clean bootJar
java -jar build/libs/festi-backend-0.0.1-SNAPSHOT.jar
```

배포 시 애플리케이션은 다음 순서로 시작합니다.

1. PostgreSQL 연결
2. Flyway migration 적용
3. Hibernate schema validation
4. HTTP 서버 기동

DB 접속 정보가 잘못되었거나 migration 권한이 부족하면 서버는 정상 기동하지 않습니다. 운영 배포 전에는 같은 환경 변수로 한 번 직접 기동해 startup log를 확인하는 편이 안전합니다.

이미지는 `FESTI_IMAGE_STORAGE_ROOT` 아래 `booths/`, `menus/`에 UUID 파일명으로 저장되고 Spring이 `/media/images/**`로 공개 제공합니다. 운영에서는 JAR 재배포 후에도 유지되는 호스트 디렉터리(예: `/srv/festi/uploads/images`)를 지정하고 애플리케이션 프로세스에 쓰기 권한을 부여해야 합니다. 업로드는 JPEG/PNG, `5MB`, `4096x4096` 제한을 적용합니다.

### 4. Deployment Checklist

- [ ] JDK 25 런타임 준비
- [ ] PostgreSQL DB와 애플리케이션 계정 생성
- [ ] 운영용 `FESTI_JWT_SECRET` 교체
- [ ] 실제 프런트엔드 origin으로 `FESTI_CORS_ALLOWED_ORIGINS` 설정
- [ ] 영구 이미지 저장 경로 생성 및 `FESTI_IMAGE_STORAGE_ROOT` 쓰기 권한 확인
- [ ] `./gradlew test` 통과 확인
- [ ] PostgreSQL 연결 환경에서 `./gradlew postgresTest` 통과 확인
- [ ] 배포 환경에서 애플리케이션 startup log와 Flyway 적용 로그 확인

## API Base Path

모든 API는 `/api` prefix를 사용합니다.

대표 엔드포인트:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/booths`
- `GET /api/locations`
- `POST /api/booths/{boothId}/waitings`
- `POST /api/push-subscriptions`
- `GET /api/festival`

전체 API 목록은 [API 엔드포인트 문서](docs/API-ENDPOINTS.md)를 기준으로 관리합니다.

## Swagger / OpenAPI

애플리케이션 실행 후 Swagger UI와 OpenAPI JSON은 인증 없이 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

JWT 인증이 필요한 API를 Swagger UI에서 호출할 때는 `Authorize` 버튼에 `Bearer <access-token>` 형식으로 값을 입력합니다.

## Development Notes

- DB schema는 PostgreSQL, UUID, `TIMESTAMPTZ`, enum 타입을 기준으로 Flyway migration에서 관리합니다.
- 운영 profile을 별도로 두지 않으므로, 배포 환경 차이는 환경 변수로 주입합니다.
- 비밀번호는 평문 저장하지 않고 BCrypt hash로 저장합니다.
- JWT는 `Authorization: Bearer <token>` 형식으로 전달합니다.
- 조회 API도 일반 사용자 이상의 인증이 필요하며, 회원가입/로그인만 비인증 접근을 허용합니다.
- 사용자 `email`, `name`, `phone`은 필수값이며, 본인 정보 수정에서도 빈 값으로 바꿀 수 없습니다.
- 본인 정보 수정은 `PATCH /api/users/me`에서 `email`, `name`, `phone`을 부분 수정할 수 있고, 성공 시 갱신된 사용자 정보와 새 access token을 함께 반환합니다.
- 부스 관리자 권한은 단순 role뿐 아니라 담당 부스 여부까지 확인합니다.
- 웨이팅 Push 알림의 표시 문구와 아이콘 경로(`title`, `body`, `icon`)는 `src/main/resources/push-messages.yml`에서 관리합니다.
- 웨이팅 호출 API는 Push outbox event만 저장하며, scheduler worker가 커밋 이후 별도로 VAPID Web Push를 발송하고 delivery 결과를 기록합니다.

## Implementation Order

1. Spring Boot/Gradle 프로젝트 생성, Java 25 설정, 테스트 워크플로우 수정
2. 공통 설정: PostgreSQL, Flyway, JPA auditing, 공통 예외 응답, validation
3. ERD 기반 Entity, enum, Repository, migration 작성
4. User/Auth/JWT 및 기본 인증/인가 구현
5. 도메인별 세부 권한 정책 적용
6. 일반 사용자 이상 조회 API 구현
7. 축제 관리자 API 구현
8. 부스 관리자 API 구현
9. 웨이팅 API 구현
10. controller/service/repository 테스트 보강

## CI

GitHub Actions는 pull request와 `main` 브랜치 push 시 `./gradlew test`를 실행합니다. CI는 JDK 25 기준으로 동작해야 합니다.
