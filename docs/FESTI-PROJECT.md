# Festi

## 한 줄 소개

대학교 축제 통합 플랫폼 (웹 및 웹앱)

## 기획 배경

학생 축제의 비효율성 인식:
- 학생: 축제 정보가 분산되어 있어 찾기 어려움, 어느 위치에 어느 부스가 있는지 알아보기 힘듬, 푸드트럭 등의 정보가 찾기 어려움.
- 부스 관리자: 부스 운영 정보를 기존처럼 sns에 공지하기엔 정보가 분산됨, 웨이팅 등이 필요한 환경에서 표준이 되는 플랫폼 부재.
- 학생회(축제 관리자): 각 부스별 정보를 유동적으로 관리 및 운영할 수 있는 플랫폼 부재

→ 축제 특화 캐치테이블 서비스가 필요!
학생회) 축제 시즌마다 축제 정보를 유동적으로 관리하고 운영할 수 있는 도구 제공
학생) 분산되어있는 축제 정보를 하나의 앱에서 편리하게 조회 가능, 부스별 웨이팅 기능을 통해 더 편리하게 대기 및 이용 가능

## 주요 기능

- 축제 부스 배치도
    - 주간 / 야간 시간대에 따라 달라지는 부스, 푸드트럭 정보 확인 가능 (날짜별 관리)
    - 일차별로 달라지는 부스 정보 확인 가능
    - 관리자) 일차 및 시간대에 따라 배치도를 기준으로 부스 배정
- 부스 정보
    - 각 부스가 무엇을 하는지, 무엇을 파는지 사용자들이 편리하게 찾아볼 수 있음
- 부스 웨이팅 기능
    - 주점 같은 부스의 경우, 캐치테이블과 같은 웨이팅 시스템을 통해 방문자가 부스에 방문하기 전에 미리 대기를 걸어둘 수 있다

## 세부 기능

### 전체

- 축제 부스 배치도
    - 주간/야간 및 일차별 부스 배치도 및 부스 세부 정보 조회,
    - 푸드트럭 위치 확인 및 정보 조회
- 축제 공지사항 조회
- 일차별 무대 시간표 조회

### 사용자

- 부스 탐색 & 웨이팅
    - 부스 검색
        - 이름/동아리 등으로 부스 검색하기
        - 배치도에서 부스 위치를 눌러 세부 정보 조회하기
    - 부스 정보 확인
    - 부스 웨이팅 등록
    - 웨이팅 알림 (3팀전)

### 부스 관리자

- 부스 관리 대시보드
    - 부스 수정
    - 부스 설명 및 메뉴 등록
    - 부스 예약 관리
      - 대기중인 예약자 명단 조회
      - 웨이팅 호출(승인) / 거절
      - 웨이팅 상태 변경 (입장 처리)
      - 부스 웨이팅 오픈 / 마감

### 축제 관리자

- 축제 관리 대시보드
    - 부스 등록 / 삭제
    - 배치도의 특정 위치에 부스 배정
    - 축제 정보 수정
      - 축제 기간
    - 축제 공지사항 관리
      - 공지사항 작성
      - 공지사항 수정
      - 공지사항 삭제
    - 공연 시간표 관리
      - 일차별 무대 시간표 관리
      - 무대 일정표 조회

## DB ERD

```sql
CREATE TABLE "users" (
	"id"	UUID		NULL,
	"email"	VARCHAR(255)		NOT NULL,
	"password_hash"	VARCHAR(255)		NOT NULL,
	"name"	VARCHAR(100)		NOT NULL,
	"phone"	VARCHAR(20)		NULL,
	"role"	user_role	DEFAULT 'user'	NOT NULL,
	"created_at"	TIMESTAMPTZ		NOT NULL,
	"updated_at"	TIMESTAMPTZ		NOT NULL
);

CREATE TABLE "booth_admin_assignments" (
	"id"	UUID		NOT NULL,
	"booth_id"	UUID		NOT NULL,
	"user_id"	UUID		NOT NULL,
	"granted_by"	UUID		NOT NULL,
	"created_at"	TIMESTAMPTZ		NOT NULL
);

COMMENT ON COLUMN "booth_admin_assignments"."granted_by" IS '권한을';

CREATE TABLE "booth_locations" (
	"id"	SMALLINT		NOT NULL,
	"booth_id"	UUID		NULL,
	"index"	SMALLINT		NULL,
	"day"	DATE		NOT NULL,
	"zone_label"	VARCHAR(100)		NULL,
	"updated_at"	TIMESTAMPTZ		NOT NULL,
	"created_at"	TIMESTAMPTZ		NULL
);

COMMENT ON COLUMN "booth_locations"."day" IS '운영';

CREATE TABLE "waitings" (
	"id"	UUID		NULL,
	"booth_id"	UUID		NOT NULL,
	"user_id"	UUID		NOT NULL,
	"party_size"	SMALLINT		NOT NULL,
	"status"	waiting_status	DEFAULT 'waiting'	NOT NULL,
	"call_count"	SMALLINT		NOT NULL,
	"registered_at"	TIMESTAMPTZ		NOT NULL,
	"updated_at"	TIMESTAMPTZ		NOT NULL
);

COMMENT ON COLUMN "waitings"."party_size" IS '일행';

COMMENT ON COLUMN "waitings"."status" IS 'waiting:';

COMMENT ON COLUMN "waitings"."call_count" IS '관리자';

COMMENT ON COLUMN "waitings"."registered_at" IS '웨이팅';

CREATE TABLE "booths" (
	"id"	UUID		NULL,
	"manager_id"	UUID		NULL,
	"name"	VARCHAR(100)		NOT NULL,
	"category"	booth_category	DEFAULT 'other'	NOT NULL,
	"description"	TEXT		NULL,
	"operating_hours"	VARCHAR(100)		NULL,
	"image_url"	VARCHAR(500)		NULL,
	"is_active"	BOOLEAN		NOT NULL,
	"created_at"	TIMESTAMPTZ		NOT NULL,
	"updated_at"	TIMESTAMPTZ		NOT NULL
);

COMMENT ON COLUMN "booths"."is_active" IS '부스';

CREATE TABLE "menu_items" (
	"id"	UUID		NULL,
	"booth_id"	UUID		NOT NULL,
	"name"	VARCHAR(100)		NOT NULL,
	"price"	INTEGER		NOT NULL,
	"description"	TEXT		NULL,
	"image_url"	VARCHAR(500)		NULL,
	"is_sold_out"	BOOLEAN		NOT NULL,
	"sort_order"	SMALLINT		NOT NULL,
	"created_at"	TIMESTAMPTZ		NOT NULL,
	"updated_at"	TIMESTAMPTZ		NOT NULL
);

COMMENT ON COLUMN "menu_items"."is_sold_out" IS '품절';

COMMENT ON COLUMN "menu_items"."sort_order" IS '메뉴';

ALTER TABLE "users" ADD CONSTRAINT "PK_USERS" PRIMARY KEY (
	"id"
);

ALTER TABLE "booth_admin_assignments" ADD CONSTRAINT "PK_BOOTH_ADMIN_ASSIGNMENTS" PRIMARY KEY (
	"id"
);

ALTER TABLE "booth_locations" ADD CONSTRAINT "PK_BOOTH_LOCATIONS" PRIMARY KEY (
	"id"
);

ALTER TABLE "waitings" ADD CONSTRAINT "PK_WAITINGS" PRIMARY KEY (
	"id"
);

ALTER TABLE "booths" ADD CONSTRAINT "PK_BOOTHS" PRIMARY KEY (
	"id"
);

ALTER TABLE "menu_items" ADD CONSTRAINT "PK_MENU_ITEMS" PRIMARY KEY (
	"id"
);
```

