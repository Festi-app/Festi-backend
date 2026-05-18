  # Festi-Backend Implementation Plan

## Summary

Festi-Backend는 대학교 축제 통합 플랫폼의 API 서버다. Spring Boot + Java 기반으로 구현하며, PostgreSQL ERD를 기준으로 도메인 모델을 구성하고 JWT 기반 인증/인가를 적용한다.

현재 구현 기준으로 공통 인프라, 도메인 모델, User/Auth/JWT, 기본 권한 체계까지 완료된 상태다. 다음 단계는 공개 조회 API를 먼저 구현해 실제 서비스 기능을 열어가는 것이다. 이후 구현도 단계별 PR로 나누어 진행한다.

| Priority | Scope | Status |
| --- | --- | --- |
| 1 | Spring Boot/Gradle 프로젝트 생성, Java 25 설정, 테스트 워크플로우 수정 | Done |
| 2 | 공통 설정: PostgreSQL, Flyway, JPA auditing, 공통 예외 응답, validation | Done |
| 3 | ERD 기반 Entity, enum, Repository, migration 작성 | Done |
| 4 | User/Auth/JWT 구현 | Done |
| 5 | SecurityConfig와 권한 체계 적용 | Done |
| 6 | 공개 조회 API 구현 | Next |
| 7 | 축제 관리자 API 구현 | Pending |
| 8 | 부스 관리자 API 구현 | Pending |
| 9 | 웨이팅 API 구현 | Pending |
| 10 | controller/service/repository 테스트 보강 | Pending |

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

## Priority 1: Project Initialization

Spring Boot 프로젝트의 실행 가능한 최소 구조를 만든다.

- `com.festi.backend` 루트 패키지 생성
- Gradle Wrapper 추가
- Java toolchain을 Java 25로 설정
- Spring Boot 4.0.x 의존성 구성
- 기본 `FestiBackendApplication` 추가
- `application.yml` 기본 설정 추가
- GitHub Actions 테스트 워크플로우를 JDK 25 기준으로 수정
- `./gradlew test`가 통과하는 최소 smoke test 추가

## Priority 2: Common Infrastructure

도메인 Entity와 API 구현 전에 모든 기능이 공유할 공통 기반을 고정한다.

- PostgreSQL 연결 설정
  - `FESTI_DATABASE_URL`
  - `FESTI_DATABASE_USERNAME`
  - `FESTI_DATABASE_PASSWORD`
  - `ddl-auto: validate`
  - `open-in-view: false`
- 테스트 profile 설정
  - `application-test.yml`
  - H2 기반 context test
- Flyway 기본 설정
  - `src/main/resources/db/migration`
  - baseline migration
- JPA auditing
  - `@EnableJpaAuditing`
  - `BaseTimeEntity`
- 공통 예외 처리
  - `ErrorCode`
  - `FestiException`
  - `BadRequestException`
  - `ConflictException`
  - `NotFoundException`
  - `GlobalExceptionHandler`
- 공통 에러 응답
  - `ErrorResponse`
  - validation details는 민감 입력값 노출을 피하기 위해 rejected value를 포함하지 않는다.

## Priority 3: Entity, Enum, Repository, Migration

ERD를 PostgreSQL 기준으로 구현한다. 이 단계에서는 Controller/Service API 동작보다 데이터 모델의 정확성을 우선한다.

### Entities

- `User`
  - `id`, `email`, `passwordHash`, `name`, `phone`, `role`, `createdAt`, `updatedAt`
  - `email` unique
- `Booth`
  - `id`, `manager`, `createdBy`, `name`, `category`, `type`, `description`, `operatingHours`, `imageUrl`, `isActive`, `isWaitingOpen`, `createdAt`, `updatedAt`
- `MenuItem`
  - `id`, `booth`, `name`, `price`, `description`, `imageUrl`, `isSoldOut`, `sortOrder`, `createdAt`, `updatedAt`
