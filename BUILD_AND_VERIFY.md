# InstaMini v0.6 빌드 및 검증

## 버전 정보
- versionName: `0.6`
- versionCode: `7`
- applicationId: `com.instamini.app`
- minSdk: 26
- targetSdk: 34
- compileSdk: 34

## 빌드 환경
- JDK 17
- Android SDK 34 (Build Tools 34.0.0)
- Gradle 8.9
- Android Gradle Plugin 8.7.3
- Kotlin Gradle Plugin 2.0.21

로컬 빌드:
```powershell
gradle --no-daemon clean assembleRelease
```

결과 APK는 `app/build/outputs/apk/release/`에 생성됩니다. 배포 Workflow는 실제 생성된 APK를 `InstaMini-v0.6-release.apk`로 정규화한 뒤 서명을 검증하고 GitHub Release에 게시합니다.

## 서명 키 보안
- JKS 파일과 비밀번호는 저장소에 포함하지 않습니다.
- GitHub Repository Secrets에 다음 네 항목이 있어야 합니다.
  - `KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- Release 빌드에는 기존 버전과 동일한 개인 서명 키를 사용해야 합니다. 키가 다르면 Android에서 기존 앱 위에 업데이트 설치가 거부됩니다.
- Actions는 필요한 Secrets가 누락되면 빌드를 중단합니다. unsigned APK를 정상 배포물로 취급하지 않습니다.

## v0.6 변경 사항
- v0.5의 공식 Instagram 앱 알림 미러링 코드 제거
- NotificationListenerService 및 관련 권한·서비스 제거
- 공식 Instagram 앱 설치 여부 확인 코드 제거
- 비공식 DM API 폴링 또는 백그라운드 DM 감시를 추가하지 않음
- Manifest 선언 권한은 `INTERNET`만 유지

**DM 백그라운드 알림은 지원하지 않습니다.** InstaMini를 열어야 새 메시지를 확인할 수 있습니다.

## 수동 검증 체크리스트
1. 공식 Instagram 앱이 설치되지 않은 기기에서 InstaMini 설치 및 실행
2. Android 알림 접근 권한이나 알림 표시 권한을 요청하지 않는지 확인
3. 로그인 후 앱을 완전히 종료하고 재실행해 로그인 유지 확인
4. 시작 시 DM 목록이 열리는지 확인
5. DM ↔ Stories 탭 및 좌우 스와이프 전환 확인
6. Reels / Explore 접근 제한 확인
7. 추천 게시물, 로딩 스피너, 불필요한 홈 피드 요소가 숨겨지는지 확인
8. DM 파일 첨부와 당겨서 새로고침 확인
9. 캐시 삭제 후 로그인 상태가 유지되는지 확인
10. 라이트/다크 모드 전환 확인
11. 기존 v0.5 위에 덮어쓰기 설치가 되는지 확인(서명 키가 같아야 함)

## 알려진 한계
- Instagram 웹사이트 DOM 변경 시 숨김 처리와 화면 전환이 동작하지 않을 수 있습니다.
- 홈 추천 게시물 블록 탐지는 문구와 화면 요소 구조에 의존합니다.
- DM 목록 헤더를 숨기는 방식이므로 해당 헤더 안의 새 메시지 작성·뒤로가기 UI도 함께 숨겨질 수 있습니다.
- Instagram 계정과 서버 측 변경 사항에 따라 로그인, DM, Stories 동작은 실제 기기 검증이 필요합니다.
