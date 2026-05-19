# Festi-Backend Implementation Plan

## Summary

Festi-Backend는 대학교 축제 통합 플랫폼의 API 서버다. Spring Boot + Java 기반으로 구현하며, PostgreSQL ERD를 기준으로 도메인 모델을 구성하고 JWT 기반 인증/인가를 적용한다.

현재 `main` 기준으로 공통 인프라, 도메인 모델, User/Auth/JWT, 기본 권한 체계, 모든 인증 사용자용 조회 API까지 구현되어 있다. 다만 `docs/PATCH.md`의 기획 변경으로 사용자 식별자, 축제별 계정, 부스 신청/삭제, 배치도, 즐겨찾기, 축제 일정/공지/공연, 웨이팅 정책을 재정렬해야 한다.

| Priority | Scope | Status |
| --- | --- | --- |
| 1 | Spring Boot/Gradle 프로젝트 생성, Java 25 설정, 테스트 워크플로우 수정 | Done |
| 2 | 공통 설정: PostgreSQL, Flyway, JPA auditing, 공통 예외 응답, validation | Done |
| 3 | ERD 기반 Entity, enum, Repository, migration 작성 | Done |
| 4 | User/Auth/JWT 구현 | Done |
| 5 | SecurityConfig와 권한 체계 적용 | Done |
| 6 | 모든 인증 사용자용 조회 API 구현 | Done |
| 7 | PATCH 기반 도메인 재정렬 | Next |
| 8 | 모든 인증 사용자 API 재정렬 + 일반 사용자 API 분리 | Pending |
| 9 | 부스 신청/승인/삭제 워크플로우 구현 | Pending |
| 10 | 축제 관리자 API 구현 | Pending |
| 11 | 부스 관리자 API 구현 | Pending |
| 12 | 웨이팅 + 알림 API 구현 | Pending |
| 13 | controller/service/repository 테스트 보강 | Pending |

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

초기 ERD를 PostgreSQL 기준으로 구현했다. PATCH 이후 목표 모델은 Priority 7에서 재정렬한다.

### 현재 구현된 Entities

- `User`
  - 현재: UUID `id`, `email`, `passwordHash`, `name`, `phone`, `role`, `createdAt`, `updatedAt`
  - PATCH 이후: 축제별 로그인 ID 기반 계정으로 변경 예정
- `Booth`
  - 현재: `id`, `manager`, `createdBy`, `name`, `category`, `type`, `description`, `operatingHours`, `imageUrl`, `isActive`, `isWaitingOpen`, `createdAt`, `updatedAt`
  - PATCH 이후: `isActive` 제거 예정
- `MenuItem`
  - `id`, `booth`, `name`, `price`, `description`, `imageUrl`, `isSoldOut`, `sortOrder`, `createdAt`, `updatedAt`
- `BoothLocation`
  - 현재: `id`, `booth`, `type`, `index`, `day`, `zoneLabel`, `createdAt`, `updatedAt`
  - PATCH 이후: 축제별 슬롯, `zoneLabel + index + day` unique, 다중 슬롯 배정 지원 예정
- `Waiting`
  - `id`, `booth`, `user`, `partySize`, `status`, `callCount`, `registeredAt`, `updatedAt`
- `BoothAdminAssignment`
  - 현재 schema에는 남아 있으나 v1 권한 판정에서는 사용하지 않는다.
- `Festival`
  - `id`, `name`, `startDate`, `endDate`, `description`, `createdAt`, `updatedAt`
- `Notice`
  - 현재: `id`, `festival`, `title`, `content`, `createdBy`, `createdAt`, `updatedAt`
  - PATCH 이후: `pinned` 추가, 유형 필드는 추가하지 않는다.

### 현재 구현된 Enums

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
  - 현재: `DAY`, `NIGHT`
  - PATCH 이후: `FOOD_TRUCK` 추가 예정
- `WaitingStatus`
  - `WAITING`
  - `CALLED`
  - `SEATED`
  - `CANCELLED`

### 현재 구현된 Repositories

- `UserRepository`
  - 현재: `findByEmail`, `existsByEmail`
  - PATCH 이후: 축제별 로그인 ID 기반 조회로 변경 예정