- `BoothLocation`
  - `id`, `booth`, `type`, `index`, `day`, `zoneLabel`, `createdAt`, `updatedAt`
  - 현재 DB column과 Repository 정렬 기준도 `index`를 사용한다.
- `Waiting`
  - `id`, `booth`, `user`, `partySize`, `status`, `callCount`, `registeredAt`, `updatedAt`
- `BoothAdminAssignment`
  - `id`, `booth`, `user`, `grantedBy`, `createdAt`
- `Festival`
  - `id`, `name`, `startDate`, `endDate`, `description`, `createdAt`, `updatedAt`
- `Notice`
  - `id`, `festival`, `title`, `content`, `createdBy`, `createdAt`, `updatedAt`

### Enums

- `UserRole`
  - `USER`
  - `BOOTH_MANAGER`
  - `FESTIVAL_ADMIN`
- `BoothCategory`
  - `ACTIVITY`
  - `INFO`
  - `MARKET`
  - `EXPERIENCE`
  - `PROMOTION`
  - `ALCOHOL`
- `BoothType`
  - `DAY`
  - `NIGHT`
- `WaitingStatus`
  - `WAITING`
  - `CALLED`
  - `SEATED`
  - `CANCELLED`

### Repositories

- `UserRepository`
  - `findByEmail`
  - `existsByEmail`
- `BoothRepository`
  - type/category/active 기반 조회
- `MenuItemRepository`
  - `findByBoothIdOrderBySortOrder`
- `BoothLocationRepository`
  - `findByDayAndTypeOrderByIndex`
- `WaitingRepository`
  - `findByUserId`
  - `findByBoothIdAndStatusOrderByRegisteredAt`
- `BoothAdminAssignmentRepository`
  - 부스 관리자 권한 확인용 query
- `FestivalRepository`
- `NoticeRepository`

### Completed Deliverables

- `User`, `Booth`, `MenuItem`, `BoothLocation`, `Waiting`, `BoothAdminAssignment`, `Festival`, `Notice` Entity와 관련 enum 구현
- 8개 JPA Repository 구현
  - `UserRepository`
  - `BoothRepository`
  - `MenuItemRepository`
  - `BoothLocationRepository`
  - `WaitingRepository`
  - `BoothAdminAssignmentRepository`
  - `FestivalRepository`
  - `NoticeRepository`
- PostgreSQL schema migration `V2__init_schema.sql`
- Lombok 기반 Entity/Common 클래스 보일러플레이트 정리
- 테스트를 H2 fast test(`./gradlew test`)와 PostgreSQL migration test(`./gradlew postgresTest`)로 분리

## Priority 4: User/Auth/JWT

회원가입, 로그인, 본인 정보 API를 구현한다. 이 단계에서는 인증 토큰을 발급할 수 있는 최소 기능을 완성하고, JWT 검증과 세부 권한 정책은 Priority 5에서 연결한다.

- DTO
  - 회원가입 요청
  - 로그인 요청
  - 사용자 응답
  - 내 정보 수정 요청

- `AuthController`
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
- `UserController`
  - `GET /api/users/me`
  - `PATCH /api/users/me`
  - `DELETE /api/users/me`
- `AuthService`
  - 회원가입 시 기본 role은 `USER`
  - 비밀번호는 BCrypt hash로 저장
  - 로그인 성공 시 JWT access token 발급
- JWT claim
  - `sub`: user id
  - `email`
  - `role`
  - `iat`
  - `exp`
- JWT secret
  - `FESTI_JWT_SECRET`
- refresh token은 v1 범위에서 제외한다.

### Completed Deliverables

- `AuthDTO`, `UserDTO` 구현
- `AuthController`
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
- `UserController`
  - `GET /api/users/me`
  - `PATCH /api/users/me`
  - `DELETE /api/users/me`
