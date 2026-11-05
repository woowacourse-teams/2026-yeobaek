# 여백 초대 웹 페이지

운영 주소: https://yeobaek-invite.vercel.app/

공유 예시: https://yeobaek-invite.vercel.app/?code=ABC123

## 동작
1. 앱은 HTTPS 초대 주소를 시스템 공유창으로 전달한다.
2. 브라우저가 이 정적 페이지를 연다.
3. 영문 대문자·숫자 6자리 코드가 정확히 하나 있으면 초대코드와 앱 열기 링크를 표시한다.
4. 모바일 기기로 추정되는 환경에서, 페이지가 보일 때 커스텀 스킴 실행을 한 번 시도한다.
5. 브라우저가 자동 실행을 막아도 '여백 앱에서 열기'를 눌러 다시 요청할 수 있다.
6. 설치가 필요하면 스토어를 사용하고, 설치 후 초대 링크를 다시 연다.

앱 실행 주소는 `yeobaek://invite?code=ABC123`이다.
공유 주소를 앱이 직접 받는 App Links·Universal Links는 사용하지 않는다.
웹에서 앱 설치 여부를 알아내거나 참여 API를 호출하지 않는다.
User-Agent는 PC에서 불필요한 자동 실행을 피하는 힌트로만 사용한다.
자동 실행 성공 여부나 설치 여부를 타이머로 추측해 스토어로 강제 이동하지 않는다.

## 화면과 미리보기
- 확정된 Figma 2072:38 / 2072:72를 HTML/CSS로 구현했다.
- 코드 없음: 다운로드 안내. 잘못되거나 중복된 코드: 링크 확인 안내.
- 모임명·책 표지 조회 없이 로고와 고정 OG 문구로 공통 미리보기를 제공한다.
- OG 정보는 최초 HTML에 있다. `og:url`을 고정 루트 주소로 지정해 초대코드를 잃지 않도록 생략했다.
- 실제 미리보기 표시는 메신저의 스크랩·캐시·사용자 설정에 영향을 받는다.

## 로컬 실행과 검사
```sh
cd web
pnpm install --ignore-scripts
pnpm check
pnpm test
python3 -m http.server 4173 --bind 127.0.0.1
```

http://127.0.0.1:4173/?code=ABC123 에서 확인한다.
브라우저는 코드가 가리키는 모임의 존재 여부를 조회하지 않는다.
형식이 맞는 테스트 코드로 페이지와 앱 실행만 확인하고, 실제 참여 버튼은 누르지 않는다.

## 배포
Vercel 프로젝트: `jo-eungyeongs-projects/yeobaek-invite`.
기존 참고 사이트 `yeobaek-download-link.vercel.app`는 현재 계정의 프로젝트가 아니어서 별도로 생성했다.
프로젝트 루트는 이 `web/` 디렉터리이며 빌드 단계와 런타임 API는 없다.

```sh
cd web
vercel login
vercel link --yes --scope jo-eungyeongs-projects --project yeobaek-invite
vercel deploy --prod --yes --scope jo-eungyeongs-projects
```

`.vercel/`, `.env*`, 개발 의존성·테스트는 배포에서 제외한다.
CLI가 생성하는 인증 환경 파일이나 토큰을 커밋하지 않는다.
도메인을 바꾸면 앱의 `InviteLink.SHARE_PREFIX`, 웹 HTML의 `og:image`, 이 문서를 함께 수정한다.
앱의 공유 주소 변경은 새 앱 빌드가 설치되어야 적용된다.

## 에셋
로고, 복사·스토어 아이콘, 배경 장식은 Figma의 개별 에셋을 로컬에 저장했다.
로고는 같은 이미지의 화면용 176px / 공유 미리보기용 512px 크기로 최적화했다.
Noto Sans KR은 Google Fonts의 공식 배포본을 화면 문구·영숫자로 서브셋한 파일이다.
문구에 새 글자를 추가하면 두 폰트 파일도 갱신한다. 라이선스는 assets/OFL-NotoSansKR.txt.
외부 글꼴·분석 스크립트·카카오 SDK를 로드하지 않는다.

## 검증 범위
정상·누락·잘못된 코드, 중복 파라미터, URL 주입, 모바일 판별,
백그라운드 대기와 앱 복귀 후 자동 재실행 방지를 테스트한다.
모바일 390×844, 320×720, PC 1280×960 화면과 에셋 로딩을 브라우저에서 확인한다.
iPhone Safari에서 자동 앱 열기 확인창과 앱 실행을 확인한다.
카카오톡 실제 메시지 전송과 초대코드를 사용한 실서버 참여는 검증에 포함하지 않는다.

웹 테스트 8개와 엄격한 JavaScript 타입 검사, 앱 공통 테스트 75개·ktlint, Android Debug APK 및 iOS 시뮬레이터 공통 코드 빌드가 통과했다.
운영 HTML·미리보기 이미지 200 응답과 비공개 환경 파일 경로 404를 확인했다.
Android Chrome의 최초 실행 설정은 완료하지 않아 해당 브라우저의 실제 앱 전환은 검증하지 못했다.
