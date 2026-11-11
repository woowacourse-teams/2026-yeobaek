# 개발 서버 무중단 배포와 기록

`deploy.sh`는 새 앱을 준비한 뒤 Nginx의 전달 대상을 전환하고, 기존 worker가 요청을 마무리한 뒤 이전 앱을 정리한다.
전환 확인 전에 실패하면 기존 앱으로 복구한다. 전환 확인 후 이전 앱 정리에 실패하면 새 앱을 유지하고 실패 결과를 남긴다.
배포 스크립트 성공 여부와 사용자 요청이 끊겼는지는 별개다. 무중단 여부는 배포 중 외부 요청 측정으로 확인한다.

## 실행 파일과 준비 조건

- `deploy.sh`와 `deploy_support.py`는 같은 디렉터리에 둔다. Python 외부 패키지는 필요하지 않다.
- 인자 없이 `bash deploy.sh`를 실행하면 읽기 전용 사전 확인만 한다.
- 실제 배포는 `bash deploy.sh <이미지주소:커밋SHA> <40자리커밋SHA>`로 실행한다.
- 기본 배포 디렉터리는 `/home/ec2-user/yeobaek-dev`이며, 그 안의 `docker-compose.yml`과 `.env`를 사용한다.
- 현재 앱도 `/health`에서 `Deployed-Commit-SHA`를 응답해야 복구 버전을 확인할 수 있다.
- 다음 배포 위치에 이전 컨테이너가 남아 있으면, 정지 상태라도 자동으로 덮어쓰지 않고 중단한다.
- Linux의 Python 3.8 이상, Docker Compose, curl, flock, timeout, ps와 스크립트에서 사용하는 Nginx·파일 명령의 비대화식 sudo 실행 권한이 필요하다.

| 역할 | Compose 서비스 | 컨테이너 | 호스트 → 앱 포트 |
|---|---|---|---|
| Blue | `app` | `yeobaek-app-blue` | `127.0.0.1:8080` → `8080` |
| Green | `app-green` | `yeobaek-app-green` | `127.0.0.1:8081` → `8080` |

서버에 기존 이름 `yeobaek-app`의 컨테이너가 있다면 이름 전환을 준비한 뒤 이 스크립트를 사용한다.
스크립트가 실행 중인 컨테이너 이름을 자동으로 변경하지는 않는다.

기본 Nginx 설정은 `/etc/nginx/conf.d/yeobaek-dev.conf`, 확인 도메인은 `https://yeobaekdev.duckdns.org/health`다.
`DEPLOY_DIR`, `NGINX_CONFIG`, `NGINX_ERROR_LOG`, `HEALTH_URL`로 경로와 주소를 지정할 수 있다.
기존 worker의 대기 제한은 `WORKER_WAIT_SECONDS`로 지정하며 기본값은 120초다. 초과 시 앱을 유지한다.

## 서버에 생기는 구조

```text
/home/ec2-user/yeobaek-dev/
└── deployment/
    ├── deploy.lock
    └── records/
        └── <전체SHA>__20261111-180000-KST__Ab12Cd/
            ├── deployment.json
            ├── deployment.log
            ├── health-checks.json
            ├── compose-image-override.yml
            ├── nginx/
            │   ├── config-before-deploy.conf
            │   ├── config-to-apply.conf
            │   ├── all-configs-before-deploy.conf
            │   ├── config-check.log
            │   ├── workers-before-switch.json
            │   └── worker-drain.log
            ├── containers/
            │   ├── yeobaek-app-blue/
            │   │   ├── application.log
            │   │   ├── shutdown.json
            │   │   ├── final-state.json
            │   │   └── inspection-error.log
            │   └── yeobaek-app-green/
            │       └── 같은 종류의 파일
            └── rollback/
                ├── nginx-workers-before-restore.json
                └── nginx-worker-drain.log
```

해당 단계에 도달했을 때만 파일이 만들어진다. `rollback/`은 복구를 시도할 때 만든다.
원본 로그는 원래 내용을 보존하며, 스크립트가 생성하는 시각은 KST(`+09:00`)다.

### 공통 폴더와 파일