- `BoothRepository`
  - 현재: type/category/active 기반 조회
  - PATCH 이후: active 조건 제거 예정
- `MenuItemRepository`
  - `findByBoothIdOrderBySortOrder`
- `BoothLocationRepository`
  - 현재: `findByDayAndTypeOrderByIndex`
  - PATCH 이후: 축제와 zone/index 기준 조회로 변경 예정
- `WaitingRepository`
  - `findByUserId`
  - `findByBoothIdAndStatusOrderByRegisteredAt`
- `BoothAdminAssignmentRepository`
  - 현재 schema에는 남아 있으나 v1 권한 판정에서는 사용하지 않는다.
- `FestivalRepository`
- `NoticeRepository`

### Completed Deliverables

- `User`, `Booth`, `MenuItem`, `BoothLocation`, `Waiting`, `BoothAdminAssignment`, `Festival`, `Notice` Entity와 관련 enum 구현
- 8개 JPA Repository 구현
- PostgreSQL schema migration `V2__init_schema.sql`
- 사용자 스키마 보강 migration `V3__users_phone_not_null.sql`
- Lombok 기반 Entity/Common 클래스 보일러플레이트 정리
- 테스트를 H2 fast test(`./gradlew test`)와 PostgreSQL migration test(`./gradlew postgresTest`)로 분리

## Priority 4: User/Auth/JWT

현재는 email 기반 회원가입, 로그인, 본인 정보 API가 구현되어 있다. PATCH 이후에는 축제별 로그인 ID 기반 인증으로 재정렬한다.

- 현재 구현
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `GET /api/users/me`
  - `PATCH /api/users/me`
  - `DELETE /api/users/me`
  - 회원가입 시 기본 role은 `USER`
  - 비밀번호는 BCrypt hash로 저장
  - 로그인 성공 시 JWT access token 발급
- PATCH 이후 변경
  - 회원가입 필드는 `id`, `password`, `phone`, `name`으로 변경한다.
  - 기존 email 로그인 정책은 제거한다.
  - 사용자 계정은 축제에 bound된다.
  - 같은 로그인 ID라도 축제가 다르면 별도 계정으로 취급한다.
  - JWT subject는 축제별 사용자 식별자를 표현해야 한다.
  - JWT claim에는 사용자 role과 festival 식별자를 포함한다.
  - refresh token은 v1 범위에서 제외한다.

### Completed Deliverables

- `AuthDTO`, `UserDTO` 구현
- `AuthController`, `UserController` 구현
- `AuthService`, `UserService` 구현
- JWT 발급/변환 계층 구현
- DTO / service / JWT converter / JWT issuance / security exception handler 테스트 추가

## Priority 5: Security and Authorization

Spring Security 기반 인증/인가 구조를 확정했다. PATCH 이후에는 모든 인증 사용자 API와 일반 사용자 전용 API를 분리한다.

- 현재 구현
  - 회원가입/로그인만 `permitAll`
  - 조회 API와 본인 정보 API는 `authenticated`
  - 축제 관리자 API는 `FESTIVAL_ADMIN`
  - 부스 관리자 API는 `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` coarse gate
  - 부스 관리자 권한은 role만 보지 않고 `booths.manager_id`와 현재 사용자 일치를 검증
  - `FESTIVAL_ADMIN`은 부스 관리자 권한도 통과
  - 문서에 정의되지 않은 라우트는 기본 `denyAll`

- PATCH 이후 권한 계층
  - `Permit All`: 로그인, 일반 사용자 회원가입, 부스 신청 + 부스 관리자 회원가입
  - `All Authenticated Users`: 축제 정보, 공지, 공연 타임라인, 부스/메뉴/배치도 조회, 내 정보 조회/수정
  - `General User Only`: 즐겨찾기, 웨이팅 등록/취소/내 웨이팅 조회
  - `Booth Manager`: 본인 담당 부스 수정, 메뉴 관리, 담당 부스 웨이팅 관리
  - `Festival Admin`: 축제 설정, 축제 일자/운영시간, 배치도 슬롯 생성/배정, 부스 신청 승인/거절, 공지/공연 관리

### Completed Deliverables

