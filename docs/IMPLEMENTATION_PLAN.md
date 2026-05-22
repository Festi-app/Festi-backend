# Festi-Backend Implementation Plan

## Summary

Festi-Backend는 대학교 축제 통합 플랫폼의 API 서버다. Spring Boot + Java 기반으로 구현하며, PostgreSQL ERD와 `docs/PATCH.md`의 변경사항을 기준으로 도메인 모델, JWT 인증, 역할 기반 인가, 사용자 조회 API를 구성한다.

현재 `main` 체크아웃 기준으로 공통 인프라, PATCH 기반 도메인 재정렬, 축제별 로그인 ID 기반 인증/JWT, 기본 권한 체계, 모든 인증 사용자용 조회 API, 일반 사용자 즐겨찾기 API, 일반 사용자 웨이팅 등록/조회/취소 API, Swagger/OpenAPI 문서화까지 구현되어 있다.

남은 핵심 작업은 부스 신청/승인 워크플로우, 축제 관리자 변경 API, 부스 관리자 변경 API, 부스 관리자 웨이팅 운영/호출/알림 API, 그리고 현재 구현된 일반 사용자 API의 validation/test 보강이다.

마지막 코드베이스 검증은 2026-05-22에 CGC 전체 인덱스를 재생성한 뒤 수행했다.

| Priority | Scope | Status |
| --- | --- | --- |
| 1 | Spring Boot/Gradle 프로젝트 생성, Java 25 설정, 테스트 워크플로우 수정 | Done |
| 2 | 공통 설정: PostgreSQL, Flyway, JPA auditing, 공통 예외 응답, validation | Done |
| 3 | 초기 ERD 기반 Entity, enum, Repository, migration 작성 | Done |
| 4 | PATCH 반영 User/Auth/JWT 구현 | Done |
| 5 | SecurityConfig와 권한 체계 적용 | Done |
| 6 | 모든 인증 사용자용 조회 API 구현 | Done |
| 7 | PATCH 기반 도메인/migration 재정렬 | Done |
| 8 | 일반 사용자 API 구현 | Done |
| 9 | 부스 신청/승인/삭제 워크플로우 구현 | Next |
| 10 | 축제 관리자 API 구현 | Pending |
| 11 | 부스 관리자 API 구현 | Pending |
| 12 | 부스 관리자 웨이팅 운영 + 알림 API 구현 | Pending |
| 13 | controller/service/repository 테스트 보강 | In Progress |
| 14 | Swagger/OpenAPI 문서화 | Done |

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
- springdoc-openapi
- JUnit 5 / Spring Boot Test / Spring Security Test

## Current Implementation Baseline

### Implemented Entities

- `User`
  - `UserPK(festivalId, id)` composite primary key
  - `festival`, `passwordHash`, `name`, `phone`, `role`, `createdAt`, `updatedAt`
  - email 기반 로그인 정책은 제거됨
- `Booth`
  - `id`, `manager`, `name`, `category`, `type`, `description`, `operatingHours`, `imageUrl`, `isWaitingOpen`, `createdAt`, `updatedAt`
  - `isActive`와 `createdBy`는 제거됨
- `MenuItem`
  - `id`, `booth`, `name`, `price`, `description`, `imageUrl`, `isSoldOut`, `sortOrder`, `createdAt`, `updatedAt`
- `BoothLocation`
  - `id`, `festival`, nullable `booth`, `type`, nullable `index`, `day(FestivalDay)`, `zoneLabel`, `createdAt`, `updatedAt`
  - unique 기준은 `festival_id + zone_label + index + festival_day_id`
- `Waiting`
  - `id`, `booth`, composite-key `user`, `partySize`, `status`, `callCount`, `registeredAt`, `updatedAt`
- `Favorite`
  - `id`, `festival`, `userId`, `booth`, `createdAt`
  - unique 기준은 `festival_id + user_id + booth_id`
- `BoothApplication`
  - `id`, `festival`, `applicantId`, `boothName`, `boothType`, `boothCategory`, `imageUrl`, `description`, `status`, `reviewMemo`, `createdAt`, `updatedAt`
