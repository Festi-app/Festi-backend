# Festi 공용 DB 더미 데이터 계획

## 개요

| 항목 | 수량 |
|------|------|
| 축제 | 1개 (3일: 2026-06-20 ~ 2026-06-22) |
| 총관리자 | 1명 (`admin`) |
| 일반 사용자 | 10명 (`user01` ~ `user10`) |
| 주간 부스 관리자 | 20명 (`day01` ~ `day20`) |
| 야간 부스 관리자 | 15명 (`night01` ~ `night15`) |
| 푸드트럭 관리자 | 6명 (`ft01` ~ `ft06`) |
| 주간 부스 (DAY) | 20개 |
| 야간 부스 (NIGHT) | 15개 (바로입장 5 / 웨이팅가능 5 / 웨이팅중 5) |
| 푸드트럭 (FOOD_TRUCK) | 6개 |
| 메뉴 | 63개 (야간 45 + 푸드트럭 18) |
| 공연 스케줄 | 12개 (일별 4개) |
| 웨이팅 | 18개 (야간 11~15번 부스) |
| 즐겨찾기 | 15개 (user01~05 각 3개) |

---

## 공통 정보

**모든 계정 비밀번호**: `1234`  
**bcrypt hash**: `$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO`

---

## 고정 UUID 참조표

| 항목 | UUID 패턴 |
|------|-----------|
| 페스티벌 | `00000000-0000-0000-0000-000000000001` |
| 축제 1일차 | `00000000-0000-0000-0001-000000000001` |
| 축제 2일차 | `00000000-0000-0000-0001-000000000002` |
| 축제 3일차 | `00000000-0000-0000-0001-000000000003` |
| 주간 부스 N번 | `00000000-0000-0000-0002-0000000000NN` (N=01~20) |
| 야간 부스 N번 | `00000000-0000-0000-0003-0000000000NN` (N=01~15) |
| 푸드트럭 N번 | `00000000-0000-0000-0004-0000000000NN` (N=01~06) |

---

## 야간 부스 웨이팅 상태 구분

| 부스 번호 | 부스명 | is_waiting_open | 웨이팅 상태 |
|-----------|--------|-----------------|-------------|
| night01~05 | 타코야키 포차 외 4개 | `false` | 바로 입장 가능 |
| night06~10 | 칵테일 바 외 4개 | `true` | 대기 없음 (웨이팅 걸기 가능) |
| night11~15 | 버스킹 존 외 4개 | `true` | WAITING 팀 다수 (웨이팅 중) |

---

## 실행 순서

```
Step 1  → 페스티벌
Step 2  → 페스티벌 일정 (festival_days)
Step 3  → 사용자 (총관리자 + 일반 + 부스 관리자)
Step 4  → 주간 부스 (20개)
Step 5  → 야간 부스 (15개)
Step 6  → 푸드트럭 (6개)
Step 7  → 메뉴 (야간 45개 + 푸드트럭 18개)
Step 8  → 공연 스케줄 (12개)
Step 9  → 웨이팅 (18개)
Step 10 → 즐겨찾기 (15개)
```

---

## Step 1: 페스티벌

```sql
INSERT INTO festival (id, name, start_date, end_date, description, created_at, updated_at) VALUES (
  '00000000-0000-0000-0000-000000000001',
  '봄빛 대학 축제 2026',
  '2026-06-20',
  '2026-06-22',
  '매년 6월 열리는 대학 축제입니다. 다양한 부스와 공연을 즐겨보세요!',
  NOW(), NOW()
);
```

---

## Step 2: 페스티벌 일정

```sql
INSERT INTO festival_days (id, festival_id, day, day_start, day_end, night_start, night_end, created_at, updated_at) VALUES
  ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001', '2026-06-20', '10:00', '18:00', '18:00', '23:00', NOW(), NOW()),
  ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', '2026-06-21', '10:00', '18:00', '18:00', '23:30', NOW(), NOW()),
  ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000001', '2026-06-22', '10:00', '18:00', '18:00', '23:00', NOW(), NOW());
```

---

## Step 3: 사용자

### 총관리자 (1명)

```sql
INSERT INTO users (festival_id, id, name, phone, password_hash, role, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', 'admin', '총관리자', '010-0000-0000',
   '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'FESTIVAL_ADMIN', NOW(), NOW());
```

### 일반 사용자 (10명)