- `SecurityConfig`에 API Access Policy 반영
- `BoothAuthorizationService`
  - `AuthenticatedUser`와 `Booth` 엔티티를 기준으로 부스 소유권 검증
  - `FESTIVAL_ADMIN` 우회 허용
  - `BOOTH_MANAGER`는 `booths.manager_id`와 현재 사용자 id가 일치할 때만 허용
  - `BoothAdminAssignmentRepository`는 v1 권한 판정에서 미사용
- `SecurityRoutePolicyIntegrationTest`
- `BoothAuthorizationServiceTest`

## Priority 6: All Authenticated Read APIs

모든 인증 사용자가 조회할 수 있는 API를 구현했다. 기존 문서의 “일반 사용자 이상” 표현은 PATCH 이후 “모든 인증 사용자”로 정규화한다.

- `GET /api/booths`
  - 선택 필터: `day`, `type`, `category`
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
  - 필수 필터: `day`, `type`
- `GET /api/festival`
- `GET /api/festival/notices`

### Completed Deliverables

- 조회 전용 DTO / Service / Controller 구현
  - `BoothDTO`, `MenuDTO`, `LocationDTO`, `FestivalDTO`, `NoticeDTO`, `WaitingDTO`
- 조회 API 구현
  - 부스 목록 / 상세 / 메뉴
  - 배치도
  - 축제 정보 / 공지사항
- 조회 정책 반영
  - 부스 목록은 `day`, `type`, `category` 조합 필터 지원
  - 배치도는 `day`, `type` 필수
  - 공지사항은 최신순
  - 목록은 빈 배열, 단건은 미존재 시 `404`

## Priority 7: PATCH-Based Domain Realignment

`docs/PATCH.md`의 변경사항을 반영해 도메인과 migration을 먼저 재정렬한다. 이 단계는 이후 API 구현 전에 반드시 선행한다.

### User / Auth

- UUID 기반 `User.id`와 email 로그인 정책을 폐기한다.
- 사용자 로그인 ID 문자열을 사용자 식별자로 사용한다.
- 계정은 축제에 bound된다.
- 동일 로그인 ID는 축제가 다르면 별도 계정으로 취급한다.
- DB 식별 기준은 `festival_id + user_id` 조합으로 설계한다.
- 사용자 FK를 가진 테이블은 축제별 사용자 식별자를 참조하도록 재설계한다.
- 회원가입 필드는 다음 4가지다.
  - `id`: 로그인 ID
  - `password`: BCrypt hash 저장
  - `phone`: 웨이팅 연락용 전화번호
  - `name`: 일반 사용자는 사용자 이름, 부스 관리자는 대표자 이름
- 총 관리자 계정은 각 축제에 bound된 `admin / pw` 계정으로 사전 생성한다.
- `UserRole`은 유지한다.
  - `USER`
  - `BOOTH_MANAGER`
  - `FESTIVAL_ADMIN`

### Booth / BoothApplication

- `Booth.active` / `is_active`는 제거한다.
- `BoothRepository`의 active 기반 조회 메서드를 제거한다.
- 부스 조회 서비스는 active 조건 없이 조회하되, 승인된 `Booth`만 생성되는 구조로 보장한다.
- 신청 시점에는 `Booth`가 아니라 `BoothApplication`을 생성한다.
- 부스 관리자 계정은 `BoothApplication` 생성과 동시에 만들어진다.
- `BoothApplication`은 최소한 다음 정보를 가진다.
  - 축제
  - 신청자/대표자 계정
  - 부스명
  - 부스 타입
  - 부스 카테고리
  - 이미지 URL optional
  - 설명 optional
  - 신청 상태: `PENDING`, `APPROVED`, `REJECTED`
  - 검토 메모 optional
- 신청 삭제는 승인 전 `BoothApplication`에만 허용한다.
- 신청 삭제 시 연결된 `BOOTH_MANAGER` 계정도 hard delete한다.
- 승인 후에는 실제 `Booth`를 생성하고 manager를 신청 시 생성된 `BOOTH_MANAGER` 계정으로 배정한다.
- 승인된 `Booth`의 삭제 API는 제공하지 않는다.
- 기존 `DELETE /api/booths/{boothId}` 계획은 제거하고, 승인 전 신청 삭제 API로 대체한다.

