# InstaMini v0.3

Android 개인용 Instagram 경량 클라이언트입니다.

## APK 배포

GitHub Actions가 main 브랜치 변경 시 APK를 빌드하고, 성공하면 버전별 GitHub Release에 자동 게시합니다.

- Actions: 저장소의 Actions 탭 → Build APK
- Releases: https://github.com/sheeeppp09-stack/instamini/releases
- APK 파일명: `InstaMini-v0.3-release.apk`

## 빌드 환경

- JDK 17
- Gradle 8.9
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.3
- versionCode: 4

## 서명 키 설정

앱 업데이트 시 로그인 상태와 앱 데이터를 유지하려면 매번 같은 개인 서명 키로 APK를 서명해야 합니다.

**`app/instamini.jks` 및 비밀번호는 공개 저장소에 커밋하지 않습니다.** GitHub Actions는 저장소의 비공개 Repository Secrets에서 키를 복원합니다.

GitHub 저장소의 **Settings → Secrets and variables → Actions**에 다음 4개 항목이 등록되어 있어야 합니다.

| Secret 이름 | 값 |
|---|---|
| `KEYSTORE_BASE64` | 기존 JKS 파일의 Base64 문자열 |
| `KEYSTORE_PASSWORD` | JKS의 store password |
| `KEY_ALIAS` | JKS의 key alias |
| `KEY_PASSWORD` | JKS의 key password |

기존 v0.2와 동일한 서명 키를 유지해야 업데이트 설치가 가능합니다. 서명 키를 변경하면 기존 앱을 제거해야 할 수 있으며, 이 경우 앱 데이터와 로그인 정보가 삭제됩니다.

## 알려진 제한

- 사진/파일 첨부 전송 미지원
- Instagram 웹사이트 구조 변경 시 일부 기능이 동작하지 않을 수 있음
- Instagram 로그인, DM, 스토리는 실제 기기에서 별도 검증 필요