- `Festival`
  - `id`, `name`, `startDate`, `endDate`, `description`, `createdAt`, `updatedAt`
- `FestivalDay`
  - `id`, `festival`, `day`, `dayStart`, `dayEnd`, `nightStart`, `nightEnd`, `createdAt`, `updatedAt`
- `Notice`
  - `id`, `festival`, `title`, `content`, `pinned`, `createdAt`, `updatedAt`
  - `createdBy`와 notice type은 없음
- `Timeline`
  - `id`, `festival`, `day`, `title`, `artist`, `startTime`, `endTime`, `createdAt`, `updatedAt`

### Implemented Enums

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
  - `FOOD_TRUCK`
- `WaitingStatus`
  - `WAITING`
  - `CALLED`
  - `SEATED`
  - `CANCELLED`
- `BoothApplicationStatus`
  - `PENDING`
  - `APPROVED`
  - `REJECTED`

### Implemented Repositories

- `UserRepository`
  - `findByIdAndFestivalId`
  - `existsByIdAndFestivalId`
- `BoothRepository`
  - `findByTypeAndCategory`
  - `findByType`
  - `findByCategory`
- `MenuItemRepository`
  - `findByBoothIdOrderBySortOrder`
- `BoothLocationRepository`
  - `findByDayAndTypeOrderByIndex`
  - `findByDayOrderByIndex`
- `WaitingRepository`
  - `findByUserIdAndFestivalId`
  - `findByUserIdAndFestivalIdOrderByRegisteredAtDesc`
  - `findByBoothIdAndStatusOrderByRegisteredAt`
  - `countByUserIdAndFestivalIdAndStatusIn`
- `FavoriteRepository`
  - `findByFestivalIdAndUserIdOrderByCreatedAtDesc`
  - `existsByFestivalIdAndUserIdAndBoothId`
  - `countByFestivalIdAndUserIdAndBoothType`
- `BoothApplicationRepository`
  - `findByFestivalId`
  - `findByFestivalIdAndApplicantId`
- `FestivalRepository`
- `FestivalDayRepository`
  - `findByFestivalIdOrderByDay`
  - `findByFestivalIdAndDay`
- `NoticeRepository`
  - `findByFestivalIdOrderByPinnedDescCreatedAtDesc`
- `TimelineRepository`
  - `findByFestivalIdOrderByDayAscStartTimeAsc`

`BoothAdminAssignmentRepository`와 `booth_admin_assignments` table은 PATCH 재정렬 후 제거되었다. v1 부스 관리자 소유권은 `booths.manager_id`를 기준으로 판단한다.

### Implemented API Documentation

- `springdoc-openapi-starter-webmvc-ui:3.0.3`
- `OpenApiConfig`
  - API title/version/description
  - JWT bearer security scheme
- Swagger/OpenAPI route allowlist
  - `/swagger-ui.html`
  - `/swagger-ui/**`
  - `/v3/api-docs`
  - `/v3/api-docs/**`
- 현재 controller에는 `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement` 기반 문서화가 적용되어 있다.

### Implemented Migrations

- `V1__baseline.sql`
- `V2__init_schema.sql`
- `V3__users_phone_not_null.sql`
- `V4__domain_realignment.sql`
  - `booth_type`에 `FOOD_TRUCK` 추가
  - `users`를 `festival_id + id` composite PK로 재구성
  - `users.email`, `booths.is_active`, `booths.created_by`, `notices.created_by` 제거
  - `booths.manager`와 `waitings.user`를 composite user FK로 재구성
  - `booth_admin_assignments` table 제거
  - `notices.pinned` 추가
  - `festival_days`, `timelines`, `booth_applications`, `favorites` table 추가
  - `booth_locations`를 `festival_day_id` 기반 슬롯 모델로 재구성

## Priority 1: Project Initialization

Spring Boot 프로젝트의 실행 가능한 최소 구조를 만들었다.

### Completed Deliverables

- `com.festi.backend` 루트 패키지 생성
- Gradle Wrapper 추가
- Java toolchain을 Java 25로 설정
- Spring Boot 4.0.x 의존성 구성
- 기본 `FestiBackendApplication` 추가
- `application.yml` 기본 설정 추가
- GitHub Actions 테스트 워크플로우를 JDK 25 기준으로 수정
- 최소 smoke test 추가

