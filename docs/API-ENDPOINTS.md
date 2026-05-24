# API Endpoints

이 문서는 현재 코드베이스에서 실제로 제공되는 엔드포인트만 기록한다. `SecurityConfig`에 권한 정책만 선언되어 있고 controller/service가 아직 없는 구현 예정 엔드포인트는 포함하지 않는다.

## 사용자 권한

| 이름 | 값 |
| --- | --- |
| 일반 사용자 | `USER` |
| 부스 관리자 | `BOOTH_MANAGER` |
| 축제 관리자 | `FESTIVAL_ADMIN` |

## 개발/문서

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/swagger-ui.html` | Swagger UI 진입점 | 모두 |
| GET | `/swagger-ui/**` | Swagger UI 정적 리소스 | 모두 |
| GET | `/v3/api-docs` | OpenAPI JSON | 모두 |
| GET | `/v3/api-docs/**` | OpenAPI 문서 리소스 | 모두 |

## user / auth

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입. `id`, `password`, `name`, `phone`을 받는다. | 모두 |
| POST | `/api/auth/login` | 로그인. JWT access token을 반환한다. | 모두 |
| GET | `/api/users/me` | 본인 정보 조회 | 인증 사용자 |
| PATCH | `/api/users/me` | 본인 정보 수정. 현재 수정 가능 필드는 `name`, `phone`이다. | 인증 사용자 |

## booth applications

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/booth-applications` | 부스 관리자 계정과 부스 신청을 함께 생성한다. JWT는 반환하지 않는다. | 모두 |
| GET | `/api/booth-applications/me` | 현재 부스 관리자 계정의 신청 상태 조회 | `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` |
| GET | `/api/admin/booth-applications` | 부스 신청 목록 조회 | `FESTIVAL_ADMIN` |
| GET | `/api/admin/booth-applications/{applicationId}` | 부스 신청 상세 조회 | `FESTIVAL_ADMIN` |
| POST | `/api/admin/booth-applications/{applicationId}/approve` | 신청 승인 및 부스 생성 | `FESTIVAL_ADMIN` |
| POST | `/api/admin/booth-applications/{applicationId}/reject` | 신청 거절. 선택 필드 `reviewMemo`를 받을 수 있다. | `FESTIVAL_ADMIN` |
| DELETE | `/api/admin/booth-applications/{applicationId}` | 승인 전 또는 거절된 신청과 생성된 부스 관리자 계정을 삭제한다. | `FESTIVAL_ADMIN` |

## booths

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/booths` | 부스 목록 조회. 선택 query: `day`, `type`, `category` | 인증 사용자 |
| GET | `/api/booths/{boothId}` | 부스 상세 정보 조회 | 인증 사용자 |
| POST | `/api/booths/{boothId}/waitings` | 특정 부스에 웨이팅 등록 | `USER` |
| GET | `/api/booths/{boothId}/waitings` | 담당 부스의 active 웨이팅 목록 조회 | `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` |
| PATCH | `/api/booths/{boothId}/waitings/status` | 야간 부스의 웨이팅 접수 오픈/마감. `open`을 받는다. | `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` |

## menu

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/booths/{boothId}/menus` | 특정 부스의 메뉴 목록 조회 | 인증 사용자 |

## 부스 위치 / 배치도

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/locations` | 배치도 조회. 필수 query: `day`, `type` | 인증 사용자 |
| POST | `/api/locations/slots` | 구역별 슬롯 생성. `festivalDayId`, `type`, `zones[]`를 받는다. | `FESTIVAL_ADMIN` |
| POST | `/api/locations/{locationId}/assignment` | 빈 슬롯에 부스 배정. `boothId`를 받는다. | `FESTIVAL_ADMIN` |
| DELETE | `/api/locations/{locationId}/assignment` | 슬롯의 부스 배정 취소 | `FESTIVAL_ADMIN` |

## favorites

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/favorites` | 본인 즐겨찾기 목록 조회 | `USER` |
| POST | `/api/favorites` | 부스 즐겨찾기 추가. `boothId`를 받는다. | `USER` |
| DELETE | `/api/favorites/{favoriteId}` | 본인 즐겨찾기 삭제 | `USER` |

## waitings

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/waitings` | 본인 웨이팅 목록 조회 | `USER` |
| DELETE | `/api/waitings/{waitingId}` | 본인 웨이팅 취소 | `USER` |
| POST | `/api/waitings/{waitingId}/call` | active 웨이팅 호출 또는 재호출. 호출 횟수를 증가시킨다. | `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` |
| PATCH | `/api/waitings/{waitingId}/status` | 호출된 웨이팅을 착석 처리. `status: "SEATED"`를 받는다. | `BOOTH_MANAGER` 또는 `FESTIVAL_ADMIN` |

## push subscriptions

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/push-subscriptions` | Web Push 구독 등록 또는 갱신. `endpoint`, `keys.p256dh`, `keys.auth`를 받는다. | `USER` |
| DELETE | `/api/push-subscriptions/{subscriptionId}` | 본인 Web Push 구독 해제 | `USER` |

## 축제 정보

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/festival` | 축제 기본 정보 조회 | 인증 사용자 |
| PATCH | `/api/festival` | 축제 기본 정보 수정. `name`, `startDate`, `endDate`, `description`을 받는다. | `FESTIVAL_ADMIN` |
| POST | `/api/festival/days` | 축제 운영 일자와 주간/야간 운영 시간 생성 | `FESTIVAL_ADMIN` |
| PATCH | `/api/festival/days/{festivalDayId}` | 축제 운영 일자와 주간/야간 운영 시간 수정 | `FESTIVAL_ADMIN` |
| DELETE | `/api/festival/days/{festivalDayId}` | 축제 운영 일자 삭제 | `FESTIVAL_ADMIN` |
| GET | `/api/festival/notices` | 축제 공지사항 목록 조회 | 인증 사용자 |
| POST | `/api/festival/notices` | 축제 공지사항 생성 | `FESTIVAL_ADMIN` |
| PATCH | `/api/festival/notices/{noticeId}` | 축제 공지사항 수정 | `FESTIVAL_ADMIN` |
| DELETE | `/api/festival/notices/{noticeId}` | 축제 공지사항 삭제 | `FESTIVAL_ADMIN` |
| GET | `/api/festival/timelines` | 축제 공연 타임라인 조회 | 인증 사용자 |
| POST | `/api/festival/timelines` | 축제 공연 타임라인 생성 | `FESTIVAL_ADMIN` |
| PATCH | `/api/festival/timelines/{timelineId}` | 축제 공연 타임라인 수정 | `FESTIVAL_ADMIN` |
| DELETE | `/api/festival/timelines/{timelineId}` | 축제 공연 타임라인 삭제 | `FESTIVAL_ADMIN` |
