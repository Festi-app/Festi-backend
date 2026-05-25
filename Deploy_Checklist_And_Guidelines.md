# Festi Backend Deployment Checklist and Guidelines

이 문서는 Festi Backend를 Proxmox VE 위의 별도 Ubuntu Server 24.04 LTS VM에
Docker Compose로 배포하기 위한 실행 체크리스트입니다. Docker에 익숙하지 않은
운영자가 아래 순서대로 진행할 수 있도록 작성되었습니다.

작성 기준:

- 배포 대상 브랜치: `feat/docker-deploy`
- 실행 서비스: `postgres`, `api`
- 운영 데이터: PostgreSQL 데이터, 부스/메뉴 업로드 이미지
- API image 저장소: GitHub Container Registry(GHCR)
- API container 사용자: UID/GID `10001:10001`
- VM 영속 데이터 루트: `/srv/festi-data`

## 1. 배포 구조

```text
GitHub Repository
  -> GitHub Actions test / postgresTest
  -> Dockerfile로 API image 빌드
  -> GHCR에 ghcr.io/festi-app/festi-backend:<tag> 게시

Proxmox VE
  -> Ubuntu Server 24.04 LTS VM
      -> Docker Compose
          -> postgres container
              -> /srv/festi-data/postgres
          -> api container
              -> /srv/festi-data/images
              -> 127.0.0.1:8080
      -> 이후 추가할 HTTPS reverse proxy
```

각 파일의 역할은 다음과 같습니다.

| 파일 | 역할 |
| --- | --- |
| `Dockerfile` | 소스 코드를 Festi API Docker image로 빌드하고 UID `10001`로 실행합니다. |
| `.dockerignore` | `.env`, 로컬 빌드 결과물 등을 Docker image 빌드 대상에서 제외합니다. |
| `.github/workflows/festi-workflows.yml` | 테스트가 통과한 `main` push에서 GHCR image를 게시합니다. |
| `docker-compose.yml` | VM에서 PostgreSQL과 게시된 API image를 실행합니다. |
| VM의 `/opt/festi/.env` | 운영 비밀번호, JWT secret, 실제 image tag와 도메인을 보관합니다. Git에 올리지 않습니다. |

## 2. 현재 구성에서 먼저 확인할 사항

### 2.1 배포 전 수정 권장: API 포트 표기 일치

현재 애플리케이션과 Compose는 API container 내부 포트 `8080`을 기준으로 합니다.

```text
README.md:          Spring Boot 기본 포트 8080
docker-compose.yml: 127.0.0.1:<host-port>:8080
```

그러나 현재 `Dockerfile`에는 다음처럼 기록되어 있습니다.

```dockerfile
EXPOSE 45566
```

`EXPOSE`는 단독으로 애플리케이션 포트를 변경하지 않으므로 Compose의 `8080`
연결이 실제 실행을 즉시 깨뜨리지는 않습니다. 하지만 운영 문서와 image 정보가
달라 혼동을 만들 수 있습니다. 첫 공개용 GHCR image를 만들기 전에 다음처럼
맞추는 것을 권장합니다.

```dockerfile
EXPOSE 8080
```

체크:

- [x] `Dockerfile`의 `EXPOSE`가 Compose target port 및 Spring Boot port와 일치한다.

### 2.2 현재 Compose에는 reverse proxy가 없음

현재 API는 다음처럼 VM의 loopback 주소에만 노출됩니다.

```yaml
ports:
  - "127.0.0.1:${FESTI_API_PORT:-8080}:8080"
```

이는 초기 점검과 같은 VM에 설치되는 reverse proxy 연결에는 적합합니다. 그러나
인터넷 사용자는 이 상태로 API에 직접 접속할 수 없습니다. 외부 공개 전에
Caddy, Nginx 또는 Traefik을 추가하여 HTTPS 요청을 `127.0.0.1:8080`으로
전달해야 합니다.

체크:

- [ ] 최초 API 기동은 VM 내부에서 검증한다.
- [ ] 외부 공개 전 HTTPS reverse proxy를 별도로 구성한다.
- [ ] PostgreSQL `5432` 포트는 외부에 공개하지 않는다.

### 2.3 Web Push를 아직 사용하지 않는 경우

현재 Web Push worker는 설정을 생략하면 활성화됩니다. worker를 끄면 웨이팅
호출 시 저장된 outbox event가 `PENDING` 상태로 남을 수 있습니다. 이후 Push를
활성화하면서 worker를 켜면 과거 호출 알림을 뒤늦게 처리할 위험이 있습니다.

