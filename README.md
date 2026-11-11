# InstaMini v0.1

Android 개인용 Instagram 경량 클라이언트입니다.

## 최신 APK 빌드 및 다운로드

1. 저장소의 **Actions** 탭을 엽니다.
2. **Build APK** 워크플로를 선택합니다.
3. **Run workflow**를 실행합니다. `main`에 push해도 자동 빌드됩니다.
4. 성공한 실행에서 **Artifacts → InstaMini-apk**를 다운로드합니다.
5. ZIP을 풀고 `InstaMini-v0.1-release.apk`를 설치합니다.

## 현재 빌드 설정

- JDK 17
- Gradle 8.9
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.1 / versionCode: 2
- 외부 AndroidX, 광고, 분석 SDK 미사용

## v0.1 변경점

- 홈 화면에서 피드 동영상 자동재생을 중지
- 단일 릴스 화면에서 세로 스와이프, 마우스 휠, 방향키를 통한 다음 릴스 이동 차단
- Reels 피드 및 Explore 링크 숨김은 유지
- APK 파일명에 버전 표시

## 설치 및 서명 주의

Release APK는 GitHub Actions 러너의 Android 디버그 키로 서명됩니다. 개인 설치용이며 Google Play 배포용이 아닙니다. 빌드마다 디버그 서명 키가 바뀔 수 있으므로 기존 InstaMini 설치본 위에 업데이트가 거부되면 기존 앱을 제거한 뒤 새 APK를 설치해야 합니다. 앱을 제거하면 로그인 쿠키와 앱 내부 데이터가 삭제될 수 있습니다.

## 알려진 제한

- 사진/파일 첨부 전송 미지원
- Instagram 웹사이트의 DOM 구조가 바뀌면 숨김 규칙이 동작하지 않을 수 있음
- Instagram 로그인, 메시지 전송, 스토리 재생 등은 실제 기기에서 별도 검증 필요
