# InstaMini

Android 개인용 Instagram 경량 클라이언트 프로젝트입니다.

## APK 빌드

1. 저장소의 **Actions** 탭을 엽니다.
2. **Build APK** 워크플로를 선택합니다.
3. **Run workflow**를 실행합니다.
4. 빌드가 성공하면 실행 결과의 **Artifacts**에서 `InstaMini-apk`를 다운로드합니다.
5. ZIP 압축을 풀고 `app-release.apk`를 Android 기기에 설치합니다.

main 브랜치에 변경 사항이 올라오면 APK 빌드가 자동으로 실행되며, 수동 실행도 지원합니다.

## 소스

원본 Android Studio 프로젝트는 저장소의 `InstaMini.zip`에 포함되어 있습니다. 빌드 워크플로가 압축을 풀고 Gradle Release APK를 생성합니다.

## 서명 관련 주의

현재 프로젝트 설정은 Android 디버그 키로 Release APK를 서명합니다. 개인 기기 설치용이며, Google Play 배포용 서명 구성은 아닙니다.