따라서 설정은 사용 상황에 따라 구분합니다.

| 상황 | `FESTI_WEB_PUSH_ENABLED` | `FESTI_WEB_PUSH_WORKER_ENABLED` | 이유 |
| --- | --- | --- | --- |
| 최초 설치 점검 중이며 실제 웨이팅 호출을 사용하지 않음 | `false` | `false` | 외부 발송 및 background 처리를 모두 멈춥니다. |
| Push 없이 웨이팅 호출 기능을 실제로 운영함 | `false` | `true` | outbox를 쌓아두지 않고 비활성 발송 결과로 처리합니다. |
| HTTPS/VAPID 준비 후 Push를 실제 운영함 | `true` | `true` | 실제 알림을 발송합니다. |

worker를 꺼둔 상태에서 실제 웨이팅 호출 테스트를 수행했다면, Push 활성화 전에
남은 event 처리 방침을 확인해야 합니다.

## 3. 전체 체크리스트

### A. 저장소와 GHCR image 준비

- [x] 배포 관련 변경을 PR로 검토하고 `main`에 병합한다.
- [ ] 포트 표기를 `8080` 기준으로 정리한다.
- [x] GitHub Actions의 `fast-tests`가 성공한다.
- [x] GitHub Actions의 `postgres-migration-tests`가 성공한다.
- [x] `publish-docker-image` job이 성공한다.
- [x] GHCR Packages 화면에서 `ghcr.io/festi-app/festi-backend:sha-...` tag를 확인한다.
- [x] 운영에 사용할 immutable `sha-...` tag를 기록한다.

### B. Proxmox VM 준비

- [ ] 별도 QEMU/KVM VM을 생성한다.
- [ ] Ubuntu Server `24.04 LTS`를 설치한다.
- [ ] CPU를 `1 socket / 4 cores`, CPU type `host`로 설정한다.
- [ ] 메모리를 `8 GB`, ballooning off로 설정한다.
- [ ] OS disk `32 GB`와 data disk `128 GB`를 연결한다.
- [ ] QEMU Guest Agent를 VM 옵션과 Ubuntu 양쪽에서 활성화한다.
- [ ] VM 고정 IP 또는 DHCP reservation을 설정한다.

### C. VM 데이터 디스크 준비

- [ ] Ubuntu에서 128 GB disk가 올바른 빈 data disk인지 확인한다.
- [ ] data disk를 ext4로 포맷한다. 기존 데이터가 있는 disk에는 실행하지 않는다.
- [ ] data disk를 `/srv/festi-data`에 마운트한다.
- [ ] `/etc/fstab`에 UUID 자동 마운트를 등록한다.
- [ ] 재부팅 후에도 `/srv/festi-data` 마운트를 확인한다.

### D. Docker와 배포 파일 준비

- [ ] Docker Engine과 Docker Compose plugin을 설치한다.
- [ ] `/opt/festi/docker-compose.yml`을 배치한다.
- [ ] `/opt/festi/.env`를 만들고 권한을 `600`으로 제한한다.
- [ ] `/srv/festi-data/postgres` 경로를 만든다.
- [ ] `/srv/festi-data/images` 경로를 UID/GID `10001:10001` 소유로 만든다.
- [ ] private GHCR package인 경우 VM에서 pull 전용 token으로 로그인한다.

### E. 최초 실행

- [ ] `docker compose config --quiet`로 변수와 문법을 확인한다.
- [ ] GHCR API image를 pull한다.
- [ ] PostgreSQL container를 먼저 기동하고 healthcheck를 확인한다.
- [ ] API container를 기동한다.
- [ ] API 로그에서 Flyway migration 성공을 확인한다.
- [ ] API 로그에서 Hibernate schema validation 및 서버 기동 성공을 확인한다.
- [ ] VM 내부에서 Swagger/OpenAPI endpoint 응답을 확인한다.
- [ ] 이미지 디렉터리가 API container에서 writable인지 확인한다.

### F. 공개 전 운영 준비

- [ ] HTTPS reverse proxy를 설정한다.
- [ ] `FESTI_CORS_ALLOWED_ORIGINS`를 실제 frontend HTTPS origin으로 설정한다.
- [ ] API, PostgreSQL 포트를 직접 인터넷에 공개하지 않는다.
- [ ] Web Push 사용 여부를 확정하고 VAPID key를 안전하게 보관한다.
- [ ] PostgreSQL dump 백업을 만든다.
- [ ] 이미지 경로 백업을 만든다.
- [ ] Proxmox VM 백업 작업을 설정한다.

