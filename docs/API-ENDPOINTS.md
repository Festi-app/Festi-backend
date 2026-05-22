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

## booths

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/booths` | 부스 목록 조회. 선택 query: `day`, `type`, `category` | 인증 사용자 |
| GET | `/api/booths/{boothId}` | 부스 상세 정보 조회 | 인증 사용자 |
| POST | `/api/booths/{boothId}/waitings` | 특정 부스에 웨이팅 등록 | `USER` |

## menu

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/booths/{boothId}/menus` | 특정 부스의 메뉴 목록 조회 | 인증 사용자 |

## 부스 위치 / 배치도

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/locations` | 배치도 조회. 필수 query: `day`, `type` | 인증 사용자 |

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

## 축제 정보

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/festival` | 축제 기본 정보 조회 | 인증 사용자 |
| GET | `/api/festival/notices` | 축제 공지사항 목록 조회 | 인증 사용자 |
| GET | `/api/festival/timelines` | 축제 공연 타임라인 조회 | 인증 사용자 |
