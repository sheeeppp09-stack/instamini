# InstaMini v0.4

Android 개인용 Instagram 경량 클라이언트입니다.

## APK 배포

GitHub Actions가 APK를 빌드하고 v0.4 Release에 게시합니다.

- Actions: https://github.com/sheeeppp09-stack/instamini/actions
- Releases: https://github.com/sheeeppp09-stack/instamini/releases
- APK: `InstaMini-v0.4-release.apk`

## 빌드 환경

- JDK 17
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.4
- versionCode: 5
- applicationId: `com.instamini.app`

## 서명 키 설정

앱 업데이트 시 기존 앱 데이터와 로그인 상태를 유지하려면 이전 버전과 동일한 서명 키가 필요합니다.

**JKS 파일과 비밀번호는 저장소에 올리지 않습니다.** GitHub Actions는 Repository Secrets에서 키를 복원합니다.

Settings → Secrets and variables → Actions에 다음 4개 항목이 등록되어 있어야 합니다.

| Secret | 내용 |
|---|---|
| `KEYSTORE_BASE64` | 기존 JKS 파일의 Base64 문자열 |
| `KEYSTORE_PASSWORD` | JKS store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

기존 v0.3 설치본과 동일한 키를 사용해야 업데이트 설치가 가능합니다. 키가 다르면 앱을 삭제하고 재설치해야 할 수 있으며, 앱 데이터와 로그인 정보가 삭제됩니다.

## 알려진 제한

- 백그라운드 DM 알림 및 새 DM 감지 기능은 포함하지 않습니다.
- Instagram 웹사이트 구조 변경 시 일부 기능이 동작하지 않을 수 있습니다.
- Instagram 로그인, DM, 스토리는 실제 기기에서 별도 검증이 필요합니다.