### Booth / Food Truck / Location

- `BoothType`에 `FOOD_TRUCK`을 추가한다.
- 푸드트럭은 내부적으로 `Booth`로 표현한다.
- 푸드트럭 manager는 해당 축제의 `FESTIVAL_ADMIN` 계정으로 배정한다.
- 부스는 여러 연속 칸에 배치될 수 있다.
- 여러 `BoothLocation` row가 같은 `booth_id`를 가질 수 있다.
- `BoothLocation`은 축제별 슬롯이다.
- `BoothLocation` unique 기준은 `festival_id + zone_label + index + day`다.
- 프론트가 구역별 칸 수 정보를 전달하면 백엔드가 이를 기반으로 `BoothLocation` 슬롯을 생성한다.
- 이미 배정된 슬롯에는 다른 부스를 직접 덮어쓸 수 없다. 먼저 배정을 취소해야 한다.

### Favorite

- `Favorite` 도메인을 추가한다.
- 일반 사용자만 즐겨찾기를 사용할 수 있다.
- 사용자당 `DAY`, `NIGHT`, `FOOD_TRUCK` 각각 5개까지 등록할 수 있다.
- 정렬 순서를 위해 즐겨찾기 생성 시점을 기록한다.
- 즐겨찾기 삭제는 hard delete다.

### Festival / Notice / Timeline

- `FestivalDay` 도메인을 추가한다.
- 각 축제 일자별 주간/야간 운영 시간대를 설정한다.
- `Notice`에는 유형 필드를 두지 않는다.
- 공지 상세보기 페이지는 제공하지 않는 전제이므로 목록 응답에 title/content를 함께 제공한다.
- `Notice.pinned`를 추가한다.
- 상단 고정은 여러 개 가능하다.
- 공지 정렬은 pinned 우선, 같은 그룹 안에서는 작성일 순으로 한다.
- `Timeline` 도메인을 추가한다.
- 공연 시간표는 축제 일자별로 관리한다.
- `Timeline` 필드는 다음을 포함한다.
  - 공연명
  - 아티스트 또는 팀명
  - 공연 시작 시간
  - 공연 종료 시간

### Waiting

- 사용자당 최대 3개까지 웨이팅을 등록할 수 있다.
- 웨이팅은 `NIGHT` 부스에서만 가능하다.
- `FOOD_TRUCK`은 웨이팅 대상이 아니다.
- 웨이팅 정보 갱신은 사용자의 화면 새로고침 기반이다.
- 웨이팅 호출 알림은 서버에서 사용자 앱으로 전송한다.

## Priority 8: All Authenticated APIs and General User APIs

Priority 6에서 구현된 조회 API를 PATCH 이후 권한 모델과 응답 모델에 맞게 재정렬하고, 일반 사용자 전용 API를 추가한다.

### All Authenticated Users

- `GET /api/booths`
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
- `GET /api/festival`
- `GET /api/festival/notices`
- `GET /api/festival/timelines`
- `GET /api/users/me`
- `PATCH /api/users/me`

### General User Only

- `POST /api/favorites`
- `GET /api/favorites`
- `DELETE /api/favorites/{favoriteId}`
- `POST /api/booths/{boothId}/waitings`
- `DELETE /api/waitings/{waitingId}`
- `GET /api/waitings`

검증 규칙:

- `USER`만 일반 사용자 전용 API를 호출할 수 있다.
- `BOOTH_MANAGER`와 `FESTIVAL_ADMIN`은 일반 사용자 전용 API에서 `403`이다.
- 즐겨찾기는 부스 타입별 최대 5개 제한을 적용한다.
- 즐겨찾기 목록은 생성 시각 기준으로 정렬한다.
- 즐겨찾기 삭제는 hard delete다.

## Priority 9: Booth Application Workflow

부스 신청과 승인 워크플로우를 구현한다.

### Permit All

- `POST /api/booth-applications`
  - 부스 관리자 회원가입과 부스 신청을 동시에 처리한다.
  - 성공 시 `BOOTH_MANAGER` 계정과 `BoothApplication`을 함께 생성한다.
  - 신청 상태는 `PENDING`으로 시작한다.

### Booth Manager