## 4. 단계별 실행 가이드

## Step 1. GHCR에 배포 가능한 API Image 만들기

### 사용자가 해야 할 일

1. `feat/docker-deploy` 브랜치의 변경을 검토합니다.
2. 포트 표기 정리 작업을 반영합니다.
3. PR을 생성하여 `main`으로 병합합니다.
4. GitHub Actions 실행 결과를 확인합니다.

현재 workflow는 다음 순서로 동작합니다.

```text
pull_request 또는 main push
  -> fast-tests: ./gradlew test
  -> postgres-migration-tests: ./gradlew postgresTest

main push이며 위 두 job 성공
  -> publish-docker-image
      -> Dockerfile build
      -> GHCR push
      -> image provenance 생성
```

현재 workflow가 게시하는 image 이름은 다음과 같습니다.

```text
ghcr.io/festi-app/festi-backend
```

게시되는 tag:

| Tag | 용도 |
| --- | --- |
| `main` | 가장 최근 `main` image를 가리키므로 편리하지만 운영 버전 고정용으로는 부적합합니다. |
| `sha-...` | 특정 commit에서 빌드된 image를 가리키므로 운영 배포에 권장합니다. |

운영 `.env`에는 `main` 대신 GitHub Actions 결과에서 확인한 `sha-...` tag를
사용합니다.

```env
FESTI_API_IMAGE=ghcr.io/festi-app/festi-backend:sha-<workflow에서-확인한-tag>
```

### 확인 방법

GitHub 웹 UI에서 다음 위치를 확인합니다.

```text
Repository -> Actions -> Festi Backend CI Workflow
Repository 또는 Organization -> Packages -> festi-backend
```

확인 항목:

- [ ] `fast-tests` success
- [ ] `postgres-migration-tests` success
- [ ] `publish-docker-image` success
- [ ] 배포할 `sha-...` image tag 확인

## Step 2. Proxmox VM과 Ubuntu 기본 설정

### 권장 VM 설정

| 항목 | 권장값 |
| --- | --- |
| VM 유형 | QEMU/KVM |
| OS | Ubuntu Server 24.04 LTS |
| CPU | `1 socket / 4 cores`, type `host` |
| RAM | `8192 MiB`, ballooning off |
| OS disk | `32 GB` |
| Data disk | `128 GB`, SSD 기반 권장 |
| Disk controller | `VirtIO SCSI single` |
| Network | VirtIO, 고정 IP 또는 DHCP reservation |
| Guest Agent | Enabled |

### 사용자가 해야 할 일

Ubuntu에 SSH로 접속한 후 기본 패키지를 갱신하고 Guest Agent를 설치합니다.

```bash
sudo apt update
sudo apt full-upgrade -y
sudo apt install -y qemu-guest-agent ca-certificates curl openssl rsync
sudo systemctl enable --now qemu-guest-agent
sudo timedatectl set-timezone Asia/Seoul
```

커널 업데이트가 있었다면 재부팅합니다.

```bash
sudo reboot
```

재접속 후 확인:

```bash
systemctl status qemu-guest-agent --no-pager
timedatectl
```

체크:

- [ ] Proxmox VM Options에서 QEMU Guest Agent가 Enable 상태다.
- [ ] Ubuntu의 `qemu-guest-agent` service가 active 상태다.
- [ ] 시간대가 의도한 값이다.

## Step 3. 128 GB Data Disk를 `/srv/festi-data`로 사용하기

이 단계에는 포맷 명령이 포함됩니다. **빈 128 GB data disk가 확실할 때만**
파티션 생성과 `mkfs.ext4`를 실행합니다. OS가 들어 있는 32 GB disk를
포맷하면 VM이 손상됩니다.

### 3.1 장치 확인

```bash
lsblk -o NAME,SIZE,TYPE,FSTYPE,MOUNTPOINTS,MODEL
sudo fdisk -l
```

예상 예시:

```text
NAME   SIZE TYPE FSTYPE MOUNTPOINTS
sda     32G disk
└─sda2  31G part ext4   /
sdb    128G disk
```