## Priority 2: Common Infrastructure

도메인 Entity와 API 구현 전에 모든 기능이 공유할 공통 기반을 고정했다.

### Completed Deliverables

- PostgreSQL 연결 설정
  - `FESTI_DATABASE_URL`
  - `FESTI_DATABASE_USERNAME`
  - `FESTI_DATABASE_PASSWORD`
  - `ddl-auto: validate`
  - `open-in-view: false`
- 테스트 profile 설정
  - `application-test.yml`
  - H2 기반 context test
- Flyway migration 구조
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
  - validation details는 민감 입력값 노출을 피하기 위해 rejected value를 포함하지 않음

## Priority 3: Initial Entity, Enum, Repository, Migration

초기 ERD 기준 Entity, enum, repository, PostgreSQL migration을 구현했다. 이후 `V4__domain_realignment.sql`에서 PATCH 기반 모델로 재정렬했다.

### Completed Deliverables

- 초기 Entity와 enum 구현
- 초기 JPA Repository 구현
- PostgreSQL schema migration `V2__init_schema.sql`
- 사용자 스키마 보강 migration `V3__users_phone_not_null.sql`
- Lombok 기반 Entity/Common 클래스 보일러플레이트 정리
- 테스트를 H2 fast test(`./gradlew test`)와 PostgreSQL migration test(`./gradlew postgresTest`)로 분리

## Priority 4: PATCH-Aligned User/Auth/JWT

축제별 로그인 ID 기반 회원가입, 로그인, 본인 정보 API가 구현되어 있다.

### Current Behavior

- `POST /api/auth/signup`
  - request: `id`, `password`, `name`, `phone`
  - 기본 role은 `USER`
  - 비밀번호는 BCrypt hash로 저장
  - 같은 축제 안에서 `id` 중복 시 `409`
- `POST /api/auth/login`
  - request: `id`, `password`
  - 로그인 성공 시 JWT access token 발급
- `GET /api/users/me`
- `PATCH /api/users/me`
  - 현재 수정 가능 필드는 `name`, `phone`
- JWT claim
  - `sub`: 사용자 로그인 ID
  - `festivalId`: 축제 ID
  - `role`: `UserRole`
- malformed `role`, malformed `festivalId`, missing subject/required claim은 `401 AUTHENTICATION_REQUIRED`로 처리한다.

### Current Limitation

- 회원가입/로그인은 현재 단일 축제 운영을 전제로 첫 번째 `Festival` row를 사용한다. 다중 축제 선택 또는 축제별 로그인 context 전달은 아직 구현되지 않았다.
- 총 관리자 계정 `admin / pw` 사전 생성은 아직 구현되지 않았다.
- refresh token은 v1 범위에서 제외한다.

### Completed Deliverables

- `AuthDTO`, `UserDTO` 구현
- `AuthController`, `UserController` 구현
- `AuthService`, `UserService` 구현
- `UserPK` composite primary key 적용
- JWT 발급/변환 계층 구현
- DTO / service / JWT converter / JWT issuance / security exception handler 테스트 추가

## Priority 5: Security and Authorization

Spring Security 기반 인증/인가 구조를 적용했다.

### Current Behavior

- 회원가입/로그인은 `permitAll`
- `POST /api/booth-applications`는 보안 정책상 `permitAll`이지만 아직 controller는 없다.
- 모든 인증 사용자 조회 API와 본인 정보 API는 `authenticated`
- 일반 사용자 전용 API는 `ROLE_USER`
- 축제 관리자 API는 `ROLE_FESTIVAL_ADMIN`
- 부스 관리자 API는 `ROLE_BOOTH_MANAGER` 또는 `ROLE_FESTIVAL_ADMIN` coarse gate
- 부스 관리자 권한은 role만 보지 않고 `booths.manager_id`와 현재 사용자 ID 일치를 검증한다.
- `FESTIVAL_ADMIN`은 부스 관리자 권한도 통과한다.
- 문서에 정의되지 않은 라우트는 기본 `denyAll`

