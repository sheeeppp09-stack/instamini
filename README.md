# InstaMini v0.2

Android 개인용 Instagram 경량 클라이언트입니다.

## APK 배포

GitHub Actions가 빌드 성공 후 버전별 GitHub Release를 자동 생성하고 APK를 첨부합니다.

- Actions: 저장소의 Actions 탭 → Build APK
- Release: https://github.com/sheeeppp09-stack/instamini/releases
- APK 파일명: `InstaMini-v0.2-release.apk`

## 빌드 환경

- JDK 17
- Gradle 8.9
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.2
- versionCode: 3

## 서명키 설정은 필수

앱 업데이트 시 로그인 상태와 앱 데이터를 유지하려면 매번 같은 개인 서명키로 APK를 서명해야 합니다.

**`app/instamini.jks`는 공개 저장소에 절대 커밋하지 않습니다.** 저장소에 비밀값으로 등록하고 GitHub Actions가 빌드 중에만 복원합니다.

GitHub 저장소에서 **Settings → Secrets and variables → Actions → New repository secret**으로 다음 4개를 등록해야 합니다.

| Secret 이름 | 값 |
|---|---|
| `KEYSTORE_BASE64` | `app/instamini.jks` 파일의 Base64 문자열 |
| `KEYSTORE_PASSWORD` | JKS의 store password |
| `KEY_ALIAS` | JKS의 key alias |
| `KEY_PASSWORD` | JKS의 key password |

Windows PowerShell에서 ZIP을 풀어 `InstaMini` 폴더 안으로 이동한 뒤 다음 명령으로 Base64 값을 클립보드에 복사합니다.

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\app\instamini.jks")) | Set-Clipboard
```

이 값을 `KEYSTORE_BASE64` Secret에 붙여넣습니다. 나머지 세 값은 ZIP에 들어 있던 기존 `app/build.gradle.kts`의 signing 설정과 동일하게 등록합니다. 비밀번호를 소스 코드나 채팅에 적지 마세요.

Secret 등록 후 **Actions → Build APK → Run workflow**를 실행하면 APK 빌드와 `v0.2` Release 게시가 진행됩니다. 이후 `appVersion`과 `versionCode`를 올리면 같은 키로 후속 버전을 자동 배포합니다.

## 설치 주의

v0.1은 GitHub Actions 러너의 임시 디버그 키로 서명됐고 v0.2는 고정 개인키로 서명하므로, 최초 v0.2 설치 때 기존 v0.1을 제거해야 할 수 있습니다. 앱을 제거하면 로그인 쿠키와 앱 내부 데이터가 삭제됩니다. v0.2부터 동일한 signing secrets를 유지하면 이후 업데이트는 덮어쓰기로 설치할 수 있습니다.

## 알려진 제한

- 사진/파일 첨부 전송 미지원
- Instagram 웹사이트 DOM 변경 시 숨김 규칙이 동작하지 않을 수 있음
- Instagram 로그인, DM, 스토리는 실제 기기에서 별도 검증 필요
