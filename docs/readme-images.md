# README 이미지 출처

README 이미지는 GitHub 첨부 URL로 표시하며 저장소에 이미지 파일을 포함하지 않습니다.

첨부 보관 위치: [README 이미지 보관 이슈 #286](https://github.com/woowacourse-teams/2026-yeobaek/issues/286)

| 이미지 | 출처 |
| --- | --- |
| [hero-banner-clean.png](https://github.com/user-attachments/assets/48eb2fb4-2ed9-45b4-88e5-5ff54be65425) | 실제 앱 화면을 참조해 생성하고 사용자가 선택한 홍보용 목업 배너 |
| [download-google-play.png](https://github.com/user-attachments/assets/cdd4b416-0461-4bf9-915f-cec8eed0e1e4) | 직접 제작한 스토어 다운로드 버튼 |
| [download-app-store.png](https://github.com/user-attachments/assets/b7ad1fa7-2ea9-406e-b871-2ce1eae788ad) | 직접 제작한 스토어 다운로드 버튼 |
| [project-architecture.png](https://github.com/user-attachments/assets/ba9a185e-b563-4c97-885c-38f0706f45a8) | 모바일·백엔드·인프라 구성을 표현한 서비스 아키텍처 |
| [cicd-architecture.png](https://github.com/user-attachments/assets/dce969b0-f900-45fd-b916-7d077ba49cce) | 백엔드 GitHub Actions → Docker Hub → EC2 배포 흐름 |
| [team-noeyhoj.png](https://github.com/user-attachments/assets/4022d80f-59e9-4031-b21f-3ee6f8596aec) | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |
| [team-eungyeong12.png](https://github.com/user-attachments/assets/142b7298-7a2f-4b3b-bdb3-15c521c7a04c) | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |
| [team-kjoon418.png](https://github.com/user-attachments/assets/3867323f-7da2-459c-8f71-5c7ba32cbf5e) | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |
| [team-alstj2384.png](https://github.com/user-attachments/assets/273fa367-a35a-425e-9ed5-785e3b0a84fa) | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |
| [team-Yeji-Kim-Erica.png](https://github.com/user-attachments/assets/7607f5ba-6c8b-4e82-a45d-17347983cf7e) | 사용자에게 확인한 팀원 계정의 공개 GitHub 프로필 사진 |

서비스 아키텍처는 `backend/docs/배포_가이드.md`, `backend/infra/prod/docker-compose.prod.yml`,
`backend/infra/dev/docker-compose.yml`, `backend/infra/dev/otel-collector.yaml`과 모바일 플랫폼 연동을 기준으로 합니다.
CI/CD는 `.github/workflows/android-ci.yml`, `backend-prod.yml`, `backend-dev.yml`을 기준으로 합니다.
운영 MySQL은 EC2 내부 컨테이너이며, OpenTelemetry·Better Stack은 개발 환경 구성으로 구분합니다.

팀원 닉네임과 담당 파트는 사용자가 제공한 매핑입니다.

## 이미지에 사용한 공개 기술 로고

로고는 아키텍처와 다운로드 버튼에 내장되어 있으며, 출처는 아래와 같습니다.

- [android.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/android/android-original.svg)
- [apple.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/apple/apple-original.svg)
- [nginx.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/nginx/nginx-original.svg)
- [spring.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/spring/spring-original.svg)
- [mysql.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/mysql/mysql-original.svg)
- [docker.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/docker/docker-original.svg)
- [github.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/github/github-original.svg)
- [github-actions.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/githubactions/githubactions-original.svg)
- [firebase.svg](https://raw.githubusercontent.com/devicons/devicon/master/icons/firebase/firebase-original.svg)
- [posthog.svg](https://raw.githubusercontent.com/simple-icons/simple-icons/develop/icons/posthog.svg)
- [otel.svg](https://raw.githubusercontent.com/simple-icons/simple-icons/develop/icons/opentelemetry.svg)
- [better-stack.svg](https://raw.githubusercontent.com/simple-icons/simple-icons/develop/icons/betterstack.svg)
- [ec2.png](https://raw.githubusercontent.com/awslabs/aws-icons-for-plantuml/main/dist/Compute/EC2.png)
- [s3.png](https://raw.githubusercontent.com/awslabs/aws-icons-for-plantuml/main/dist/Storage/SimpleStorageService.png)