### 부스 (booths)

주간/야간 운영을 하나의 테이블에서 `type` 컬럼으로 구분

- `id` — 부스 고유 id. (UUID, PK)
- `name` — 부스 이름
- `category` — 부스가 해당하는 카테고리 (food / beverage / snack / game / exhibition / other)
- `type` — 부스가 주간에 운영되는지, 또는 야간에 운영되는지 (day / night)
- `description` — 주간 활동 설명 (주간 부스용)
- `operating_hours` — 운영 시간 문자열
- `image_url` — 대표 이미지
- `is_active` — 노출 여부
- `created_by` — 등록된 부스 관리자 ID (FK → users)
- `created_at`, `updated_at`

### 메뉴 (menu_items)

야간 부스에서만 등록할 수 있다.

- `id` — 메뉴 고유 id. (UUID, PK)
- `booth_id` — 이 메뉴를 판매하는 부스 id. (FK → booths)
- `name` — 메뉴명
- `price` — 가격 (원)
- `description` — 메뉴 설명
- `image_url` — 메뉴 사진
- `is_sold_out` — 품절 여부
- `sort_order` — 노출 순서

### 웨이팅 (waitings)

야간 부스 전용. 현장 줄서기 (캐치테이블 같은 기능)

- `id` — 웨이팅 고유 id. (UUID, PK)
- `booth_id` — 이 웨이팅을 등록한 부스 id. (FK → booths, waitings N:1 booths)
- `user_id` — 웨이팅을 등록한 사용자 id. (FK → users, waitings N:1 users)
- `party_size` — 인원수
- `registered_at` — 웨이팅 등록 시간
- `status` — 상태 (waiting / called / seated / cancelled)
- `call_count` — 호출 횟수 (알림 발송 추적용)
- `updated_at` — 마지막 수정 시간

### 부스 위치 (booth_locations)

- `id` — 부스 위치 고유 id. (UUID, PK)
- `type` — 부스 유형 (주간/야간)
- `booth_id` — 이 위치에 배치된 부스 id. (FK → booths, 1:1)
- `index` — 배치도 상의 번호
- `day` — 운영 날짜 정보
- `zone_label` — 구역 명칭 (예: "A구역 — 공학관 앞")

### 사용자 (users)

- `id` — 사용자 고유 id. (UUID, PK)
- `email` — 이메일 (unique)
- `password_hash` — 비밀번호 해시
- `name` — 이름
- `phone` — 전화번호
- `role` — 사용자 역할 (user_role, enum: 'user', 'booth_manager', 'festival_admin')
- `created_at`, `updated_at`

### 부스 관리자 등록

- `id` — 부스 관리자 등록 고유 id. (UUID, PK)
- `booth_id` — 부스 id. (FK → booths, 1:1)
- `user_id` — 사용자 id. (FK → users, 1:1)
- `granted_by` - 권한을 부여한 사용자(festival_admin) id. (FK -> users, 1:1)
- `created_at`

## API

[API 문서](./API-ENDPOINTS.md)에 기재.

# 참고자료

## 배치도

- 주간부스
    
  ![스크린샷 2026-05-14 14.41.39.png](attachment:52235f0e-4e25-4bf5-9d16-c3e90fad33df:스크린샷_2026-05-14_14.41.39.png)
    
- 푸드트럭
    
  ![image.png](attachment:2db15591-adcc-45d0-a794-3d2a3658696d:e790b40f-c1e5-44b9-ab13-780769b3d95d.png)
    

- 야간부스
    
  ![스크린샷 2026-05-14 14.41.20.png](attachment:125f7092-6d56-46c8-8d97-252b4e7fa556:스크린샷_2026-05-14_14.41.20.png)
