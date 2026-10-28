# README 이미지 출처

루트 README에서 사용하는 이미지와 편집용 SVG 원본을 보관합니다.
기술 스택 배지는 [Shields.io](https://shields.io/) URL을 직접 사용합니다.

| 파일 | 출처 |
| --- | --- |
| `hero-banner-clean.png` | 실제 앱 화면을 참조해 생성하고 사용자가 선택한 홍보용 목업 배너 |
| `download-google-play.svg`, `.png` | 직접 제작한 Google Play 다운로드 버튼. SVG 원본과 PNG 출력 |
| `download-app-store.svg`, `.png` | 직접 제작한 App Store 다운로드 버튼. Apple 심볼은 공개 기술 로고를 사용 |
| `project-architecture.svg`, `.png` | 모바일·백엔드·인프라 구성을 표현한 서비스 아키텍처 |
| `cicd-architecture.svg`, `.png` | 백엔드 GitHub Actions → Docker Hub → EC2 배포 흐름 |
| `team-*.png` | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |

SVG 원본에는 필요한 로고가 내장되어 있어 별도의 로고 파일이 필요하지 않습니다.
내장 로고의 출처는 [sources.txt](sources.txt)에 기록합니다.

서비스 아키텍처는 `backend/docs/배포_가이드.md`, `backend/infra/prod/docker-compose.prod.yml`,
`backend/infra/dev/docker-compose.yml`, `backend/infra/dev/otel-collector.yaml`과 모바일 플랫폼 연동을 기준으로 합니다.
CI/CD는 `.github/workflows/android-ci.yml`, `backend-prod.yml`, `backend-dev.yml`을 기준으로 합니다.
운영 MySQL은 EC2 내부 컨테이너이며, OpenTelemetry·Better Stack은 개발 환경 구성으로 구분합니다.

팀원 닉네임과 담당 파트는 사용자가 제공한 매핑입니다.