```sql
INSERT INTO users (festival_id, id, name, phone, password_hash, role, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', 'user01', '김민수', '010-1001-0001', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user02', '이지은', '010-1001-0002', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user03', '박준호', '010-1001-0003', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user04', '최유나', '010-1001-0004', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user05', '정도현', '010-1001-0005', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user06', '강서연', '010-1001-0006', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user07', '윤재원', '010-1001-0007', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user08', '임소희', '010-1001-0008', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user09', '한동현', '010-1001-0009', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'user10', '오미래', '010-1001-0010', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'USER', NOW(), NOW());
```

### 부스 관리자 - 주간 (20명)

```sql
INSERT INTO users (festival_id, id, name, phone, password_hash, role, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', 'day01', '김현우', '010-2001-0001', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day02', '이수아', '010-2001-0002', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day03', '박지민', '010-2001-0003', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day04', '최재훈', '010-2001-0004', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day05', '정예은', '010-2001-0005', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day06', '강민준', '010-2001-0006', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day07', '윤하은', '010-2001-0007', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day08', '임세준', '010-2001-0008', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day09', '한지수', '010-2001-0009', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day10', '오준혁', '010-2001-0010', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day11', '서아린', '010-2001-0011', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day12', '신동혁', '010-2001-0012', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day13', '권나연', '010-2001-0013', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day14', '배민서', '010-2001-0014', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day15', '조현석', '010-2001-0015', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day16', '유지원', '010-2001-0016', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day17', '문채원', '010-2001-0017', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day18', '노태준', '010-2001-0018', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day19', '하지혜', '010-2001-0019', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'day20', '마준서', '010-2001-0020', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW());
```

### 부스 관리자 - 야간 (15명)

```sql
INSERT INTO users (festival_id, id, name, phone, password_hash, role, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', 'night01', '김태양', '010-3001-0001', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night02', '이소율', '010-3001-0002', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night03', '박정민', '010-3001-0003', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night04', '최가은', '010-3001-0004', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night05', '정시훈', '010-3001-0005', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night06', '강은서', '010-3001-0006', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night07', '윤민혁', '010-3001-0007', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night08', '임지안', '010-3001-0008', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night09', '한준영', '010-3001-0009', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night10', '오서원', '010-3001-0010', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night11', '서지후', '010-3001-0011', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night12', '신유진', '010-3001-0012', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night13', '권민준', '010-3001-0013', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night14', '배나연', '010-3001-0014', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'night15', '조하린', '010-3001-0015', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW());
```

### 부스 관리자 - 푸드트럭 (6명)

```sql
INSERT INTO users (festival_id, id, name, phone, password_hash, role, created_at, updated_at) VALUES
  ('00000000-0000-0000-0000-000000000001', 'ft01', '이재원', '010-4001-0001', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'ft02', '김수현', '010-4001-0002', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'ft03', '박도연', '010-4001-0003', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'ft04', '최민재', '010-4001-0004', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'ft05', '정하윤', '010-4001-0005', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW()),
  ('00000000-0000-0000-0000-000000000001', 'ft06', '강지수', '010-4001-0006', '$2b$10$c7VE395WlOrbuE6.jLtkhuhA9GIMbEO2.ZK75UDAAjOTyA2FzeAoO', 'BOOTH_MANAGER', NOW(), NOW());
```

---

## Step 4: 주간 부스 (20개)

이미지: `https://picsum.photos/seed/day-{N}/600/400`