### Completed Deliverables

- `SecurityConfig`에 API Access Policy 반영
- `AuthenticatedUser`
  - `id`
  - `festivalId`
  - `role`
- `BoothAuthorizationService`
  - `AuthenticatedUser`와 `Booth` 엔티티를 기준으로 부스 소유권 검증
  - `FESTIVAL_ADMIN` 우회 허용
  - `BOOTH_MANAGER`는 `booths.manager_id`와 현재 사용자 id가 일치할 때만 허용
- `SecurityRoutePolicyIntegrationTest`
- `BoothAuthorizationServiceTest`

## Priority 6: All Authenticated Read APIs

모든 인증 사용자가 조회할 수 있는 API를 구현했다.

### Implemented APIs

- `GET /api/booths`
  - 선택 필터: `day`, `type`, `category`
  - `day`가 있으면 `FestivalDay`와 `BoothLocation` 기반으로 배치된 부스를 조회한다.
  - `day`가 없으면 `type`, `category` 기반으로 부스를 조회한다.
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
  - 필수 필터: `day`, `type`
  - 배정되지 않은 slot도 응답에 포함할 수 있다.
- `GET /api/festival`
- `GET /api/festival/notices`
  - `pinned` 우선, 같은 그룹 안에서는 최신순
- `GET /api/festival/timelines`
  - 축제 일자, 시작 시간 순
- `GET /api/users/me`
- `PATCH /api/users/me`

### Completed Deliverables

- 조회 전용 DTO / Service / Controller 구현
  - `BoothDTO`, `MenuDTO`, `LocationDTO`, `FestivalDTO`, `FestivalDayDTO`, `NoticeDTO`, `TimelineDTO`, `WaitingDTO`
- 조회 API 구현
  - 부스 목록 / 상세 / 메뉴
  - 배치도
  - 축제 정보 / 공지사항 / 공연 타임라인
  - 본인 정보 조회/수정
- 조회 정책 반영
  - 부스 목록은 `day`, `type`, `category` 조합 필터 지원
  - 배치도는 `day`, `type` 필수
  - 목록은 빈 배열, 단건은 미존재 시 `404`

## Priority 7: PATCH-Based Domain Realignment

`docs/PATCH.md`의 변경사항을 반영해 도메인과 migration을 재정렬했다.

### Completed Deliverables

- User/Auth
  - UUID 기반 `User.id`와 email 로그인 정책 제거
  - 사용자 로그인 ID 문자열을 사용자 식별자로 사용
  - `festival_id + id` composite primary key 적용
  - 사용자 FK를 가진 테이블은 축제별 사용자 식별자를 참조하도록 재설계
- Booth / BoothApplication
  - `Booth.active` / `is_active` 제거
  - `BoothRepository`의 active 기반 조회 제거
  - `BoothApplication` entity, enum, repository, table 추가
  - `BoothApplicationStatus`는 `PENDING`, `APPROVED`, `REJECTED`
- Booth / Food Truck / Location
  - `BoothType.FOOD_TRUCK` 추가
  - 푸드트럭은 내부적으로 `Booth`로 표현
  - `BoothLocation`을 축제별 `FestivalDay` slot 모델로 재구성
  - 여러 `BoothLocation` row가 같은 `booth_id`를 가질 수 있는 구조 반영
- Favorite
  - `Favorite` entity, repository, table 추가
  - 사용자당 부스 중복 즐겨찾기 방지 unique 제약 추가
  - 생성 시각 기록
- Festival / Notice / Timeline
  - `FestivalDay` entity, repository, table 추가
  - `Notice.pinned` 추가
  - `Timeline` entity, repository, table 추가
- Waiting
  - `Waiting.user`를 축제별 composite user FK로 재구성
  - 사용자별 웨이팅 조회 repository 추가

### Current Limitation

- `BoothApplication`은 domain/repository/migration만 구현되어 있고 신청/승인/거절/삭제 service/controller는 아직 없다.
- 배치도 slot 생성/배정/취소 API는 아직 없다.
- 푸드트럭 manager를 축제 관리자 계정으로 배정하는 생성 로직은 아직 없다.
- 부스 관리자용 웨이팅 목록/호출/상태 변경/알림 로직은 아직 없다.

