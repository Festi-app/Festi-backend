# API Endpoints

## 사용자 권한

| 이름 | 값 |
| --- | --- |
| 일반 사용자 | 0 |
| 부스 관리자 | 1 |
| 축제 관리자 | 2 |

## user / auth
    
| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | /api/auth/signup | 회원가입 | 모두 |
| POST | /api/auth/login | 로그인 | 모두 |
| GET | /api/users/me | 본인 정보 조회 | 인증 사용자 |
| PATCH | /api/users/me | 본인 정보 수정 (`email`, `name`, `phone`) | 인증 사용자 |
| DELETE | /api/users/me | 탈퇴 | 인증 사용자 |

## booths
    
| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | /api/booths | 부스 목록 조회 (day, type, category 필터) | 일반 사용자 이상 |
| GET | /api/booths/:boothId  | 부스 상세 정보 조회 | 일반 사용자 이상 |
| POST | /api/booths | 부스 등록 | 축제 관리자 |
| PATCH | /api/booths/:boothId | 부스 정보 수정 | 부스 관리자 |
| DELETE | /api/booths/:boothId | 부스 삭제 | 축제 관리자 |

## menu
    
| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | /api/booths/:boothId/menus | 특정 부스의 메뉴 목록 조회 | 일반 사용자 이상 |
| POST | /api/booths/:boothId/menus | 특정 부스에 새 메뉴 추가 | 부스 관리자 |
| PATCH | /api/booths/:boothId/menus/:menuId | 특정 부스의 특정 메뉴 수정 | 부스 관리자 |
| DELETE | /api/booths/:boothId/menus/:menuId | 특정 부스의 특정 메뉴 삭제 | 부스 관리자 |
| POST | /api/booths/:boothId/menus/:menuId/sold-out | 특정 부스의 특정 메뉴 품절 처리 | 부스 관리자 |

## 부스위치/배치도

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | /api/locations | 배치도 전체 조회 (day, type 필터) | 일반 사용자 이상 |
| POST | /api/locations | 특정 위치에 부스 등록 | 축제 관리자 |
| PATCH | /api/locations/:locationId | 특정 위치의 부스 수정 | 축제 관리자 |
| DELETE | /api/locations/:locationId | 특정 위치의 부스 삭제 | 축제 관리자 |

## 웨이팅 (Waitings)

| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | /api/booths/:boothId/waitings | 특정 부스에 웨이팅 등록 | 일반 사용자 |
| DELETE | /api/waitings/:waitingId | 자신의 웨이팅 취소 | 일반 사용자 |
| GET | /api/waitings | 자신의 전체 웨이팅 조회 | 일반 사용자 |
| GET | /api/booths/:boothId/waitings | 부스 웨이팅 목록 | 부스 관리자 |
| POST | /api/waitings/:waitingId/call | 호출 | 부스 관리자 |
| PATCH | /api/waitings/:waitingId/status | 입장 처리 | 부스 관리자 |
| PATCH | /api/booths/:boothId/waitings/status | 웨이팅 오픈/마감 | 부스 관리자 |

## 축제 정보
    
| method | endpoint | 설명 | 권한 |
| --- | --- | --- | --- |
| GET | /api/festival | 축제 정보(기간 등) 조회 | 일반 사용자 이상 |
| PATCH | /api/festival | 축제 정보 수정 | 축제 관리자 |
| GET | /api/festival/notices | 축제 공지사항 조회 | 일반 사용자 이상 |
| POST | /api/festival/notices | 축제 공지사항 추가 | 축제 관리자 |