```sql
INSERT INTO booths (id, festival_id, manager_id, name, category, type, description, operating_hours, image_url, is_waiting_open, created_at, updated_at) VALUES
  ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000001', 'day01', '컴퓨터공학과', 'ACTIVITY',    'DAY', '코딩 체험과 미니 해커톤을 즐겨보세요.',   '10:00 ~ 18:00', 'https://picsum.photos/seed/day-01/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000001', 'day02', '경영학과',     'MARKET',      'DAY', '학과 굿즈와 중고 물품을 판매합니다.',     '10:00 ~ 18:00', 'https://picsum.photos/seed/day-02/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000001', 'day03', '디자인학과',   'EXPERIENCE',  'DAY', '포트폴리오 전시와 굿즈 판매 부스입니다.', '10:00 ~ 18:00', 'https://picsum.photos/seed/day-03/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000001', 'day04', '기계공학과',   'ACTIVITY',    'DAY', '로봇 시연과 체험 프로그램을 진행합니다.', '10:00 ~ 18:00', 'https://picsum.photos/seed/day-04/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0000-000000000001', 'day05', '화학공학과',   'ACTIVITY',    'DAY', '과학 실험 체험 부스입니다.',              '10:00 ~ 18:00', 'https://picsum.photos/seed/day-05/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0000-000000000001', 'day06', '심리학과',     'INFO',        'DAY', 'MBTI 검사와 심리 상담을 해드립니다.',     '10:00 ~ 18:00', 'https://picsum.photos/seed/day-06/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000007', '00000000-0000-0000-0000-000000000001', 'day07', '영어영문학과', 'EXPERIENCE',  'DAY', '영어 게임과 외국 문화 체험 부스입니다.',  '10:00 ~ 18:00', 'https://picsum.photos/seed/day-07/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000008', '00000000-0000-0000-0000-000000000001', 'day08', '수학과',       'INFO',        'DAY', '수학 퀴즈와 퍼즐 체험 부스입니다.',       '10:00 ~ 18:00', 'https://picsum.photos/seed/day-08/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000009', '00000000-0000-0000-0000-000000000001', 'day09', '물리학과',     'ACTIVITY',    'DAY', '물리 실험 및 VR 체험 부스입니다.',        '10:00 ~ 18:00', 'https://picsum.photos/seed/day-09/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000010', '00000000-0000-0000-0000-000000000001', 'day10', '건축학과',     'MARKET',      'DAY', '건축 모형 전시와 굿즈 판매 부스입니다.',  '10:00 ~ 18:00', 'https://picsum.photos/seed/day-10/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000011', '00000000-0000-0000-0000-000000000001', 'day11', '미술학과',     'EXPERIENCE',  'DAY', '그림 그리기 체험과 작품 전시 부스.',       '10:00 ~ 18:00', 'https://picsum.photos/seed/day-11/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000012', '00000000-0000-0000-0000-000000000001', 'day12', '음악학과',     'EXPERIENCE',  'DAY', '미니 공연과 악기 체험 부스입니다.',        '10:00 ~ 18:00', 'https://picsum.photos/seed/day-12/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000013', '00000000-0000-0000-0000-000000000001', 'day13', '체육학과',     'ACTIVITY',    'DAY', '스포츠 체험과 건강 측정 부스입니다.',      '10:00 ~ 18:00', 'https://picsum.photos/seed/day-13/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000014', '00000000-0000-0000-0000-000000000001', 'day14', '법학과',       'INFO',        'DAY', '법률 상담 및 모의 재판 체험 부스.',        '10:00 ~ 18:00', 'https://picsum.photos/seed/day-14/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000015', '00000000-0000-0000-0000-000000000001', 'day15', '사회학과',     'INFO',        'DAY', '사회 이슈 전시 및 토론 부스입니다.',       '10:00 ~ 18:00', 'https://picsum.photos/seed/day-15/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000016', '00000000-0000-0000-0000-000000000001', 'day16', '국어국문학과', 'EXPERIENCE',  'DAY', '시 낭송과 창작 글쓰기 체험 부스.',         '10:00 ~ 18:00', 'https://picsum.photos/seed/day-16/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000017', '00000000-0000-0000-0000-000000000001', 'day17', '사진동아리',   'EXPERIENCE',  'DAY', '즉석 사진 촬영 및 포토존 운영 부스.',      '10:00 ~ 18:00', 'https://picsum.photos/seed/day-17/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000018', '00000000-0000-0000-0000-000000000001', 'day18', '밴드동아리',   'ACTIVITY',    'DAY', '버스킹 및 밴드 공연 부스입니다.',          '10:00 ~ 18:00', 'https://picsum.photos/seed/day-18/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000019', '00000000-0000-0000-0000-000000000001', 'day19', '댄스동아리',   'ACTIVITY',    'DAY', '댄스 공연과 댄스 배틀 체험 부스.',         '10:00 ~ 18:00', 'https://picsum.photos/seed/day-19/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0002-000000000020', '00000000-0000-0000-0000-000000000001', 'day20', '봉사동아리',   'PROMOTION',   'DAY', '환경 캠페인과 나눔 물품 배포 부스.',       '10:00 ~ 18:00', 'https://picsum.photos/seed/day-20/600/400', false, NOW(), NOW());
```

---

## Step 5: 야간 부스 (15개)

이미지: `https://picsum.photos/seed/night-{N}/600/400`  
웨이팅 상태: `night01~05` = `false` / `night06~10` = `true`(대기 없음) / `night11~15` = `true`(대기 있음)