이후 예시는 새 data disk가 `/dev/sdb`라고 가정합니다. 실제 장치 이름이
`/dev/vdb` 등으로 보이면 명령의 장치명을 반드시 실제 값으로 바꿉니다.

### 3.2 빈 data disk 초기화

```bash
sudo parted /dev/sdb --script mklabel gpt
sudo parted /dev/sdb --script mkpart primary ext4 0% 100%
sudo partprobe /dev/sdb
sudo mkfs.ext4 -L festi-data /dev/sdb1
```

### 3.3 마운트 및 자동 마운트

```bash
sudo mkdir -p /srv/festi-data
sudo mount /dev/sdb1 /srv/festi-data
sudo blkid /dev/sdb1
```

`blkid` 출력의 `UUID="..."` 값을 확인한 후 `/etc/fstab`에 추가합니다.
UUID는 꺾쇠괄호나 따옴표 없이 입력합니다.

```bash
sudo nano /etc/fstab
```

```fstab
UUID=실제-UUID-값 /srv/festi-data ext4 defaults,nofail 0 2
```

검증:

```bash
sudo umount /srv/festi-data
sudo mount -a
findmnt /srv/festi-data
df -h /srv/festi-data
sudo systemctl enable --now fstrim.timer
```

체크:

- [ ] `findmnt /srv/festi-data`가 data disk를 표시한다.
- [ ] `df -h /srv/festi-data`가 약 128 GB 용량을 표시한다.
- [ ] 재부팅 후에도 mount가 유지된다.

## Step 4. Docker Engine과 Compose Plugin 설치

Docker 공식 apt repository를 사용합니다. Ubuntu 24.04 LTS는 Docker Engine
공식 지원 대상입니다.

### 사용자가 해야 할 일

충돌 가능한 기존 패키지를 제거합니다.

```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do
  sudo apt-get remove -y "$pkg"
done
```

Docker apt key와 repository를 등록합니다.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
```

Docker와 Compose plugin을 설치합니다.

```bash
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

동작 확인:

```bash
sudo docker run --rm hello-world
sudo docker compose version
```

운영 VM에서는 사용자 계정을 `docker` group에 추가하지 않는 것을 권장합니다.
`docker` group은 사실상 root 수준 권한을 제공합니다. 이 문서에서는 모든 운영
명령을 `sudo docker ...` 형태로 실행합니다.

체크:

- [ ] `docker run --rm hello-world`가 성공한다.
- [ ] `docker compose version`이 출력된다.
- [ ] Docker service가 boot 시 자동 시작되도록 enabled 상태다.

## Step 5. VM 데이터 경로와 UID 권한 준비

### UID `10001`이 필요한 이유

현재 `Dockerfile`은 API process를 root가 아닌 다음 사용자 번호로 실행합니다.

```dockerfile
USER 10001:10001
```

또한 API는 업로드 이미지를 직접 저장하고 삭제합니다.

```text
VM host:       /srv/festi-data/images
API container: /data/festi/images
```

bind mount에서는 두 경로가 같은 실제 파일을 가리킵니다. 따라서 VM의
`/srv/festi-data/images`를 UID `10001`이 쓸 수 있도록 설정해야 합니다.

### 사용자가 해야 할 일

먼저 data disk 마운트를 다시 확인합니다.

```bash
mountpoint /srv/festi-data
df -h /srv/festi-data
```

마운트가 확인된 뒤 하위 경로를 만듭니다.

```bash
sudo install -d -m 0700 /srv/festi-data/postgres
sudo install -d -o 10001 -g 10001 -m 0750 /srv/festi-data/images
sudo install -d -m 0700 /srv/festi-data/backups
sudo install -d -m 0700 /srv/festi-data/backups/db
```

권한 확인:

```bash
ls -ldn /srv/festi-data/postgres /srv/festi-data/images
```

이미지 경로 결과에는 숫자 owner/group `10001 10001`이 보여야 합니다.

PostgreSQL 디렉터리는 API UID로 바꾸지 않습니다. `postgres:16-alpine` container가
자신의 데이터 디렉터리를 초기화하고 필요한 권한을 관리하도록 둡니다.

체크:

- [ ] `/srv/festi-data`가 실제 data disk 위에 mount되어 있다.
- [ ] `/srv/festi-data/images` 소유자가 `10001:10001`이다.
- [ ] `/srv/festi-data/postgres`는 이미지 경로와 구분되어 있다.

## Step 6. Compose 파일과 운영 `.env` 배치