- `AuthService`, `UserService` 구현
  - 회원가입 시 기본 role `USER`
  - BCrypt 기반 비밀번호 저장
  - 이메일 변경 시 fresh access token 재발급
- JWT 발급/변환 계층 구현
  - `JwtTokenService`
  - `HmacJwtTokenService`
  - `JwtProperties`
  - `AuthenticatedUser`
  - `AuthenticatedUserAuthenticationToken`
  - `JwtAuthenticationConverter`
- 사용자 스키마 보강 migration `V3__users_phone_not_null.sql`
- DTO / service / JWT converter / JWT issuance / security exception handler 테스트 추가

## Priority 5: Security and Authorization

Spring Security 기반 인증/인가 구조를 확정한다.

- `SecurityConfig`
  - stateless session
  - CSRF disabled for token API
  - CORS 기본 정책
  - JWT resource server 설정
- 인증 객체
  - JWT claim에서 user id/email/role을 읽어 custom principal로 변환
- 접근 제어
  - 공개 API는 `permitAll`
  - 사용자 API는 authenticated
  - 관리자 API는 role + 도메인 소유권 검증
- 부스 관리자 권한
  - 단순 role만 보지 않는다.
  - v1에서는 `booths.managerId`만 기준으로 해당 부스 담당자인지 확인한다.
  - `BoothAdminAssignment`는 현재 schema에 남아 있지만, v1 인증/인가 경로에서는 사용하지 않는다.
  - `FESTIVAL_ADMIN`은 부스 관리자 권한도 통과한다.

### Completed Deliverables

- `SecurityConfig`에 API Access Policy 전체 반영
  - 공개 조회 API는 `permitAll`
  - 본인 정보 / 일반 사용자 웨이팅 API는 `authenticated`
  - 축제 관리자 API는 `FESTIVAL_ADMIN`
  - 부스 관리자 API는 `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN`
  - 문서에 정의되지 않은 라우트는 기본 `denyAll`
- `BoothAuthorizationService`
  - `AuthenticatedUser`와 `Booth` 엔티티를 기준으로 부스 소유권 검증
  - `FESTIVAL_ADMIN` 우회 허용
  - `BOOTH_MANAGER`는 `booths.manager_id`와 현재 사용자 id가 일치할 때만 허용
  - `BoothAdminAssignmentRepository`는 v1 권한 판정에서 미사용
- `SecurityRoutePolicyIntegrationTest`
  - 공개 / 인증 사용자 / 부스 관리자 / 축제 관리자 coarse gate 검증
- `BoothAuthorizationServiceTest`
  - festival admin 우회
  - 동일 manager 허용
  - 다른 manager / manager 없음 / 일반 사용자 거부

## Priority 6: Public Read APIs

인증 없이 조회 가능한 API를 먼저 구현한다.

- `GET /api/booths`
  - `day`, `type` filter
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
  - `day`, `type` filter
- `GET /api/festival`
- `GET /api/festival/notices`

## Priority 7: Festival Admin APIs

축제 관리자 권한이 필요한 API를 구현한다.

- `POST /api/booths`
- `DELETE /api/booths/{boothId}`
- `POST /api/locations`
- `PATCH /api/locations/{locationId}`
- `DELETE /api/locations/{locationId}`
- `PATCH /api/festival`
- `POST /api/festival/notices`

`API-ENDPOINTS.md`의 `UPDATE` 표기는 `PATCH`로 정규화한다.

## Priority 8: Booth Manager APIs

부스 관리자 권한이 필요한 API를 구현한다.

- `PATCH /api/booths/{boothId}`
- `POST /api/booths/{boothId}/menus`
- `PATCH /api/booths/{boothId}/menus/{menuId}`
- `DELETE /api/booths/{boothId}/menus/{menuId}`
- `POST /api/booths/{boothId}/menus/{menuId}/sold-out`

메뉴 생성/수정은 야간 부스에서만 허용한다.

## Priority 9: Waiting APIs

웨이팅 기능을 구현한다.