| 경로 | 내용과 필요한 이유 |
|---|---|
| `deployment/` | 배포 잠금과 기록을 모아 Compose·앱 설정 파일과 구분한다. |
| `deploy.lock` | 동시 배포를 막는 잠금 파일. 파일 하나를 재사용하며 실행이 끝나면 잠금은 해제된다. 파일이 남아 있는 것과 잠금이 잡혀 있는 것은 다르다. |
| `records/` | 배포별 기록 폴더를 보관한다. |
| `<SHA>__<KST시각>__<구분값>/` | 배포 한 번의 기록. SHA로 검색하고 같은 커밋의 재배포는 시각으로 구분한다. |
| `deployment.json` | 배포 대상, 기존 앱, 시작·종료 시각, 결과, 복구 결과를 한곳에 기록한다. 문제 발생 시 먼저 확인할 파일이다. |
| `deployment.log` | 진행 단계와 경고를 시간순으로 기록한다. 결과에 이르게 된 경위를 확인한다. |
| `health-checks.json` | 기존 앱·새 앱·복구된 앱에 대한 헬스체크 결과를 모은다. 어떤 주소에서 어떤 버전이 응답했는지 확인한다. |
| `compose-image-override.yml` | 원래 Compose에 덧붙여 이번 배포 대상 서비스의 `image`만 지정한다. pull 후에는 정확한 이미지 ID로 고정한다. 원본 Compose를 수정하거나 다른 앱의 이미지를 바꾸지 않기 위한 파일이다. 이미지 본체가 들어 있는 파일은 아니다. |

JSON에는 `//`나 `#` 주석을 넣을 수 없으므로 `deployment.json`의 `_descriptions`와
각 헬스체크의 `description`에 한글 설명을 데이터로 함께 저장한다.

### deployment.json 항목

| 항목 | 의미 |
|---|---|
| `started_at`, `finished_at` | 배포 시작·종료 시각 |
| `target` | 배포할 커밋 SHA(`commit_sha`), 이미지 주소(`image_reference`), pull한 이미지 ID(`image_id`) |
| `previous` | 기존 앱의 SHA·이미지 ID·색상·컨테이너 이름·주소 |
| `phase` | 마지막으로 실행한 단계 |
| `result` | 종료 코드, 새 앱 전환 확인 여부, 이전 앱 정리 결과, 실패 이유 |
| `rollback` | 복구 결과와 시작(`started_at`), 기존 앱 응답 복구(`traffic_restored_at`), 새 앱 정리 완료(`completed_at`) 시각 |

복구가 성공해도 원래 배포가 실패했다면 종료 코드는 실패로 유지한다.
Blue가 종료됐지만 강제 종료 가능성이 있으면 경고를 남기고, 종료 확인 자체가 안 되면 실패 처리한다.

### health-checks.json 항목

| 이름 | 확인하는 대상과 목적 |
|---|---|
| `active_app_local` | 배포 전 기존 앱에 직접 요청해 정상 응답과 기존 SHA 확인 |
| `active_app_via_domain` | 배포 전 도메인을 통해 기존 앱의 정상 응답과 기존 SHA 확인 |
| `new_app_local` | 새 앱에 직접 요청해 실행 준비 상태와 배포할 SHA 확인 |
| `new_app_via_domain` | 전환 후 도메인을 통해 새 앱의 정상 응답과 배포한 SHA 확인 |
| `restored_app_via_domain` | 롤백 후 도메인을 통해 기존 앱의 정상 응답과 기존 SHA 확인 |

각 항목에는 한글 설명, 요청 URL, 기대 SHA, 마지막 응답 SHA, HTTP 상태, 성공·실패,
요청 횟수, 확인 시각을 저장한다. HTTP 200만으로 통과하지 않고 SHA도 비교한다.
헬스체크 헤더는 메모리에서 읽으므로 `/tmp`에 헤더 파일을 남기지 않는다.

### nginx/의 파일

| 파일 | 내용과 필요한 이유 |
|---|---|
| `config-before-deploy.conf` | 변경 대상인 `yeobaek-dev.conf`의 배포 전 복사본. 실패 시 이 파일로 복원한다. |
| `config-to-apply.conf` | 새 앱을 가리키도록 주소를 바꾼 설정. 실제로 적용됐다는 의미는 아니며, 적용하려고 준비한 내용이다. |
| `all-configs-before-deploy.conf` | 배포 전 `nginx -T` 출력. `nginx.conf`와 `include`로 읽는 다른 파일들의 내용을 함께 보관해 전체 설정 관계를 확인한다. 복원용 파일은 아니다. |
| `config-check.log` | 배포 전 `nginx -T`가 출력한 설정 검사 메시지. 사전 검사 실패 원인을 확인한다. |
| `workers-before-switch.json` | Nginx 전환 직전 master·worker의 PID, 시작 정보, 로그 파일 식별 정보와 읽기 시작 위치. 어떤 worker가 끝나야 이전 앱을 끌 수 있는지 판단할 기준이다. |
| `worker-drain.log` | 전환 후 기존 worker의 정상 종료를 확인하면서 읽은 Nginx 로그. 이전 앱 종료 판단의 근거를 남긴다. |