## Priority 8: General User APIs

일반 사용자 전용 API 중 즐겨찾기와 웨이팅 등록/조회/취소가 구현되어 있다.

### Implemented APIs

- `POST /api/favorites`
  - `USER`만 접근 가능
  - 같은 부스 중복 등록 시 `409`
  - 부스 타입별 최대 5개 제한
- `GET /api/favorites`
  - `USER`만 접근 가능
  - 생성 시각 역순 조회
- `DELETE /api/favorites/{favoriteId}`
  - `USER`만 접근 가능
  - 본인 즐겨찾기만 hard delete
- `GET /api/waitings`
  - `USER`만 접근 가능
  - 현재 사용자와 축제 ID 기준 조회
  - `booth` fetch plan 적용
- `POST /api/booths/{boothId}/waitings`
  - `USER`만 접근 가능
  - `NIGHT` 부스만 등록 가능
  - 부스의 `isWaitingOpen`이 true일 때만 등록 가능
  - 현재 사용자 기준 active waiting(`WAITING`, `CALLED`) 최대 3개 제한
- `DELETE /api/waitings/{waitingId}`
  - `USER`만 접근 가능
  - 본인 웨이팅만 취소 가능
  - `WAITING`, `CALLED` 상태만 취소 가능

### Follow-Up Hardening

- 즐겨찾기 request validation 보강
- `FavoriteServiceTest` / favorite controller integration test 추가
- `WaitingServiceTest`의 등록/취소 정책 테스트 추가
- 웨이팅 controller integration test 추가

## Priority 9: Booth Application Workflow

부스 신청과 승인 워크플로우를 구현한다. 현재는 domain/repository/security route policy까지만 준비되어 있다.

### APIs To Implement

#### Permit All

- `POST /api/booth-applications`
  - 부스 관리자 회원가입과 부스 신청을 동시에 처리한다.
  - 성공 시 `BOOTH_MANAGER` 계정과 `BoothApplication`을 함께 생성한다.
  - 신청 상태는 `PENDING`으로 시작한다.

#### Booth Manager

- `GET /api/booth-applications/me`
  - 현재 부스 관리자 계정의 신청 상태를 조회한다.

#### Festival Admin

- `GET /api/admin/booth-applications`
- `GET /api/admin/booth-applications/{applicationId}`
- `POST /api/admin/booth-applications/{applicationId}/approve`
- `POST /api/admin/booth-applications/{applicationId}/reject`
- `DELETE /api/admin/booth-applications/{applicationId}`

### Validation Rules

- 승인 전 신청만 삭제할 수 있다.
- 신청 삭제 시 신청과 함께 생성된 `BOOTH_MANAGER` 계정도 hard delete한다.
- 승인된 신청은 삭제할 수 없고 `409 Conflict`를 반환한다.
- 승인 시 실제 `Booth`를 생성한다.
- 승인된 신청의 manager 계정은 생성된 `Booth.manager`가 된다.
- 거절된 신청은 승인 전 신청과 동일하게 삭제 가능 대상으로 본다.
- 운영 중/운영 후 부스 삭제는 지원하지 않는다.

## Priority 10: Festival Admin APIs

축제 관리자 권한이 필요한 API를 구현한다. 현재는 `SecurityConfig` route policy만 적용되어 있고 controller/service는 없다.

### APIs To Implement

- `PATCH /api/festival`
- `POST /api/festival/days`
- `PATCH /api/festival/days/{festivalDayId}`
- `DELETE /api/festival/days/{festivalDayId}`
- `POST /api/festival/notices`
- `PATCH /api/festival/notices/{noticeId}`
- `DELETE /api/festival/notices/{noticeId}`
- `POST /api/festival/timelines`
- `PATCH /api/festival/timelines/{timelineId}`
- `DELETE /api/festival/timelines/{timelineId}`
- `POST /api/locations/slots`
- `POST /api/locations/{locationId}/assignment`
- `DELETE /api/locations/{locationId}/assignment`

### Validation Rules