```sql
INSERT INTO booths (id, festival_id, manager_id, name, category, type, description, operating_hours, image_url, is_waiting_open, created_at, updated_at) VALUES
  -- 바로 입장 가능 (is_waiting_open = false)
  ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000001', 'night01', '타코야키 포차', 'ALCOHOL',    'NIGHT', '바삭한 타코야키와 야키소바를 즐겨보세요.', '18:00 ~ 23:00', 'https://picsum.photos/seed/night-01/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000001', 'night02', '맥주 포차',     'ALCOHOL',    'NIGHT', '시원한 생맥주와 다양한 안주 부스.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-02/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000001', 'night03', '막걸리 포차',   'ALCOHOL',    'NIGHT', '전통 막걸리와 파전을 즐겨보세요.',         '18:00 ~ 23:00', 'https://picsum.photos/seed/night-03/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000001', 'night04', '소주방',        'ALCOHOL',    'NIGHT', '다양한 소주와 간단한 안주 세트.',          '18:00 ~ 23:00', 'https://picsum.photos/seed/night-04/600/400', false, NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0000-000000000001', 'night05', '와인 바',       'ALCOHOL',    'NIGHT', '레드·화이트 와인과 치즈플레이트.',         '18:00 ~ 23:00', 'https://picsum.photos/seed/night-05/600/400', false, NOW(), NOW()),
  -- 웨이팅 걸기 가능 (is_waiting_open = true, 대기 없음)
  ('00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0000-000000000001', 'night06', '칵테일 바',     'ALCOHOL',    'NIGHT', '시그니처 칵테일을 직접 만들어보세요.',     '18:00 ~ 23:00', 'https://picsum.photos/seed/night-06/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0000-000000000001', 'night07', '하이볼 바',     'ALCOHOL',    'NIGHT', '다양한 하이볼과 위스키 칵테일.',           '18:00 ~ 23:00', 'https://picsum.photos/seed/night-07/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0000-000000000001', 'night08', '라멘야',        'EXPERIENCE', 'NIGHT', '진한 육수의 정통 일본식 라멘 부스.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-08/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000009', '00000000-0000-0000-0000-000000000001', 'night09', '닭꼬치 포차',   'ALCOHOL',    'NIGHT', '숯불 닭꼬치와 맥주 조합을 즐기세요.',     '18:00 ~ 23:00', 'https://picsum.photos/seed/night-09/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000010', '00000000-0000-0000-0000-000000000001', 'night10', '이자카야',      'ALCOHOL',    'NIGHT', '일본식 이자카야 메뉴를 즐겨보세요.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-10/600/400', true,  NOW(), NOW()),
  -- 웨이팅 중 (is_waiting_open = true, WAITING 팀 다수)
  ('00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000001', 'night11', '버스킹 존',     'EXPERIENCE', 'NIGHT', '라이브 공연과 음료를 함께 즐기세요.',     '18:00 ~ 23:00', 'https://picsum.photos/seed/night-11/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000001', 'night12', '포토 부스',     'EXPERIENCE', 'NIGHT', '다양한 콘셉트 즉석 사진 촬영 부스.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-12/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'night13', '학생 주점',     'ALCOHOL',    'NIGHT', '학우들이 직접 운영하는 주점 부스.',        '18:00 ~ 23:00', 'https://picsum.photos/seed/night-13/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0000-000000000001', 'night14', '감성 포차',     'ALCOHOL',    'NIGHT', '감성 조명 아래 즐기는 포차 분위기.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-14/600/400', true,  NOW(), NOW()),
  ('00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000001', 'night15', '심야 카페',     'EXPERIENCE', 'NIGHT', '달달한 디저트와 음료를 즐겨보세요.',       '18:00 ~ 23:00', 'https://picsum.photos/seed/night-15/600/400', true,  NOW(), NOW());
```

---

## Step 6: 푸드트럭 (6개)

이미지 없음 (user 요구사항에 명시되지 않음)

```sql
INSERT INTO booths (id, festival_id, manager_id, name, category, type, description, operating_hours, image_url, is_waiting_open, created_at, updated_at) VALUES
  ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000001', 'ft01', '떡볶이트럭',     'MARKET', 'FOOD_TRUCK', '매콤달콤 국물 떡볶이와 순대 세트.',       '11:00 ~ 21:00', NULL, false, NOW(), NOW()),
  ('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000001', 'ft02', '핫도그트럭',     'MARKET', 'FOOD_TRUCK', '바삭한 핫도그와 치즈핫도그 전문.',         '11:00 ~ 21:00', NULL, false, NOW(), NOW()),
  ('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0000-000000000001', 'ft03', '아이스크림트럭', 'MARKET', 'FOOD_TRUCK', '소프트콘부터 샌드위치까지 아이스크림.',   '11:00 ~ 21:00', NULL, false, NOW(), NOW()),
  ('00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0000-000000000001', 'ft04', '타코트럭',       'MARKET', 'FOOD_TRUCK', '멕시코 정통 타코와 부리또 전문 트럭.',     '11:00 ~ 21:00', NULL, false, NOW(), NOW()),
  ('00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0000-000000000001', 'ft05', '피자트럭',       'MARKET', 'FOOD_TRUCK', '화덕에서 구운 정통 이탈리아 피자.',        '11:00 ~ 21:00', NULL, false, NOW(), NOW()),
  ('00000000-0000-0000-0004-000000000006', '00000000-0000-0000-0000-000000000001', 'ft06', '커피트럭',       'MARKET', 'FOOD_TRUCK', '스페셜티 원두로 내린 핸드드립 커피.',      '11:00 ~ 21:00', NULL, false, NOW(), NOW());
```