- `GET /api/booth-applications/me`
  - 현재 부스 관리자 계정의 신청 상태를 조회한다.

### Festival Admin

- `GET /api/admin/booth-applications`
- `GET /api/admin/booth-applications/{applicationId}`
- `POST /api/admin/booth-applications/{applicationId}/approve`
- `POST /api/admin/booth-applications/{applicationId}/reject`
- `DELETE /api/admin/booth-applications/{applicationId}`

검증 규칙:

- 승인 전 신청만 삭제할 수 있다.
- 신청 삭제 시 신청과 함께 생성된 `BOOTH_MANAGER` 계정도 hard delete한다.
- 승인된 신청은 삭제할 수 없고 `409 Conflict`를 반환한다.
- 승인 시 실제 `Booth`를 생성한다.
- 승인된 신청의 manager 계정은 생성된 `Booth.manager`가 된다.
- 거절된 신청은 삭제 가능 여부를 승인 전과 동일하게 본다.
- 운영 중/운영 후 부스 삭제는 지원하지 않는다.

## Priority 10: Festival Admin APIs

축제 관리자 권한이 필요한 API를 구현한다.

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

검증 규칙:

- 공지는 pinned 여러 개를 허용한다.
- 공지 목록은 pinned 우선, 같은 그룹 안에서는 작성일 순으로 정렬한다.
- 배치도 슬롯 생성은 프론트가 전달한 구역별 칸 수 정보를 기준으로 한다.
- 이미 부스가 배정된 슬롯에는 다른 부스를 바로 배정할 수 없다.
- 슬롯 배정을 바꾸려면 기존 배정을 먼저 취소한다.
- 승인된 부스 자체를 삭제하는 API는 제공하지 않는다.

## Priority 11: Booth Manager APIs

부스 관리자 권한이 필요한 API를 구현한다. `BOOTH_MANAGER` role을 유지하되, 담당 부스 소유권도 함께 검증한다.

- `PATCH /api/booths/{boothId}`
- `POST /api/booths/{boothId}/menus`
- `PATCH /api/booths/{boothId}/menus/{menuId}`
- `DELETE /api/booths/{boothId}/menus/{menuId}`
- `POST /api/booths/{boothId}/menus/{menuId}/sold-out`

검증 규칙:

- `BOOTH_MANAGER`는 본인 담당 부스만 수정할 수 있다.
- `FESTIVAL_ADMIN`은 부스 관리자 API를 우회 통과할 수 있다.
- 메뉴 생성/수정은 `NIGHT` 부스에서만 허용한다.
- `FOOD_TRUCK`은 내부적으로 `Booth`지만 부스 관리자 계정이 아닌 축제 관리자 계정이 manager다.

## Priority 12: Waiting APIs and Notifications

웨이팅 기능과 호출 알림을 구현한다.

### General User Only

- `POST /api/booths/{boothId}/waitings`
- `DELETE /api/waitings/{waitingId}`
- `GET /api/waitings`

### Booth Manager

- `GET /api/booths/{boothId}/waitings`
- `POST /api/waitings/{waitingId}/call`
- `PATCH /api/waitings/{waitingId}/status`
- `PATCH /api/booths/{boothId}/waitings/status`

검증 규칙:

- 웨이팅 등록은 `USER`만 가능하다.
- 웨이팅은 `NIGHT` 부스에만 등록할 수 있다.
- 사용자당 최대 3개까지 웨이팅을 등록할 수 있다.
- 부스의 웨이팅이 open 상태일 때만 등록할 수 있다.
- 웨이팅 취소는 본인만 가능하다.
- 호출 시 `callCount`를 증가시킨다.
- 호출 시 서버는 사용자 앱으로 알림을 전송한다.
- 상태 전이는 `WAITING -> CALLED -> SEATED` 흐름을 기본으로 하고, 사용자 취소는 `CANCELLED`로 처리한다.
- 사용자 앱의 웨이팅 정보 갱신은 별도 push state sync가 아니라 새로고침 기반 조회로 처리한다.

## Priority 13: Test Coverage

기능별 단위/통합 테스트를 보강한다.