### 배포 디렉터리 만들기

```bash
sudo install -d -o root -g root -m 0750 /opt/festi
```

`main`에 병합된 `docker-compose.yml`을 VM의 `/opt/festi/docker-compose.yml`로
전달합니다. 예를 들어 로컬 개발 PC에서 실행할 경우:

```bash
scp docker-compose.yml <vm-user>@<vm-ip>:/tmp/docker-compose.yml
```

VM에서 운영 위치로 설치합니다.

```bash
sudo install -o root -g root -m 0644 \
  /tmp/docker-compose.yml /opt/festi/docker-compose.yml
rm -f /tmp/docker-compose.yml
```

### 운영 secret 생성

VM에서 DB password와 JWT secret의 후보 값을 생성합니다.

```bash
openssl rand -base64 32
openssl rand -base64 48
```

첫 번째 값을 PostgreSQL password로, 두 번째 값을 JWT secret으로 사용합니다.
값은 password manager 등 안전한 위치에도 보관합니다.

### `.env` 작성

```bash
sudo nano /opt/festi/.env
```

Push를 아직 사용하지 않는 최초 배포 예시:

```env
# Published API image: GitHub Actions에서 확인한 sha tag를 사용합니다.
FESTI_API_IMAGE=ghcr.io/festi-app/festi-backend:sha-실제-tag

# PostgreSQL
FESTI_DATABASE_NAME=festi
FESTI_DATABASE_USERNAME=festi_app
FESTI_DATABASE_PASSWORD=생성한-강한-db-password

# Authentication and browser access
FESTI_JWT_SECRET=생성한-강한-jwt-secret
FESTI_JWT_ACCESS_TOKEN_EXPIRATION=3600
FESTI_CORS_ALLOWED_ORIGINS=https://실제-frontend-domain

# VM persistence paths
FESTI_POSTGRES_DATA_ROOT=/srv/festi-data/postgres
FESTI_IMAGE_DATA_ROOT=/srv/festi-data/images

# API is bound to localhost by docker-compose.yml.
FESTI_API_PORT=8080

# Upload validation
FESTI_IMAGE_MAX_FILE_SIZE=5MB
FESTI_IMAGE_MAX_REQUEST_SIZE=6MB
FESTI_IMAGE_MAX_WIDTH=4096
FESTI_IMAGE_MAX_HEIGHT=4096

# Installation-only setting: do not trigger real waiting calls in this state.
FESTI_WEB_PUSH_ENABLED=false
FESTI_WEB_PUSH_WORKER_ENABLED=false
```

Push는 아직 없지만 웨이팅 호출 기능을 실제 운영하기 시작할 때는 다음처럼
worker를 활성화합니다.

```env
FESTI_WEB_PUSH_ENABLED=false
FESTI_WEB_PUSH_WORKER_ENABLED=true
```

파일 권한을 제한합니다.

```bash
sudo chown root:root /opt/festi/.env
sudo chmod 600 /opt/festi/.env
sudo ls -l /opt/festi/.env
```

주의:

- `.env`는 Git에 commit하지 않습니다.
- `.env`를 Docker image 안에 복사하지 않습니다.
- 화면 공유, issue, PR comment, 로그에 secret 값을 붙여 넣지 않습니다.

## Step 7. Private GHCR Image를 VM에서 Pull할 수 있게 설정

GHCR package가 public이면 로그인 없이 pull할 수 있습니다. Private package라면
GitHub에서 `read:packages` 권한만 가진 pull용 token을 준비합니다.

### 사용자가 해야 할 일

VM에서 token을 입력받아 GHCR에 로그인합니다.

```bash
read -rsp "GHCR read token: " GHCR_PAT
echo
printf '%s' "$GHCR_PAT" | sudo docker login ghcr.io \
  -u '<github-username>' --password-stdin
unset GHCR_PAT
```

성공하면 다음 단계에서 API image를 pull할 수 있습니다.

체크:

- [ ] token은 `read:packages`만 가진다.
- [ ] `docker login ghcr.io`가 성공한다.
- [ ] token 원문을 `.env`에 넣지 않았다.

## Step 8. Compose 구성 검증과 Image Pull

### 사용자가 해야 할 일

```bash
cd /opt/festi
sudo docker compose --env-file .env config --quiet
sudo docker compose --env-file .env pull
```

`config --quiet`가 실패할 때는 오류가 가리키는 필수 환경 변수를 확인합니다.
현재 Compose가 필수로 요구하는 값은 다음입니다.