- 공지는 pinned 여러 개를 허용한다.
- 공지 목록은 pinned 우선, 같은 그룹 안에서는 작성일 순으로 정렬한다.
- 배치도 슬롯 생성은 프론트가 전달한 구역별 칸 수 정보를 기준으로 한다.
- 이미 부스가 배정된 슬롯에는 다른 부스를 바로 배정할 수 없다.
- 슬롯 배정을 바꾸려면 기존 배정을 먼저 취소한다.
- 승인된 부스 자체를 삭제하는 API는 제공하지 않는다.

## Priority 11: Booth Manager APIs

부스 관리자 권한이 필요한 API를 구현한다. 현재는 `SecurityConfig` route policy와 `BoothAuthorizationService`만 준비되어 있고 controller/service는 없다.

### APIs To Implement

- `PATCH /api/booths/{boothId}`
- `POST /api/booths/{boothId}/menus`
- `PATCH /api/booths/{boothId}/menus/{menuId}`
- `DELETE /api/booths/{boothId}/menus/{menuId}`
- `POST /api/booths/{boothId}/menus/{menuId}/sold-out`

### Validation Rules

- `BOOTH_MANAGER`는 본인 담당 부스만 수정할 수 있다.
- `FESTIVAL_ADMIN`은 부스 관리자 API를 우회 통과할 수 있다.
- 메뉴 생성/수정은 `NIGHT` 부스에서만 허용한다.
- `FOOD_TRUCK`은 내부적으로 `Booth`지만 부스 관리자 계정이 아닌 축제 관리자 계정이 manager다.

## Priority 12: Booth Manager Waiting Operations and Notifications

일반 사용자용 웨이팅 등록/조회/취소는 구현되어 있다. 남은 범위는 부스 관리자용 웨이팅 목록/호출/상태 변경, 웨이팅 오픈/마감, 호출 알림 전송이다.

### Implemented General User APIs

- `POST /api/booths/{boothId}/waitings`
- `GET /api/waitings`
- `DELETE /api/waitings/{waitingId}`

### APIs To Implement

- `GET /api/booths/{boothId}/waitings`
- `POST /api/waitings/{waitingId}/call`
- `PATCH /api/waitings/{waitingId}/status`
- `PATCH /api/booths/{boothId}/waitings/status`

### Implemented Validation Rules

- 웨이팅 등록은 `USER`만 가능하다.
- 웨이팅은 `NIGHT` 부스에만 등록할 수 있다.
- `FOOD_TRUCK`은 `NIGHT`가 아니므로 웨이팅 등록 대상이 아니다.
- 사용자당 최대 3개까지 웨이팅을 등록할 수 있다.
- 부스의 웨이팅이 open 상태일 때만 등록할 수 있다.
- 웨이팅 취소는 본인만 가능하다.
- 취소 가능한 상태는 `WAITING`, `CALLED`다.

### Validation Rules To Implement

- 호출 시 `callCount`를 증가시킨다.
- 호출 시 서버는 사용자 앱으로 알림을 전송한다.
- 상태 전이는 `WAITING -> CALLED -> SEATED` 흐름을 기본으로 하고, 사용자 취소는 `CANCELLED`로 처리한다.
- 사용자 앱의 웨이팅 정보 갱신은 별도 push state sync가 아니라 새로고침 기반 조회로 처리한다.

## Priority 13: Test Coverage

현재 구현된 slice에 대한 단위/통합 테스트가 추가되어 있다. 남은 API 구현 시 각 service/controller/security 경계를 함께 보강한다.

### Current Tests

- `FestiBackendApplicationTests`
- `PostgresMigrationApplicationTests`
- `AuthDTOTest`
- `AuthServiceTest`
- `AuthUserControllerIntegrationTest`
- `UserDTOTest`
- `UserServiceTest`
- `HmacJwtTokenServiceTest`
- `JwtAuthenticationConverterTest`
- `SecurityExceptionHandlersTest`
- `SecurityRoutePolicyIntegrationTest`
- `BoothAuthorizationServiceTest`
- `BoothServiceTest`
- `MenuServiceTest`
- `LocationServiceTest`
- `FestivalServiceTest`
- `WaitingServiceTest`
- `RepositoryFetchPlanTest`
- `GlobalExceptionHandlerTest`
- `ErrorResponseTest`

