# Frontend API Context

이 문서는 프론트엔드 프로젝트에서 사용하는 에이전트가 Festi 백엔드의 현재 API 구성을 이해하기 위한 컨텍스트 문서다. 기준일은 2026-05-23이며, 현재 코드베이스에서 실제 controller/service가 구현된 API만 다룬다.

## Source Of Truth

- 구현된 엔드포인트 목록: `docs/API-ENDPOINTS.md`
- 상세 실행 문서: `/swagger-ui.html`, `/v3/api-docs`
- 인증/인가 정책: `src/main/java/com/festi/backend/security/SecurityConfig.java`
- 응답 DTO: 각 domain package의 `*DTO.java`

`SecurityConfig`에 권한 정책만 있고 controller/service가 없는 라우트는 아직 호출 대상이 아니다. 프론트엔드 에이전트는 이 문서나 OpenAPI에 없는 endpoint를 임의로 만들지 않는다.

## Runtime Basics

- 기본 local origin: `http://localhost:8080`
- API prefix: `/api`
- JSON 요청은 `Content-Type: application/json`을 사용한다.
- 인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 사용한다.
- CORS 허용 origin은 서버 환경 변수 `FESTI_CORS_ALLOWED_ORIGINS`로 설정된다.
  - local default: `http://localhost:3000`
  - 여러 origin은 쉼표로 구분한다.
- Swagger UI는 인증 없이 접근 가능하다.
  - `GET /swagger-ui.html`
  - `GET /v3/api-docs`

## Auth Model

현재 계정은 축제에 bound된다. 회원가입과 로그인은 서버가 첫 번째 `Festival` row를 사용해 처리하며, 클라이언트가 festival id를 선택해서 보내는 구조는 아직 없다.

### Signup

`POST /api/auth/signup`

```json
{
  "id": "alice123",
  "password": "Password1!",
  "name": "Alice",
  "phone": "01012345678"
}
```

성공 응답은 `201 Created`와 사용자 정보를 반환한다.

```json
{
  "id": "alice123",
  "festivalId": "00000000-0000-0000-0000-000000000000",
  "name": "Alice",
  "phone": "01012345678",
  "role": "USER"
}
```

비밀번호는 8-100자이며 대문자, 소문자, 숫자, 특수문자를 각각 포함해야 한다. 같은 축제 안에서 `id`가 중복되면 `409 CONFLICT`가 반환된다.

### Login

`POST /api/auth/login`

```json
{
  "id": "alice123",
  "password": "Password1!"
}
```

성공 응답:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

`expiresIn`은 초 단위이며 local default는 3600초다. refresh token은 아직 없다. access token 만료 후에는 다시 로그인해야 한다.

### Token Claims

프론트에서 JWT를 직접 해석할 필요는 없지만, 서버 토큰에는 다음 claim이 들어간다.

- `sub`: 사용자 로그인 ID
- `festivalId`: UUID string
- `role`: `USER`, `BOOTH_MANAGER`, `FESTIVAL_ADMIN`

## Roles

| Role | Frontend meaning |
| --- | --- |
| `USER` | 일반 사용자. 즐겨찾기와 웨이팅 등록/조회/취소 가능 |
| `BOOTH_MANAGER` | 부스 관리자. 현재 구현된 전용 API는 아직 없음 |
| `FESTIVAL_ADMIN` | 축제 관리자. 현재 구현된 전용 API는 아직 없음 |

현재 구현된 조회 API는 인증된 모든 role이 접근할 수 있다. `favorites`와 일반 사용자 `waitings` API는 `USER` role만 접근할 수 있다.

## Error Shape

모든 공통 에러 응답은 아래 형태다.

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "details": [
    {
      "field": "name",
      "message": "Name must not be blank."
    }
  ],
  "timestamp": "2026-05-23T00:00:00Z"
}
```

`details`는 없으면 빈 배열이다. validation 에러는 rejected value를 내려주지 않는다.

대표 error code:

| HTTP | code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_INPUT_VALUE` | 잘못된 입력 또는 비즈니스 규칙 위반 |
| 400 | `VALIDATION_FAILED` | DTO/query/path validation 실패 |
| 401 | `AUTHENTICATION_REQUIRED` | 토큰 없음, 만료, malformed token |
| 403 | `ACCESS_DENIED` | role이 맞지 않음 |
| 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 또는 현재 상태와 충돌 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

## Common Types

UUID는 string으로 다룬다. 날짜/시간은 ISO string이다.

Enums:

- `UserRole`: `USER`, `BOOTH_MANAGER`, `FESTIVAL_ADMIN`
- `BoothType`: `DAY`, `NIGHT`, `FOOD_TRUCK`
- `BoothCategory`: `ACTIVITY`, `INFO`, `MARKET`, `EXPERIENCE`, `PROMOTION`, `ALCOHOL`
- `WaitingStatus`: `WAITING`, `CALLED`, `SEATED`, `CANCELLED`

Common DTO snippets:

