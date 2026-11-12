# 블루·그린 배포와 기록

`deploy.sh`는 새 앱을 준비한 뒤 Nginx의 전달 대상을 전환하고, 기존 worker가 요청을 마무리한 뒤 이전 앱을 정리한다.
전환 확인 전에 실패하면 기존 앱으로 복구한다. 전환 확인 후 이전 앱 정리에 실패하면 새 앱을 유지하고 실패 결과를 남긴다.
배포 스크립트 성공 여부와 사용자 요청이 끊겼는지는 별개다. 무중단 여부는 배포 중 외부 요청 측정으로 확인한다.

## 실행 파일과 준비 조건

- 공용 [deploy.sh](deploy.sh)와 `deploy_support.py`는 `backend/infra/deploy/`에 함께 둔다. Python 외부 패키지는 필요하지 않다.
- 인자 없이 `bash deploy.sh`를 실행하면 읽기 전용 사전 확인만 한다.
- 실제 배포는 `bash deploy.sh <이미지주소:커밋SHA> <40자리커밋SHA>`로 실행한다.
- 서버 배포 디렉터리의 `docker-compose.yml`과 `.env`를 사용한다. 개발·운영 값은 [배포 가이드](../../docs/배포_가이드.md#구성-개요)를 참고한다.
- 현재 앱도 `/health`에서 `Deployed-Commit-SHA`를 응답해야 복구 버전을 확인할 수 있다.
- 다음 배포 위치에 이전 컨테이너가 남아 있으면, 정지 상태라도 자동으로 덮어쓰지 않고 중단한다.
- Linux의 Python 3.8 이상, Docker Compose, curl, flock, timeout, ps와 스크립트에서 사용하는 Nginx·파일 명령의 비대화식 sudo 실행 권한이 필요하다.

| 역할 | Compose 서비스 | 컨테이너 | 호스트 → 앱 포트 |
|---|---|---|---|
| Blue | `app-blue` | `yeobaek-app-blue` | `127.0.0.1:8080` → `8080` |
| Green | `app-green` | `yeobaek-app-green` | `127.0.0.1:8081` → `8080` |

서버별 값은 `DEPLOY_DIR`, `COMPOSE_PROJECT_NAME`, `NGINX_CONFIG`, `HEALTH_URL`, `DEPLOY_LABEL`로 지정한다.
Nginx 로그 경로는 `NGINX_ERROR_LOG`로 바꿀 수 있다. 기존 worker의 대기 제한은
`WORKER_WAIT_SECONDS`로 지정하며 기본값은 120초다. 초과 시 이전 앱을 유지한다.

## GitHub Actions 연결

개발·운영 워크플로는 테스트와 빌드를 거쳐 SHA 태그 이미지를 발행한다. 서버 러너는 같은 커밋을
체크아웃하고 공용 스크립트에 이미지 주소와 SHA를 전달한다. Docker Hub 로그인은 이미지를
발행하는 빌드 작업에서 수행하며, 서버 러너는 공개 이미지를 받아 배포한다.

스크립트가 실패 코드를 반환하면 배포 단계도 실패한다. 복구 여부는 스크립트가 결정하며,
Actions가 실패했다는 이유만으로 별도의 롤백을 실행하지 않는다.
스크립트의 경고·오류와 결과 요약은 Actions 화면에 표시되고, 배포 기록은 서버의 `deployment/records/`에 남는다.

배포 기록을 생성한 이후에는 종료 시 실행 화면의 요약(Summary) 맨 위에 다음 표시를 남긴다.

| 요약 표시 | 의미 | Actions 기본 실행 결과 |
| --- | --- | --- |
| 🟢 배포 완료 | 경고 없이 완료 | 성공(초록 체크) |
| 🟠 배포 완료 — 경고 확인 필요 | 배포는 완료됐지만 강제 종료 가능성이나 기록 정리 실패 등 확인할 경고가 있음 | 성공(초록 체크) |
| 🔴 배포 실패 — 확인 필요 | 배포 또는 필수 정리 단계 실패. 복구 결과는 별도로 확인 | 실패(빨간 표시) |

요약에는 커밋 SHA, 전환 확인 여부, 마지막 단계, 복구·앱 정리 결과, 실패 이유와 경고 내용을 표시한다.
경고는 Actions의 경고 표시(`::warning::`)에도 남긴다. 주황색은 요약의 표시이며 기본 초록 체크를 바꾸지는 않는다.
기록 생성 전 사전 조건 검사에서 중단되면 실행 실패 표시와 단계 로그를 확인한다.
무중단 여부는 이 표시와 별개로 외부 요청 측정 결과를 확인한다.

## 배포 중 외부 헬스체크

배포 중 실제 서비스 주소가 응답했는지 확인하기 위해 GitHub 호스팅 러너가 공개 HTTPS
`/health`로 요청을 보낸다. 개발은 `develop`, 운영은 `main`의 기존 백엔드 배포 워크플로에서 측정한다.

1. 브랜치에 머지되면 기존 배포 대상 경로 조건에 따라 빌드와 이미지 발행이 실행된다.
2. 이미지가 준비되면 호스팅 러너가 기존 서비스에 요청을 보내고, EC2 배포 잡은 첫 세 건의 결과가
   모두 기록된 뒤 배포를 실행한다. HTTP 오류나 타임아웃도 결과로 기록하고 배포를 진행한다.
3. 1초마다 새 요청을 보내며 각 요청은 최대 3초까지 기다린다. HTTP 200이 아닌 응답, 연결 오류, 타임아웃은 실패로 센다.
4. 배포 잡 종료 후 최대 5분까지 관찰한다. 자동 복구가 배포 잡 안에서 일어나면 같은 실행에 포함된다.
   전체 측정은 30분으로 제한한다. 배포 잡 결과는 성공·실패 여부에 관계없이 측정 요약에 남긴다.
5. 요청별 로그와 요약을 호스팅 러너에서 Better Stack으로 전송한다.

[개발 대시보드](https://telemetry.betterstack.com/team/t615586/dashboards/1181343)와
[운영 대시보드](https://telemetry.betterstack.com/team/t602822/dashboards/1181344)에서
Actions 실행과 시도 번호로 배포별 결과를 확인한다. 요청별 기록은 Better Stack Logs에서
`environment`, `runId`, `runAttempt`로 찾는다.
`deployment_health_summary`는 실패 요청 수, 관측된 중단 구간, 복구 확인 여부, 배포 결과를 보여준다.
`deployment_health_check`는 요청 시각, 상태 코드, 응답 시간, 응답에서 받은 커밋 SHA를 보여준다.
첫 세 건은 `phase=pre_deploy`로 표시되며, 요약의 `preDeployChecks`와 `preDeployFailures`에서
배포 전 요청 수와 실패 수를 확인할 수 있다. 전체 실패 수와 중단 구간에는 배포 전 관측도 포함된다.

끊긴 시간은 첫 실패 요청의 시작부터 다음 성공 요청의 시작까지 추정한다.
마지막 실패 이후 성공을 관측하지 못했다면 요약의 `recoveryUnconfirmed`를 확인한다.
1초 사이에 끝난 장애는 관측되지 않을 수 있다.

환경별 변수와 토큰은 [배포 모니터링 설정](../../docs/배포_가이드.md#배포-모니터링-설정)을 참고한다.

활성화된 환경에서는 배포 전 요청 세 건의 결과 수집을 확인할 수 없으면 배포 잡이 서버 변경 전에 실패한다.
Better Stack이 로그를 수락하지 않거나 측정이 30분 한도에 도달하면 측정 잡에 실패가 표시된다.
배포가 이미 시작된 상태에서 측정 잡이 실패하면 배포 자체의 결과는 해당 배포 잡에서 확인한다.
측정과 배포를 다시 실행할 때는 Actions의 **Re-run all jobs**를 사용한다.

## 서버에 생기는 구조

```text
<배포 디렉터리>/
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
이전 앱이 종료됐지만 강제 종료 가능성이 있으면 경고를 남기고, 종료 확인 자체가 안 되면 실패 처리한다.
스크립트가 SIGTERM을 보낸 뒤 종료가 확인되고 OOM이 없으면 종료 코드 `143`은 일반 로그로만 남긴다.
`137`, OOM 또는 `0`·`143` 이외의 종료 코드는 계속 경고한다. 종료 코드는 `shutdown.json`에도 보관하며,
`143` 자체를 모든 요청이 완료됐다는 근거로 사용하지 않는다.

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
| `config-before-deploy.conf` | `NGINX_CONFIG`로 지정한 파일의 배포 전 복사본. 실패 시 이 파일로 복원한다. |
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
예: `<배포 디렉터리>/deployment/records/<SHA>__<시각>__<구분값>/rollback/`

| 파일 | 내용과 필요한 이유 |
|---|---|
| `nginx-workers-before-restore.json` | 기존 앱으로 설정을 복원하기 직전의 worker 정보. 새 앱으로 전달 중인 요청이 남을 수 있으므로 배포 전환 시점의 기록과 따로 저장한다. |
| `nginx-worker-drain.log` | 복구 시 기존 worker의 종료를 확인한 로그. 복구 대상 앱의 응답을 확인한 뒤에도 새 앱을 바로 종료하지 않기 위한 판단 근거다. |

복구 단계별 시각과 결과는 `deployment.json`의 `rollback` 항목에, 복구 후 헬스체크는
`health-checks.json`의 `restored_app_via_domain` 항목에 기록한다.

## 보관과 임시 파일 정리

- 배포 결과를 기록한 뒤 **현재 기록 1개 + 이전 기록 중 최신 19개**를 남긴다. 성공·실패를 모두 포함한다.
- 폴더 이름의 KST 시각으로 정렬한다. SHA의 문자 순서로 정렬하지 않는다.
- 현재 배포 기록은 항상 보호하며, 이름 형식이 다른 폴더와 심볼릭 링크는 삭제하지 않는다.
- 기록 정리 실패는 경고만 남기고 배포의 성공·실패 결과는 유지한다. 용량 상한은 두지 않는다.
- 설정 교체 중 Nginx 설정과 같은 디렉터리에 `.<Compose 프로젝트>-deploy.tmp`를 만든다.
- JSON을 저장할 때는 같은 위치의 `.<파일이름>.tmp`를 완성한 뒤 교체한다.
- 배포 잠금을 잡은 뒤, 그리고 배포 종료 시 우리 스크립트가 쓰는 임시 파일 경로만 정리한다.
  강제 중단으로 남은 파일은 다음 실행에서 다시 정리한다. 링크를 따라가서 다른 디렉터리의 내용을 지우지 않는다.
- Docker 원본 로그·이미지와 수동 백업 폴더는 이 기록 보관 정책의 대상이 아니다.

## 로컬 검증

저장소 루트에서 다음 명령을 실행한다. 테스트는 가짜 Docker·Nginx 명령과 임시 디렉터리를 사용한다.

```bash
bash -n backend/infra/deploy/deploy.sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s backend/infra/deploy/tests -v
```

실제 서버의 요청 연속성, sudo 실행 권한, worker 종료 로그, 기록 용량은 대상 서버의 배포 테스트에서 확인한다.