- `AuthServiceTest`
  - 로그인 ID 기반 일반 사용자 회원가입
  - 부스 신청과 동시에 부스 관리자 계정 생성
  - 축제별 계정 분리
  - 중복 로그인 ID는 같은 축제 안에서만 충돌
  - 비밀번호 hash 저장
  - 로그인 성공/실패
  - JWT claim 검증
- `SecurityRoutePolicyIntegrationTest`
  - permit all endpoint 접근
  - 모든 인증 사용자 endpoint 접근
  - 일반 사용자 전용 endpoint에 `BOOTH_MANAGER` / `FESTIVAL_ADMIN` 접근 시 `403`
  - 부스 관리자 endpoint에 일반 사용자 접근 시 `403`
  - 축제 관리자 endpoint에 일반/부스 관리자 접근 시 `403`
  - 문서화되지 않은 endpoint는 `denyAll`
- `BoothApplicationServiceTest`
  - 신청 생성 시 `BOOTH_MANAGER` 계정 생성
  - 승인 전 신청 삭제 시 신청과 계정 hard delete
  - 승인된 신청 삭제 시 `409 Conflict`
  - 승인 시 `Booth` 생성 및 manager 연결
  - 거절 시 검토 메모 저장
- `BoothServiceTest`
  - `active` 제거 후 목록/상세 조회
  - `FOOD_TRUCK` 타입 조회
  - 담당 부스 수정 권한
- `FavoriteServiceTest`
  - 타입별 5개 제한
  - 생성 시각 정렬
  - hard delete
  - 일반 사용자 외 role 접근 차단
- `MenuServiceTest`
  - 야간 부스 메뉴 생성 성공
  - 주간 부스 메뉴 생성 실패
  - 품절 처리
- `LocationServiceTest`
  - 구역별 슬롯 생성
  - `festival_id + zone_label + index + day` unique 검증
  - 여러 슬롯에 같은 부스 배정
  - 이미 배정된 슬롯 중복 배정 실패
  - 배정 취소
- `FestivalServiceTest`
  - 축제 정보 조회/수정
  - 축제 일자별 주간/야간 운영 시간 관리
  - 공지 등록/수정/삭제/조회
  - pinned 우선 및 작성일 순 정렬
  - 공연 타임라인 등록/수정/삭제/조회
- `WaitingServiceTest`
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

## API Access Policy

### Permit All

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/booth-applications`

### All Authenticated Users

- `GET /api/booths`
- `GET /api/booths/{boothId}`
- `GET /api/booths/{boothId}/menus`
- `GET /api/locations`
- `GET /api/festival`
- `GET /api/festival/notices`
- `GET /api/festival/timelines`
- `GET /api/users/me`
- `PATCH /api/users/me`

### General User Only

- `POST /api/favorites`
- `GET /api/favorites`
- `DELETE /api/favorites/{favoriteId}`
- `POST /api/booths/{boothId}/waitings`
- `DELETE /api/waitings/{waitingId}`
- `GET /api/waitings`

### Booth Manager

- `GET /api/booth-applications/me`
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
- `GET /api/admin/booth-applications`
- `GET /api/admin/booth-applications/{applicationId}`
- `POST /api/admin/booth-applications/{applicationId}/approve`
- `POST /api/admin/booth-applications/{applicationId}/reject`
- `DELETE /api/admin/booth-applications/{applicationId}`

## Scope Notes

- `Booth.active` / `is_active`는 제거한다.
- 축제 운영 중/운영 후 부스 삭제는 지원하지 않는다.
- 승인된 `Booth`는 삭제 대상이 아니다.
- 삭제 가능한 것은 승인 전 `BoothApplication`뿐이다.
- 신청 삭제 시 신청과 함께 생성된 `BOOTH_MANAGER` 계정도 hard delete한다.
- `BOOTH_MANAGER`는 전역 role로 유지한다.
- 부스 관리자 계정은 일반 사용자 계정과 재사용하지 않는다.
- `BoothAdminAssignment`는 현재 schema에 남아 있지만, v1 권한 판정에서는 사용하지 않는다.
- 이미지 업로드 저장소 연동은 v1 도메인/API 구현 이후 별도 계획으로 분리한다.
- `docs/API-ENDPOINTS.md`도 PATCH 이후 권한 구분과 삭제 정책에 맞춰 별도 갱신이 필요하다.