---

## Step 7: 메뉴

메뉴 이미지: `https://picsum.photos/seed/menu-{booth-name}-{N}/400/300`

### 야간 부스 메뉴 (15개 × 3 = 45개)

```sql
INSERT INTO menu_items (id, booth_id, name, price, description, image_url, is_sold_out, sort_order, created_at, updated_at) VALUES
  -- 타코야키 포차 (night01)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000001', '타코야키 6개',  5000, '문어 듬뿍 타코야키.',        'https://picsum.photos/seed/menu-night01-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000001', '오코노미야끼', 6000, '일본식 부침개 오코노미야끼.', 'https://picsum.photos/seed/menu-night01-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000001', '야키소바',      5500, '볶음 소바 면 요리.',          'https://picsum.photos/seed/menu-night01-3/400/300', false, 3, NOW(), NOW()),
  -- 맥주 포차 (night02)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000002', '생맥주 500cc', 4000, '시원한 생맥주 한 잔.',        'https://picsum.photos/seed/menu-night02-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000002', '하이네켄',      5000, '수입 맥주 하이네켄 병.',       'https://picsum.photos/seed/menu-night02-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000002', '오돌뼈 구이',  7000, '쫄깃한 오돌뼈 안주.',         'https://picsum.photos/seed/menu-night02-3/400/300', false, 3, NOW(), NOW()),
  -- 막걸리 포차 (night03)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000003', '생막걸리',      3000, '시원한 생막걸리 한 사발.',    'https://picsum.photos/seed/menu-night03-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000003', '해물파전',      5000, '바삭한 해물파전.',            'https://picsum.photos/seed/menu-night03-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000003', '두부김치',      6000, '매콤한 두부김치 안주.',       'https://picsum.photos/seed/menu-night03-3/400/300', false, 3, NOW(), NOW()),
  -- 소주방 (night04)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000004', '참이슬',        3500, '정통 소주 참이슬.',           'https://picsum.photos/seed/menu-night04-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000004', '처음처럼',      3500, '부드러운 소주 처음처럼.',     'https://picsum.photos/seed/menu-night04-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000004', '안주 세트',     8000, '소주에 딱 맞는 안주 세트.',   'https://picsum.photos/seed/menu-night04-3/400/300', false, 3, NOW(), NOW()),
  -- 와인 바 (night05)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000005', '레드 와인',     8000, '부드러운 레드 와인 1잔.',     'https://picsum.photos/seed/menu-night05-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000005', '화이트 와인',   8000, '상큼한 화이트 와인 1잔.',     'https://picsum.photos/seed/menu-night05-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000005', '치즈 플레이트', 9000, '와인과 어울리는 치즈 모둠.',  'https://picsum.photos/seed/menu-night05-3/400/300', false, 3, NOW(), NOW()),
  -- 칵테일 바 (night06)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000006', '모히토',        7000, '민트 가득 상큼한 모히토.',    'https://picsum.photos/seed/menu-night06-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000006', '마가리타',      7000, '짭조름한 마가리타 칵테일.',   'https://picsum.photos/seed/menu-night06-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000006', '블루라군',      7000, '아름다운 색감의 블루라군.',   'https://picsum.photos/seed/menu-night06-3/400/300', false, 3, NOW(), NOW()),
  -- 하이볼 바 (night07)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000007', '클래식 하이볼', 6000, '위스키와 탄산수의 하이볼.',   'https://picsum.photos/seed/menu-night07-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000007', '유자 하이볼',   6500, '상큼한 유자향 하이볼.',       'https://picsum.photos/seed/menu-night07-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000007', '위스키 하이볼', 7000, '프리미엄 위스키 하이볼.',     'https://picsum.photos/seed/menu-night07-3/400/300', false, 3, NOW(), NOW()),
  -- 라멘야 (night08)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000008', '쇼유 라멘',     8000, '간장 육수 정통 쇼유 라멘.',   'https://picsum.photos/seed/menu-night08-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000008', '미소 라멘',     8000, '구수한 된장 미소 라멘.',       'https://picsum.photos/seed/menu-night08-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000008', '돈코츠 라멘',   9000, '진한 돼지뼈 육수 라멘.',      'https://picsum.photos/seed/menu-night08-3/400/300', false, 3, NOW(), NOW()),
  -- 닭꼬치 포차 (night09)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000009', '닭꼬치 2개',    3000, '숯불 닭꼬치 2개 세트.',       'https://picsum.photos/seed/menu-night09-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000009', '소금 닭꼬치',   3000, '담백한 소금 양념 닭꼬치.',    'https://picsum.photos/seed/menu-night09-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000009', '쪽파 닭꼬치',   3500, '향긋한 쪽파를 곁들인 닭꼬치.', 'https://picsum.photos/seed/menu-night09-3/400/300', false, 3, NOW(), NOW()),
  -- 이자카야 (night10)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000010', '에다마메',      4000, '짭짤한 일본식 풋콩.',         'https://picsum.photos/seed/menu-night10-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000010', '카라아게',      7000, '바삭한 일본식 닭튀김.',       'https://picsum.photos/seed/menu-night10-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000010', '오야코동',      9000, '부드러운 닭고기 계란덮밥.',   'https://picsum.photos/seed/menu-night10-3/400/300', false, 3, NOW(), NOW()),
  -- 버스킹 존 (night11)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '아메리카노',    3000, '공연과 함께하는 커피.',        'https://picsum.photos/seed/menu-night11-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '레모네이드',    4000, '상큼한 수제 레모네이드.',     'https://picsum.photos/seed/menu-night11-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '수제 맥주',     4500, '라이브와 함께하는 맥주.',     'https://picsum.photos/seed/menu-night11-3/400/300', false, 3, NOW(), NOW()),
  -- 포토 부스 (night12)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '즉석 사진 2매', 5000, '귀여운 즉석 사진 2장.',       'https://picsum.photos/seed/menu-night12-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '스티커 사진',   4000, '예쁜 스티커 사진 1판.',       'https://picsum.photos/seed/menu-night12-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '포토 액자',     8000, '사진 + 미니 액자 패키지.',    'https://picsum.photos/seed/menu-night12-3/400/300', false, 3, NOW(), NOW()),
  -- 학생 주점 (night13)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '소맥 세트',     4000, '소주+맥주 소맥 세트.',         'https://picsum.photos/seed/menu-night13-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '막걸리 세트',   5000, '막걸리+안주 기본 세트.',       'https://picsum.photos/seed/menu-night13-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '안주 모둠',     7000, '다양한 안주 모둠 플레이트.',  'https://picsum.photos/seed/menu-night13-3/400/300', false, 3, NOW(), NOW()),
  -- 감성 포차 (night14)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '달고나 라떼',   5000, '달달한 달고나 라떼.',          'https://picsum.photos/seed/menu-night14-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '감귤 에이드',   4500, '상큼한 제주 감귤 에이드.',    'https://picsum.photos/seed/menu-night14-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '호떡',          2000, '뜨거운 흑설탕 호떡.',         'https://picsum.photos/seed/menu-night14-3/400/300', false, 3, NOW(), NOW()),
  -- 심야 카페 (night15)
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '아이스크림',    3000, '다양한 맛 아이스크림.',       'https://picsum.photos/seed/menu-night15-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '조각 케이크',   5000, '달콤한 수제 케이크 한 조각.', 'https://picsum.photos/seed/menu-night15-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '카페 라떼',     4000, '진한 에스프레소 카페 라떼.',  'https://picsum.photos/seed/menu-night15-3/400/300', false, 3, NOW(), NOW());
```