### Tests To Add Or Expand

- `BoothApplicationServiceTest`
  - 신청 생성 시 `BOOTH_MANAGER` 계정 생성
  - 승인 전 신청 삭제 시 신청과 계정 hard delete
  - 승인된 신청 삭제 시 `409 Conflict`
  - 승인 시 `Booth` 생성 및 manager 연결
  - 거절 시 검토 메모 저장
- `BoothApplicationControllerIntegrationTest`
  - permit all 신청 endpoint
  - 부스 관리자 본인 신청 조회
  - 축제 관리자 신청 목록/상세/승인/거절/삭제
- `FavoriteServiceTest`
  - 타입별 5개 제한
  - 생성 시각 정렬
  - hard delete
  - 중복 등록 차단
- `FavoriteControllerIntegrationTest`
  - `USER` 접근 허용
  - `BOOTH_MANAGER` / `FESTIVAL_ADMIN` 접근 시 `403`
- `BoothServiceTest` 확장
  - 담당 부스 수정 권한
  - `FOOD_TRUCK` 타입 조회/관리자 배정
- `MenuServiceTest` 확장
  - 야간 부스 메뉴 생성 성공
  - 주간 부스 메뉴 생성 실패
  - 품절 처리
- `LocationServiceTest` 확장
  - 구역별 슬롯 생성
  - `festival_id + zone_label + index + festival_day_id` unique 검증
  - 여러 슬롯에 같은 부스 배정
  - 이미 배정된 슬롯 중복 배정 실패
  - 배정 취소
- `FestivalServiceTest` 확장
  - 축제 정보 수정
  - 축제 일자별 주간/야간 운영 시간 관리
  - 공지 등록/수정/삭제/조회
  - 공연 타임라인 등록/수정/삭제/조회
- `WaitingServiceTest` 확장
  - 일반 사용자 웨이팅 등록
  - 일반 사용자 외 role 등록 실패
  - 사용자당 최대 3개 제한
  - 야간 부스만 등록 가능
  - 본인 취소
  - 호출 시 `callCount` 증가
  - 호출 시 알림 서비스 호출
  - 상태 전이 검증
  - 오픈/마감 검증

CI acceptance 기준은 `./gradlew test` 통과다. DB migration 검증이 필요한 변경은 `./gradlew postgresTest`도 통과해야 한다.

## Priority 14: Swagger/OpenAPI Documentation

현재 구현된 controller는 Swagger/OpenAPI 주석이 적용되어 있고, OpenAPI metadata와 JWT bearer security scheme이 설정되어 있다.

### Completed Deliverables

- `springdoc-openapi-starter-webmvc-ui:3.0.3` 의존성 추가
- `OpenApiConfig` 추가
  - API title/version/description 설정
  - `bearerAuth` JWT security scheme 설정
- `SecurityConfig`에서 Swagger/OpenAPI 문서 route를 `permitAll` 처리
- 현재 controller 문서화
  - `AuthController`
  - `UserController`
  - `BoothController`
  - `MenuController`
  - `LocationController`
  - `FestivalController`
  - `FavoriteController`
  - `WaitingController`

### Follow-Up

- Priority 9-12에서 새 controller를 추가할 때 같은 OpenAPI annotation 기준을 적용한다.
- 구현되지 않은 endpoint가 controller에 추가되면 `docs/API-ENDPOINTS.md`와 Swagger 설명을 함께 갱신한다.

## API Access Policy

### Permit All

- `GET /swagger-ui.html` - Implemented
- `GET /swagger-ui/**` - Implemented
- `GET /v3/api-docs` - Implemented
- `GET /v3/api-docs/**` - Implemented
- `POST /api/auth/signup` - Implemented
- `POST /api/auth/login` - Implemented
- `POST /api/booth-applications` - Security policy only, controller pending

### All Authenticated Users