| 변수 | 의미 |
| --- | --- |
| `FESTI_API_IMAGE` | 실행할 GHCR API image tag |
| `FESTI_DATABASE_PASSWORD` | PostgreSQL 및 API DB 접속 password |
| `FESTI_JWT_SECRET` | JWT 서명 secret |
| `FESTI_CORS_ALLOWED_ORIGINS` | 브라우저 frontend origin |

체크:

- [ ] Compose config validation이 성공한다.
- [ ] `postgres:16-alpine` image pull이 성공한다.
- [ ] GHCR의 Festi API image pull이 성공한다.

## Step 9. PostgreSQL 최초 기동

PostgreSQL을 먼저 시작하고 정상 상태를 확인합니다.

```bash
cd /opt/festi
sudo docker compose --env-file .env up -d postgres
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --tail=100 postgres
```

healthcheck 확인:

```bash
sudo docker compose --env-file .env exec postgres \
  sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

체크:

- [ ] `postgres` container가 `healthy` 상태다.
- [ ] `/srv/festi-data/postgres`에 초기화된 DB 데이터가 생성되었다.
- [ ] VM 외부에서 `5432`를 공개하지 않았다.

## Step 10. API 최초 기동과 Flyway 확인

### 사용자가 해야 할 일

```bash
cd /opt/festi
sudo docker compose --env-file .env up -d api
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs -f --tail=200 api
```

첫 실행에서 API는 다음 순서로 진행합니다.

```text
PostgreSQL 연결
  -> Flyway migration 적용
  -> Hibernate schema validation
  -> Spring Boot HTTP server 시작
```

확인해야 할 로그:

- [ ] DB 연결 오류가 없다.
- [ ] Flyway migration 실패가 없다.
- [ ] Hibernate schema validation 실패가 없다.
- [ ] Spring Boot가 정상 기동되었다.

로그 확인을 끝내려면 `Ctrl+C`를 누릅니다. 이는 container를 중지하지 않습니다.

## Step 11. 최초 Smoke Test와 이미지 권한 확인

현재 API는 VM 내부 `127.0.0.1:8080`에만 노출됩니다. VM 안에서 점검합니다.

```bash
curl -fsS http://127.0.0.1:8080/v3/api-docs >/dev/null \
  && echo "API response OK"
```

API container의 사용자와 mount 경로를 확인합니다.

```bash
cd /opt/festi
sudo docker compose --env-file .env exec api id
sudo docker compose --env-file .env exec api ls -ldn /data/festi/images
ls -ldn /srv/festi-data/images
```

예상 핵심 결과:

```text
uid=10001(festi) gid=10001(festi)
/data/festi/images       owner 10001 group 10001
/srv/festi-data/images   owner 10001 group 10001
```

관리자 기능으로 실제 JPEG 또는 PNG 이미지를 한 번 업로드한 뒤 VM에서 파일이
생겼는지 확인합니다.

```bash
find /srv/festi-data/images -maxdepth 2 -type f -print
```

체크:

- [ ] OpenAPI endpoint가 VM 내부에서 응답한다.
- [ ] API process의 UID/GID가 `10001:10001`이다.
- [ ] 이미지 upload 후 `/srv/festi-data/images`에 파일이 유지된다.

## Step 12. HTTPS Reverse Proxy와 외부 공개

현재 Compose는 API와 PostgreSQL만 포함합니다. 외부 사용자를 받으려면 같은
VM에 reverse proxy를 추가하는 구성이 가장 단순합니다.

```text
Internet HTTPS :443
  -> Caddy/Nginx/Traefik on same VM
      -> http://127.0.0.1:8080
          -> Festi API container
