# InstaMini v0.7 빌드 및 검증

## 버전 정보
- versionName: `0.7`
- versionCode: `8`
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

결과 APK는 `app/build/outputs/apk/release/`에 생성됩니다. 배포 Workflow는 실제 생성된 APK를 `InstaMini-v0.7-release.apk`로 정규화한 뒤 서명을 검증하고 GitHub Release에 게시합니다.

## 서명 키 보안
- JKS 파일과 비밀번호는 저장소에 포함하지 않습니다.
- GitHub Repository Secrets에 다음 네 항목이 있어야 합니다.
  - `KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- Release 빌드에는 기존 버전과 동일한 개인 서명 키를 사용해야 합니다. 키가 다르면 Android에서 기존 앱 위에 업데이트 설치가 거부됩니다.
- Actions는 필요한 Secrets가 누락되면 빌드를 중단합니다. unsigned APK를 정상 배포물로 취급하지 않습니다.

## v0.7 변경 사항
- 추천 게시물 텍스트 노드를 찾는 DOM 전체 탐색 결과를 캐시하고, 재탐색을 2초 간격으로 제한
- `VelocityTracker`로 스와이프 속도를 감지해 빠른 좌우 제스처의 탭 전환 조건을 추가
- 기존 단일 WebView, 로그인 쿠키 및 페이지 상태 유지 로직 보존
- 비공식 DM API 폴링이나 백그라운드 DM 감시를 추가하지 않음

## 성능 측정 상태
이 ZIP에는 실제 Android 기기에서 측정한 전후 로딩 시간, 프레임 시간, CPU 또는 메모리 결과가 포함되어 있지 않습니다. 코드 변경을 성능 개선의 확정된 수치로 표현하지 않습니다.

기기 USB 디버깅 연결 후:
- 메모리: `adb shell dumpsys meminfo | grep -iE "instamini|webview|sandboxed"`
- CPU: `adb shell top -b -n 1 | grep -iE "instamini|sandboxed|webview"`
- Chrome WebView 원격 디버깅: `chrome://inspect`의 Performance 탭

## 수동 검증 체크리스트
1. 공식 Instagram 앱이 설치되지 않은 기기에서 InstaMini 설치 및 실행
2. 로그인 후 앱을 완전히 종료하고 재실행해 로그인 유지 확인
3. 시작 시 DM 목록이 열리는지 확인
4. DM ↔ Stories 탭 및 좌우 스와이프 전환 확인
5. 빠른 스와이프와 느린 드래그 모두에서 의도한 탭으로 전환되는지 확인
6. Stories 화면에서 스토리 트레이의 가로 스크롤과 탭 전환 제스처가 충돌하지 않는지 확인
7. Stories 최초 진입·재진입 시간을 같은 네트워크 환경에서 각각 5회 측정
8. Reels / Explore 접근 제한 확인
9. 추천 게시물, 로딩 스피너, 불필요한 홈 피드 요소가 숨겨지는지 확인
10. DM 파일 첨부, 당겨서 새로고침, 캐시 삭제 후 로그인 유지 확인
11. 라이트/다크 모드 전환 확인
12. 기존 v0.6 위에 덮어쓰기 설치가 되는지 확인(서명 키가 같아야 함)

## 알려진 한계
- Stories 홈 첫 표시 속도는 Instagram 홈 페이지의 다운로드·실행·렌더링 시간에 영향을 받습니다.
- Instagram DOM이 변경되면 추천 게시물 탐지 및 숨김 처리가 동작하지 않을 수 있습니다.
- 스와이프 전환은 화면 아래쪽에서 시작한 좌우 제스처에 한정되며, Instagram Stories 내부 스와이프와 충돌하는지 실제 기기 확인이 필요합니다.
- Instagram 계정 및 서버 측 변경 사항에 따라 로그인, DM, Stories 동작은 실제 기기 검증이 필요합니다.