### 푸드트럭 메뉴 (6개 × 3 = 18개)

```sql
INSERT INTO menu_items (id, booth_id, name, price, description, image_url, is_sold_out, sort_order, created_at, updated_at) VALUES
  -- 떡볶이트럭 (ft01)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000001', '떡볶이',        4000, '매콤달콤 국물 떡볶이.',       'https://picsum.photos/seed/menu-ft01-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000001', '순대',          3000, '쫄깃한 당면 순대.',           'https://picsum.photos/seed/menu-ft01-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000001', '튀김 모듬',     2000, '바삭바삭 튀김 모듬.',         'https://picsum.photos/seed/menu-ft01-3/400/300', false, 3, NOW(), NOW()),
  -- 핫도그트럭 (ft02)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000002', '핫도그',        3000, '바삭한 클래식 핫도그.',       'https://picsum.photos/seed/menu-ft02-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000002', '치즈 핫도그',   4000, '쭉쭉 늘어나는 치즈 핫도그.', 'https://picsum.photos/seed/menu-ft02-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000002', '감자 핫도그',   3500, '아삭한 감자 코팅 핫도그.',   'https://picsum.photos/seed/menu-ft02-3/400/300', false, 3, NOW(), NOW()),
  -- 아이스크림트럭 (ft03)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000003', '소프트 콘',     2000, '부드러운 소프트아이스크림.',  'https://picsum.photos/seed/menu-ft03-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000003', '더블 콘',       3000, '두 가지 맛 더블 콘.',         'https://picsum.photos/seed/menu-ft03-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000003', '아이스크림 샌드', 3500, '쫀득한 아이스크림 샌드.', 'https://picsum.photos/seed/menu-ft03-3/400/300', false, 3, NOW(), NOW()),
  -- 타코트럭 (ft04)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000004', '불고기 타코',   6000, '달콤한 불고기를 넣은 타코.',  'https://picsum.photos/seed/menu-ft04-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000004', '쉬림프 타코',   7000, '탱글탱글 새우 타코.',         'https://picsum.photos/seed/menu-ft04-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000004', '아보카도 타코', 7000, '신선한 아보카도 타코.',       'https://picsum.photos/seed/menu-ft04-3/400/300', false, 3, NOW(), NOW()),
  -- 피자트럭 (ft05)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000005', '치즈 피자',     8000, '쭉쭉 늘어나는 치즈 피자.',   'https://picsum.photos/seed/menu-ft05-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000005', '페퍼로니 피자', 9000, '스파이시 페퍼로니 피자.',    'https://picsum.photos/seed/menu-ft05-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000005', '콤비네이션',    10000,'야채+고기 콤비네이션 피자.', 'https://picsum.photos/seed/menu-ft05-3/400/300', false, 3, NOW(), NOW()),
  -- 커피트럭 (ft06)
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000006', '아메리카노',    2500, '싱글 오리진 핸드드립.',       'https://picsum.photos/seed/menu-ft06-1/400/300', false, 1, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000006', '카페 라떼',     3500, '부드러운 우유 카페 라떼.',    'https://picsum.photos/seed/menu-ft06-2/400/300', false, 2, NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0004-000000000006', '과일 스무디',   4500, '신선한 과일 블렌딩 스무디.', 'https://picsum.photos/seed/menu-ft06-3/400/300', false, 3, NOW(), NOW());
```