```

외부 공개 전 해야 할 일:

- [ ] 도메인의 DNS record를 VM 공개 IP 또는 tunnel endpoint에 연결한다.
- [ ] HTTPS certificate가 정상 발급된다.
- [ ] reverse proxy가 `127.0.0.1:8080`으로 요청을 전달한다.
- [ ] `FESTI_CORS_ALLOWED_ORIGINS`에 실제 frontend HTTPS origin을 넣는다.
- [ ] API container를 직접 외부 포트로 공개하지 않는다.
- [ ] PostgreSQL container를 외부에 공개하지 않는다.

참고로 Swagger/OpenAPI endpoint는 현재 애플리케이션 security 설정에서 공개
접근을 허용합니다. 운영에서 공개 문서를 원하지 않는다면 reverse proxy에서
접근을 제한하거나 애플리케이션 정책을 별도로 수정합니다.

## Step 13. Web Push 활성화

Web Push는 HTTPS frontend와 service worker 준비 이후 활성화합니다. VAPID key는
한번 운영에 사용한 뒤 재배포마다 새로 생성하지 않고 안전하게 보존합니다.

### Push를 활성화할 때 `.env` 변경

```env
FESTI_WEB_PUSH_ENABLED=true
FESTI_WEB_PUSH_WORKER_ENABLED=true
FESTI_VAPID_PUBLIC_KEY=실제-public-key
FESTI_VAPID_PRIVATE_KEY=실제-private-key
FESTI_VAPID_SUBJECT=mailto:실제-admin-email
FESTI_WEB_PUSH_TTL_SECONDS=300
```

변경 적용:

```bash
cd /opt/festi
sudo docker compose --env-file .env up -d api
sudo docker compose --env-file .env logs --tail=200 api
```

체크:

- [ ] HTTPS frontend가 browser push subscription을 생성한다.
- [ ] VAPID private key가 `.env` 이외의 공개 위치에 노출되지 않는다.
- [ ] 실제 웨이팅 호출에서 Push 수신을 확인한다.

## Step 14. 백업 설정

운영 데이터는 두 종류입니다.

| 대상 | 실제 위치 | 백업 방법 |
| --- | --- | --- |
| PostgreSQL | `/srv/festi-data/postgres` | 실행 중인 데이터 디렉터리 복사 대신 `pg_dump` 사용 |
| 업로드 이미지 | `/srv/festi-data/images` | `rsync` 또는 외부 storage 백업 |

### PostgreSQL dump 만들기

기본 DB 이름과 사용자 이름을 사용한다면:

```bash
cd /opt/festi
sudo docker compose --env-file .env exec -T postgres \
  pg_dump -U festi_app -d festi -Fc \
  | sudo tee /srv/festi-data/backups/db/festi-$(date +%F).dump >/dev/null
```

`.env`에서 DB 이름이나 사용자 이름을 바꿨다면 위 명령의 `-U`, `-d` 값도
같이 바꿉니다.

### 이미지 백업 만들기

아래의 `/mnt/external-backup`은 NAS, PBS와 별도로 관리되는 mount, 또는 다른
외부 저장소 경로로 교체합니다.

```bash
sudo rsync -a /srv/festi-data/images/ \
  /mnt/external-backup/festi/images/
```

### Proxmox VM 백업

VM 전체 백업도 활성화합니다. QEMU Guest Agent가 정상 동작해야 backup 시
filesystem 일관성 확보에 도움이 됩니다.

체크:

- [ ] PostgreSQL dump가 VM 외부 저장소에 보관된다.
- [ ] 이미지 백업이 VM 외부 저장소에 보관된다.
- [ ] Proxmox VM 정기 백업이 설정되어 있다.
- [ ] migration이 포함된 배포 직전 별도 DB dump를 만든다.

## Step 15. 새 API 버전 배포

새 버전을 배포할 때는 container를 직접 수정하지 않습니다. 새 commit에서
생성된 GHCR image tag로 `.env`를 변경하고 container를 교체합니다.

### 사용자가 해야 할 일

1. GitHub Actions에서 새로운 `sha-...` image tag를 확인합니다.
2. DB migration이 포함되었으면 먼저 DB dump를 만듭니다.
3. `/opt/festi/.env`의 `FESTI_API_IMAGE`를 새 tag로 변경합니다.
4. 새 image를 pull하고 API container를 다시 생성합니다.

```bash
cd /opt/festi
sudo nano .env
sudo docker compose --env-file .env pull api
sudo docker compose --env-file .env up -d api
sudo docker compose --env-file .env logs --tail=200 api
```

검증:

```bash
curl -fsS http://127.0.0.1:8080/v3/api-docs >/dev/null \
  && echo "Updated API response OK"