```ts
type UserResponse = {
  id: string;
  festivalId: string;
  name: string;
  phone: string;
  role: "USER" | "BOOTH_MANAGER" | "FESTIVAL_ADMIN";
};

type BoothSummary = {
  id: string;
  name: string;
  category: BoothCategory;
  type: BoothType;
  imageUrl: string | null;
  isWaitingOpen: boolean;
};

type BoothDetail = BoothSummary & {
  description: string | null;
  operatingHours: string | null;
};
```

## Implemented Endpoints

### User Profile

`GET /api/users/me`

- Auth: any authenticated user
- Response: `UserResponse`

`PATCH /api/users/me`

- Auth: any authenticated user
- Body fields are optional, but at least one field must be present.

```json
{
  "name": "New Name",
  "phone": "01098765432"
}
```

- Response: `UserResponse`
- `email` is not part of the current model.

### Booths

`GET /api/booths`

- Auth: any authenticated user
- Query:
  - `day`: optional ISO date, for example `2026-05-22`
  - `type`: optional `DAY | NIGHT | FOOD_TRUCK`
  - `category`: optional `ACTIVITY | INFO | MARKET | EXPERIENCE | PROMOTION | ALCOHOL`
- Response: `BoothSummary[]`

`GET /api/booths/{boothId}`

- Auth: any authenticated user
- Response: `BoothDetail`

### Menus

`GET /api/booths/{boothId}/menus`

- Auth: any authenticated user
- Response:

```ts
type MenuResponse = {
  id: string;
  name: string;
  price: number;
  description: string | null;
  imageUrl: string | null;
  isSoldOut: boolean;
  sortOrder: number;
};
```

### Locations

`GET /api/locations`

- Auth: any authenticated user
- Required query:
  - `day`: ISO date
  - `type`: `DAY | NIGHT | FOOD_TRUCK`
- Response:

```ts
type LocationResponse = {
  id: number;
  type: BoothType;
  index: number | null;
  festivalDay: {
    id: string;
    day: string;
  };
  zoneLabel: string;
  boothSummary: BoothSummary | null;
};
```

`boothSummary` can be `null` for an unassigned location slot. The frontend should render empty slots explicitly instead of filtering them out by default.

### Festival

`GET /api/festival`

- Auth: any authenticated user
- Response:

```ts
type FestivalResponse = {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  description: string | null;
};
```

`GET /api/festival/notices`

- Auth: any authenticated user
- Sort: pinned first, then newest first within each group
- Response:

```ts
type NoticeResponse = {
  id: string;
  title: string;
  content: string;
  pinned: boolean;
  createdAt: string;
};
```

`GET /api/festival/timelines`

- Auth: any authenticated user
- Sort: festival day ascending, start time ascending
- Response:

```ts
type TimelineResponse = {
  id: string;
  festivalDay: {
    id: string;
    day: string;
  };
  title: string;
  artist: string;
  startTime: string;
  endTime: string;
};
```

### Favorites

`GET /api/favorites`

- Auth: `USER`
- Response:

```ts
type FavoriteResponse = {
  id: string;
  boothSummary: BoothSummary;
  createdAt: string;
};
```

`POST /api/favorites`

- Auth: `USER`
- Body:

```json
{
  "boothId": "00000000-0000-0000-0000-000000000000"
}
```

- Response: `FavoriteResponse`
- Duplicate favorite returns `409 CONFLICT`.
- The backend enforces max 5 favorites per `BoothType`.

`DELETE /api/favorites/{favoriteId}`

- Auth: `USER`
- Response: `204 No Content`
- Deleting another user's favorite is hidden as `404 RESOURCE_NOT_FOUND`.

### Waitings

`POST /api/booths/{boothId}/waitings`

- Auth: `USER`
- Body:

```json
{
  "partySize": 2
}
```

- Response:

```ts
type WaitingResponse = {
  id: string;
  boothSummary: BoothSummary;
  partySize: number;
  status: "WAITING" | "CALLED" | "SEATED" | "CANCELLED";
  callCount: number;
  registeredAt: string;
};
```

Backend rules:

- Only `NIGHT` booths can accept waiting registration.
- The booth must have `isWaitingOpen = true`.
- A user can have up to 3 active waitings. Active statuses are `WAITING` and `CALLED`.

`GET /api/waitings`

- Auth: `USER`
- Response: `WaitingResponse[]`
- Sort: newest registered first

`DELETE /api/waitings/{waitingId}`

- Auth: `USER`
- Response: `204 No Content`
- Only the owner can cancel.
- Only `WAITING` and `CALLED` statuses can be cancelled.

## Frontend Agent Rules

- Use `docs/API-ENDPOINTS.md` or `/v3/api-docs` as the endpoint boundary.
- Do not add frontend calls to admin, booth manager, booth application, menu mutation, location mutation, or waiting call/status APIs until they appear in the implemented API docs.
- Treat `401` as a login/session recovery path.
- Treat `403` as a role mismatch path.
- Treat `404` on owner-scoped resources as "not visible or not found"; do not reveal ownership assumptions in UI copy.
- Use uppercase enum strings exactly as returned by the backend.
- Preserve unknown nullable fields in UI types; `imageUrl`, `description`, `operatingHours`, `boothSummary`, and `festival.description` may be null.
- For local development, set the frontend dev server origin to a value allowed by `FESTI_CORS_ALLOWED_ORIGINS`.