---

## Step 8: 공연 스케줄 (12개)

```sql
INSERT INTO timelines (id, festival_id, day, title, artist, start_time, end_time, created_at, updated_at) VALUES
  -- 1일차 (2026-06-20)
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-20', '개막 공연',        '교내 밴드 연합',          '18:00', '18:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-20', '버스킹 공연',      '어쿠스틱 듀오 소리와 바람', '19:00', '20:00', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-20', '재즈 나이트',      '재즈 앙상블 블루문',       '20:30', '21:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-20', 'DJ 파티',          'DJ FESTA',                '22:00', '23:00', NOW(), NOW()),
  -- 2일차 (2026-06-21)
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-21', '댄스 공연',        '댄스동아리 플레이',        '17:00', '17:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-21', '포크 버스킹',      '포크 듀오 달빛',           '18:30', '19:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-21', '인디 밴드 공연',   '인디밴드 새벽',            '20:00', '21:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-21', '헤드라이너 공연',  '가수 하준',                '22:00', '23:30', NOW(), NOW()),
  -- 3일차 (2026-06-22)
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-22', '오프닝 퍼포먼스',  '댄스팀 스텝',              '16:00', '16:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-22', '통기타 버스킹',    '통기타 공연 서율',         '17:30', '18:30', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-22', '메인 공연',        '가수 리아',                '19:00', '21:00', NOW(), NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '2026-06-22', '피날레 + 불꽃놀이','피날레 공연팀',            '21:30', '22:30', NOW(), NOW());
```

---

## Step 9: 웨이팅

야간 부스 11~15번에 user01~10이 대기 중.  
각 유저는 최대 3개 부스 동시 대기 가능 (비즈니스 제약).

| 유저 | 대기 부스 | 활성 웨이팅 수 |
|------|-----------|---------------|
| user01 | night11, night12, night15 | 3 |
| user02 | night11, night13 | 2 |
| user03 | night12, night14 | 2 |
| user04 | night11, night13, night15 | 3 |
| user05 | night12, night14 | 2 |
| user06 | night13, night15 | 2 |
| user07 | night13 | 1 |
| user08 | night13 | 1 |
| user09 | night14 | 1 |
| user10 | night15 | 1 |