- 일반 사용자
  - `POST /api/booths/{boothId}/waitings`
  - `DELETE /api/waitings/{waitingId}`
  - `GET /api/waitings`
- 부스 관리자
  - `GET /api/booths/{boothId}/waitings`
  - `POST /api/waitings/{waitingId}/call`
  - `PATCH /api/waitings/{waitingId}/status`
  - `PATCH /api/booths/{boothId}/waitings/status`

검증 규칙:

- 웨이팅은 야간 부스에만 등록할 수 있다.
- 부스의 웨이팅이 open 상태일 때만 등록할 수 있다.
- 웨이팅 취소는 본인만 가능하다.
- 호출 시 `callCount`를 증가시킨다.
- 상태 전이는 `WAITING -> CALLED -> SEATED` 흐름을 기본으로 하고, 사용자 취소는 `CANCELLED`로 처리한다.

## Priority 10: Test Coverage

기능별 단위/통합 테스트를 보강한다.

- `AuthServiceTest`
  - 회원가입
  - 중복 이메일
  - 비밀번호 hash 저장
  - 로그인 성공/실패
  - JWT claim 검증
- `SecurityConfigTest`
  - public endpoint 접근
  - 인증 필요 endpoint 401
  - 권한 부족 403
- `BoothServiceTest`
  - day/type filter
  - festival admin 등록/삭제
  - booth manager 수정 권한
- `MenuServiceTest`
  - 야간 부스 메뉴 생성 성공
  - 주간 부스 메뉴 생성 실패
  - 품절 처리
- `LocationServiceTest`
  - 날짜/타입별 배치도 조회
  - 위치 등록/수정/삭제
- `WaitingServiceTest`
  - 웨이팅 등록
  - 본인 취소
  - 호출 시 `callCount` 증가
  - 상태 전이 검증
  - 오픈/마감 검증
- `FestivalServiceTest`
  - 축제 정보 조회/수정
  - 공지 등록/조회

CI acceptance 기준은 `./gradlew test` 통과다.

## API Access Policy

### Permit All

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/booths`
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
- `GET /api/festival`
- `GET /api/festival/notices`

### Authenticated User

- `GET /api/users/me`
- `PATCH /api/users/me`
- `DELETE /api/users/me`
- `POST /api/booths/{boothId}/waitings`
- `DELETE /api/waitings/{waitingId}`
- `GET /api/waitings`

### Booth Manager

- `PATCH /api/booths/{boothId}`
- `POST /api/booths/{boothId}/menus`
- `PATCH /api/booths/{boothId}/menus/{menuId}`
- `DELETE /api/booths/{boothId}/menus/{menuId}`
- `POST /api/booths/{boothId}/menus/{menuId}/sold-out`
- `GET /api/booths/{boothId}/waitings`
- `POST /api/waitings/{waitingId}/call`
- `PATCH /api/waitings/{waitingId}/status`
- `PATCH /api/booths/{boothId}/waitings/status`

### Festival Admin

- `POST /api/booths`
- `DELETE /api/booths/{boothId}`
- `POST /api/locations`
- `PATCH /api/locations/{locationId}`
- `DELETE /api/locations/{locationId}`
- `PATCH /api/festival`
- `POST /api/festival/notices`

## Scope Notes

- 공연 시간표 API는 기획서에는 있지만 `API-ENDPOINTS.md`에 없으므로 v1 구현 범위에서 제외한다.
- 공지사항 수정/삭제 API도 v1 API 문서에 없으므로 이번 구현 범위에서 제외한다.
- 이미지 업로드 저장소 연동은 v1 도메인/API 구현 이후 별도 계획으로 분리한다.
- v1 부스 관리자 권한은 `booths.manager_id` 단일 계정만 기준으로 판단한다. `BoothAdminAssignment`는 schema에 남겨두되, 다중 관리자 요구가 생기기 전까지 권한 판정에서는 제외한다.