예를 들어 `nginx.conf`에 `include /etc/nginx/conf.d/*.conf;`가 있으면 Nginx는 그 디렉터리의
설정 파일들도 읽는다. `all-configs-before-deploy.conf`에는 주 설정 파일뿐 아니라 이 파일들의 내용도
파일 구분과 함께 출력된다. `include` 문장을 제거해서 하나의 실행용 설정으로 바꾸는 기능은 아니다.

### containers/의 파일

`containers/yeobaek-app-blue/`와 `containers/yeobaek-app-green/`으로 대상을 구분한다.

| 파일 | 내용과 필요한 이유 |
|---|---|
| `application.log` | 정리 대상 앱의 최근 로그 최대 2,000줄. 컨테이너를 삭제한 뒤에도 시작·종료 실패 원인을 확인한다. |
| `shutdown.json` | 종료 확인 시 상태, 실행 여부, 종료 코드, OOM 여부와 확인 시각. 강제 종료 가능성을 구분한다. |
| `final-state.json` | 배포 종료 시 Docker가 알려준 컨테이너 상태. 조회할 수 없으면 조회 불가 정보를 기록한다. |
| `inspection-error.log` | 마지막 컨테이너 상태 조회의 오류 출력. 컨테이너 없음과 Docker 조회 오류의 원인을 확인한다. 오류가 없으면 빈 파일일 수 있다. |

### rollback/의 파일

위치는 **이번 배포 기록 폴더 안의 `rollback/`**이다.
예: `/home/ec2-user/yeobaek-dev/deployment/records/<SHA>__<시각>__<구분값>/rollback/`

| 파일 | 내용과 필요한 이유 |
|---|---|
| `nginx-workers-before-restore.json` | 기존 앱으로 설정을 복원하기 직전의 worker 정보. 새 앱으로 전달 중인 요청이 남을 수 있으므로 최초 전환 시점과 별도로 저장한다. |
| `nginx-worker-drain.log` | 복구 시 기존 worker의 종료를 확인한 로그. 복구 대상 앱의 응답을 확인한 뒤에도 새 앱을 바로 종료하지 않기 위한 판단 근거다. |

복구 단계별 시각과 결과는 `deployment.json`의 `rollback` 항목에, 복구 후 헬스체크는
`health-checks.json`의 `restored_app_via_domain` 항목에 기록한다.

## 보관과 임시 파일 정리

- 배포 결과를 기록한 뒤 **현재 기록 1개 + 이전 기록 중 최신 19개**를 남긴다. 성공·실패를 모두 포함한다.
- 폴더 이름의 KST 시각으로 정렬한다. SHA의 문자 순서로 정렬하지 않는다.
- 현재 배포 기록은 항상 보호하며, 이름 형식이 다른 폴더와 심볼릭 링크는 삭제하지 않는다.
- 기록 정리 실패는 경고만 남기고 배포의 성공·실패 결과는 유지한다. 용량 상한은 두지 않는다.
- 설정 교체 중 `/etc/nginx/conf.d/.yeobaek-dev-deploy.tmp`를 사용한다. 설정 경로를 바꾸면 그 설정 파일과 같은 디렉터리에 만든다.
- JSON을 저장할 때는 같은 위치의 `.<파일이름>.tmp`를 완성한 뒤 교체한다.
- 배포 잠금을 잡은 뒤, 그리고 배포 종료 시 우리 스크립트가 쓰는 임시 파일 경로만 정리한다.
  강제 중단으로 남은 파일은 다음 실행에서 다시 정리한다. 링크를 따라가서 다른 디렉터리의 내용을 지우지 않는다.
- Docker 원본 로그·이미지와 수동 백업 폴더는 이 기록 보관 정책의 대상이 아니다.

## 로컬 검증

저장소 루트에서 다음 명령을 실행한다. 테스트는 가짜 Docker·Nginx 명령과 임시 디렉터리를 사용한다.

```bash
bash -n backend/infra/dev/deploy.sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s backend/infra/dev/tests -v
```

실제 서버의 요청 연속성, sudo 실행 권한, worker 종료 로그, 기록 용량은 개발 서버 배포 테스트에서 확인한다.