- `GET /api/booths` - Implemented
- `GET /api/booths/{boothId}` - Implemented
- `GET /api/booths/{boothId}/menus` - Implemented
- `GET /api/locations` - Implemented
- `GET /api/festival` - Implemented
- `GET /api/festival/notices` - Implemented
- `GET /api/festival/timelines` - Implemented
- `GET /api/users/me` - Implemented
- `PATCH /api/users/me` - Implemented

### General User Only

- `POST /api/favorites` - Implemented
- `GET /api/favorites` - Implemented
- `DELETE /api/favorites/{favoriteId}` - Implemented
- `POST /api/booths/{boothId}/waitings` - Implemented
- `DELETE /api/waitings/{waitingId}` - Implemented
- `GET /api/waitings` - Implemented

### Booth Manager

- `GET /api/booth-applications/me` - Security policy only, controller pending
- `PATCH /api/booths/{boothId}` - Security policy only, controller pending
- `POST /api/booths/{boothId}/menus` - Security policy only, controller pending
- `PATCH /api/booths/{boothId}/menus/{menuId}` - Security policy only, controller pending
- `DELETE /api/booths/{boothId}/menus/{menuId}` - Security policy only, controller pending
- `POST /api/booths/{boothId}/menus/{menuId}/sold-out` - Security policy only, controller pending
- `GET /api/booths/{boothId}/waitings` - Security policy only, controller pending
- `POST /api/waitings/{waitingId}/call` - Security policy only, controller pending
- `PATCH /api/waitings/{waitingId}/status` - Security policy only, controller pending
- `PATCH /api/booths/{boothId}/waitings/status` - Security policy only, controller pending

### Festival Admin

- `PATCH /api/festival` - Security policy only, controller pending
- `POST /api/festival/days` - Security policy only, controller pending
- `PATCH /api/festival/days/{festivalDayId}` - Security policy only, controller pending
- `DELETE /api/festival/days/{festivalDayId}` - Security policy only, controller pending
- `POST /api/festival/notices` - Security policy only, controller pending
- `PATCH /api/festival/notices/{noticeId}` - Security policy only, controller pending
- `DELETE /api/festival/notices/{noticeId}` - Security policy only, controller pending
- `POST /api/festival/timelines` - Security policy only, controller pending
- `PATCH /api/festival/timelines/{timelineId}` - Security policy only, controller pending
- `DELETE /api/festival/timelines/{timelineId}` - Security policy only, controller pending
- `POST /api/locations/slots` - Security policy only, controller pending
- `POST /api/locations/{locationId}/assignment` - Security policy only, controller pending
- `DELETE /api/locations/{locationId}/assignment` - Security policy only, controller pending
- `GET /api/admin/booth-applications` - Security policy only, controller pending
- `GET /api/admin/booth-applications/{applicationId}` - Security policy only, controller pending
- `POST /api/admin/booth-applications/{applicationId}/approve` - Security policy only, controller pending
- `POST /api/admin/booth-applications/{applicationId}/reject` - Security policy only, controller pending
- `DELETE /api/admin/booth-applications/{applicationId}` - Security policy only, controller pending

## Scope Notes

- `Booth.active` / `is_active`는 제거되었다.
- `BoothAdminAssignment`는 제거되었고, v1 권한 판정에서는 `booths.manager_id`를 사용한다.
- 축제 운영 중/운영 후 부스 삭제는 지원하지 않는다.
- 승인된 `Booth`는 삭제 대상이 아니다.
- 삭제 가능한 것은 승인 전 `BoothApplication`뿐이다.
- 신청 삭제 시 신청과 함께 생성된 `BOOTH_MANAGER` 계정도 hard delete한다.
- `BOOTH_MANAGER`는 전역 role로 유지한다.
- 부스 관리자 계정은 일반 사용자 계정과 재사용하지 않는다.
- `FOOD_TRUCK`은 내부적으로 `Booth`로 표현하되 축제 관리자 계정을 manager로 배정하는 생성 로직이 필요하다.
- 이미지 업로드 저장소 연동은 v1 도메인/API 구현 이후 별도 계획으로 분리한다.
- `docs/API-ENDPOINTS.md`도 현재 구현 상태와 PATCH 이후 권한 구분에 맞춰 별도 갱신이 필요하다.