```

주의:

- 이전 API image tag로 바꾸는 것은 container code rollback입니다.
- Flyway migration이 이미 DB에 적용되었다면 image tag만 되돌려 완전한 DB
  rollback이 되지 않습니다.
- schema 변경 배포 전에는 반드시 DB backup을 확보합니다.

## 5. 문제 발생 시 점검표

| 증상 | 먼저 확인할 것 | 명령 |
| --- | --- | --- |
| API image를 받을 수 없음 | GHCR package 공개 여부 또는 login token 권한 | `sudo docker pull ghcr.io/festi-app/festi-backend:<tag>` |
| Compose가 실행 전 실패함 | 필수 `.env` 값 누락 | `sudo docker compose --env-file .env config` |
| PostgreSQL이 시작되지 않음 | data disk mount, DB 경로 권한, 로그 | `mountpoint /srv/festi-data`; `sudo docker compose --env-file .env logs postgres` |
| API가 반복 재시작함 | DB 연결, Flyway, JWT/VAPID 설정 | `sudo docker compose --env-file .env logs api` |
| 이미지 업로드가 실패함 | UID `10001` write permission | `sudo docker compose --env-file .env exec api id`; `ls -ldn /srv/festi-data/images` |
| 외부에서 API 접속 불가 | 현재 API는 localhost 전용임 | reverse proxy/HTTPS 설정 확인 |
| Push를 아직 사용하지 않음 | 실제 웨이팅 호출 사용 여부를 구분해야 함 | 설치 점검만 하면 worker `false`; 웨이팅을 운영하면 worker `true` |
| Push 활성화 직후 오래된 알림이 우려됨 | worker off 동안 누적된 pending outbox가 있을 수 있음 | 활성화 전 DB event 상태와 테스트 호출 내역 확인 |
| DB 파일이 OS disk에 생김 | data disk mount 전에 container를 실행했을 가능성 | `findmnt /srv/festi-data`; Compose 중지 후 데이터 위치 조사 |

## 6. 운영 명령 빠른 참조

모든 명령은 VM의 `/opt/festi`에서 실행합니다.

```bash
cd /opt/festi
```

| 목적 | 명령 |
| --- | --- |
| Compose 설정 검증 | `sudo docker compose --env-file .env config --quiet` |
| 상태 확인 | `sudo docker compose --env-file .env ps` |
| 전체 시작 | `sudo docker compose --env-file .env up -d` |
| API만 재시작/교체 | `sudo docker compose --env-file .env up -d api` |
| 새 API image 다운로드 | `sudo docker compose --env-file .env pull api` |
| API 로그 | `sudo docker compose --env-file .env logs -f --tail=200 api` |
| PostgreSQL 로그 | `sudo docker compose --env-file .env logs -f --tail=200 postgres` |
| 전체 정지 | `sudo docker compose --env-file .env down` |
| API container UID 확인 | `sudo docker compose --env-file .env exec api id` |
| VM data disk 확인 | `findmnt /srv/festi-data && df -h /srv/festi-data` |

`docker compose down`은 container와 network를 내리지만, 현재 bind mount로
관리하는 `/srv/festi-data/postgres` 및 `/srv/festi-data/images`의 파일을
자동 삭제하지 않습니다. 다만 직접 디렉터리를 지우는 명령은 실행하지 않습니다.

## 7. 완료 기준

최초 내부 배포 완료:

- [ ] GHCR에서 `sha-...` API image가 생성되었다.
- [ ] VM의 data disk가 `/srv/festi-data`로 자동 마운트된다.
- [ ] PostgreSQL 및 API container가 실행 상태다.
- [ ] Flyway migration과 schema validation이 성공했다.
- [ ] VM 내부에서 `http://127.0.0.1:8080/v3/api-docs`가 응답한다.
- [ ] 이미지 저장 경로가 UID/GID `10001:10001`로 쓰기 가능하다.

외부 운영 공개 완료:

- [ ] HTTPS reverse proxy가 동작한다.
- [ ] 실제 frontend origin에 맞춘 CORS 설정이 적용되었다.
- [ ] Web Push를 사용하는 경우 VAPID 설정과 실제 수신을 확인했다.
- [ ] DB dump, 이미지 백업, Proxmox VM 백업이 모두 준비되었다.
- [ ] 배포 및 장애 발생 시 되돌릴 GHCR image tag를 기록해 두었다.

## 8. 참고 자료

- Docker Engine on Ubuntu: <https://docs.docker.com/engine/install/ubuntu/>
- Docker Compose installation: <https://docs.docker.com/compose/install/>
- Docker bind mounts: <https://docs.docker.com/engine/storage/bind-mounts/>
- Dockerfile best practices: <https://docs.docker.com/build/building/best-practices/>
- GitHub Container Registry: <https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- GitHub Actions Docker image publishing: <https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images>
