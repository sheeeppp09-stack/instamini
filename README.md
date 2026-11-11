# InstaMini

Android 개인용 Instagram 경량 클라이언트 프로젝트입니다.

## 휴대폰에서 APK 받기

1. 저장소의 Actions 탭을 엽니다.
2. Build APK 워크플로를 선택합니다.
3. Run workflow를 누릅니다.
4. 빌드가 끝나면 실행 결과의 Artifacts에서 InstaMini-apk를 다운로드합니다.
5. 내려받은 ZIP을 풀고 app-release.apk를 설치합니다.

main 브랜치에 변경 사항을 올리면 자동 빌드됩니다. 수동 실행도 지원합니다.

## 빌드 환경

- JDK 17
- Gradle 8.9
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)

GitHub Actions가 빌드 환경을 준비하므로 휴대폰에서 별도 Android SDK 설치는 필요하지 않습니다.

## 서명

현재 Release APK는 Android 디버그 키로 서명됩니다. 개인 기기에 설치해 테스트하기 위한 설정이며, Google Play 배포용 서명 구성은 아닙니다.