```sql
INSERT INTO waitings (id, booth_id, festival_id, user_id, party_size, status, call_count, registered_at, updated_at) VALUES
  -- night11 (버스킹 존): user01, user02, user04
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000001', 'user01', 2, 'WAITING', 0, NOW() - INTERVAL '40 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000001', 'user02', 3, 'WAITING', 0, NOW() - INTERVAL '35 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000001', 'user04', 4, 'WAITING', 0, NOW() - INTERVAL '30 minutes', NOW()),
  -- night12 (포토 부스): user01, user03, user05
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000001', 'user01', 2, 'WAITING', 0, NOW() - INTERVAL '25 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000001', 'user03', 1, 'WAITING', 0, NOW() - INTERVAL '20 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000001', 'user05', 2, 'WAITING', 0, NOW() - INTERVAL '15 minutes', NOW()),
  -- night13 (학생 주점): user02, user04, user06, user07, user08
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'user02', 4, 'WAITING', 0, NOW() - INTERVAL '50 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'user04', 2, 'WAITING', 0, NOW() - INTERVAL '45 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'user06', 3, 'WAITING', 0, NOW() - INTERVAL '38 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'user07', 1, 'WAITING', 0, NOW() - INTERVAL '30 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000001', 'user08', 2, 'WAITING', 0, NOW() - INTERVAL '22 minutes', NOW()),
  -- night14 (감성 포차): user03, user05, user09
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0000-000000000001', 'user03', 2, 'WAITING', 0, NOW() - INTERVAL '55 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0000-000000000001', 'user05', 3, 'WAITING', 0, NOW() - INTERVAL '42 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0000-000000000001', 'user09', 1, 'WAITING', 0, NOW() - INTERVAL '28 minutes', NOW()),
  -- night15 (심야 카페): user01, user04, user06, user10
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000001', 'user01', 1, 'WAITING', 0, NOW() - INTERVAL '60 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000001', 'user04', 2, 'WAITING', 0, NOW() - INTERVAL '48 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000001', 'user06', 2, 'WAITING', 0, NOW() - INTERVAL '33 minutes', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000001', 'user10', 3, 'WAITING', 0, NOW() - INTERVAL '18 minutes', NOW());
```

---

## Step 10: 즐겨찾기

user01~05 각 3개씩 총 15개

| 유저 | 즐겨찾기 부스 |
|------|-------------|
| user01 | 컴퓨터공학과(day01), 타코야키 포차(night01), 라멘야(night08) |
| user02 | 디자인학과(day03), 맥주 포차(night02), 떡볶이트럭(ft01) |
| user03 | 밴드동아리(day18), 칵테일 바(night06), 핫도그트럭(ft02) |
| user04 | 음악학과(day12), 학생 주점(night13), 아이스크림트럭(ft03) |
| user05 | 체육학과(day13), 버스킹 존(night11), 피자트럭(ft05) |

```sql
INSERT INTO favorites (id, festival_id, user_id, booth_id, created_at) VALUES
  -- user01
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user01', '00000000-0000-0000-0002-000000000001', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user01', '00000000-0000-0000-0003-000000000001', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user01', '00000000-0000-0000-0003-000000000008', NOW()),
  -- user02
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user02', '00000000-0000-0000-0002-000000000003', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user02', '00000000-0000-0000-0003-000000000002', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user02', '00000000-0000-0000-0004-000000000001', NOW()),
  -- user03
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user03', '00000000-0000-0000-0002-000000000018', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user03', '00000000-0000-0000-0003-000000000006', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user03', '00000000-0000-0000-0004-000000000002', NOW()),
  -- user04
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user04', '00000000-0000-0000-0002-000000000012', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user04', '00000000-0000-0000-0003-000000000013', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user04', '00000000-0000-0000-0004-000000000003', NOW()),
  -- user05
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user05', '00000000-0000-0000-0002-000000000013', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user05', '00000000-0000-0000-0003-000000000011', NOW()),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'user05', '00000000-0000-0000-0004-000000000005', NOW());
```

---

## 주의사항

- **공용 DB는 데이터 중복 삽입 주의**: festival, festival_days의 경우 고정 UUID를 사용하므로 이미 존재하면 오류 발생. `ON CONFLICT DO NOTHING` 추가 또는 사전 확인 필요
- **이미지 URL**: `https://picsum.photos/seed/{seed}/width/height` 는 seed 기반 고정 이미지를 반환 (실제 서버 업로드 없이 URL만 저장)
- **비밀번호**: 모든 계정 `1234` (bcrypt $2b$10$ 해시 적용)
- **웨이팅 제약**: 한 유저 최대 3개 동시 활성 웨이팅 — Step 9 데이터는 이를 준수함
- **메뉴 지원 부스**: 야간(NIGHT)과 푸드트럭(FOOD_TRUCK)만 메뉴 보유. 주간(DAY)은 메뉴 없음
