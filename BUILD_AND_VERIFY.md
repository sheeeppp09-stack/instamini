# InstaMini 빌드 및 검증 (v0.9)

## 빌드
```
./gradlew clean assembleRelease
```
결과: `app/build/outputs/apk/release/InstaMini-v0.9-release.apk` (버전은 `app/build.gradle.kts`의 `appVersion`, `versionCode`는 10)

## 서명 (중요)
- 서명 정보는 소스에 없다. 환경 변수 `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, 키 파일(`KEYSTORE_FILE`, 기본 `app/instamini.jks`)이 있을 때만 release에 서명된다.
- GitHub Actions(`.github/workflows/build-apk.yml`)는 저장소 Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`로 서명한다.
- 이전 버전과 같은 키를 써야 덮어쓰기 설치가 된다. 키가 다르면 "패키지가 기존 패키지와 충돌" 오류가 나고, 환경 변수가 없으면 서명되지 않은 APK라 설치되지 않는다.

## 독립 실행 / DM 알림 없음
- v0.6부터 알림 기능(공식 Instagram 앱 알림 미러링)을 제거했다. 공식 Instagram 앱 없이 독립 실행된다.
- 알림 접근 권한과 알림 표시 권한을 요청하지 않는다. 선언 권한은 `INTERNET`뿐이다.
- **DM 백그라운드 알림은 지원하지 않는다.** 공식 앱 없이 새 DM을 안정적으로 감지할 공식 방법이 없다. 앱을 열어야 새 메시지를 볼 수 있다.

## 측정 명령 (기기 USB 디버깅 연결 후)
- APK 용량: `ls -l app/build/outputs/apk/release/*.apk`
- 메모리: `adb shell dumpsys meminfo | grep -iE "instamini|webview|sandboxed"` (WebView 렌더러 PSS 포함)
- CPU: `adb shell top -b -n 1 | grep -iE "instamini|sandboxed|webview"`

## 수동 검증 체크리스트
1. 공식 Instagram 앱이 없는 기기에서 설치·실행되고, 알림 관련 권한 요청이 나타나지 않는지
2. 로그인 → 앱 완전 종료 → 재실행 → 로그인 유지
3. 시작 시 Instagram 로고 화면 없이 DM 목록이 열리는지
4. DM ↔ Stories 스와이프/탭 전환이 로고·로딩 없이 즉시 되는지
5. 스토리 화면에서 "추천 게시물", 로딩 스피너, 하단 Instagram 메뉴가 보이지 않고 피드가 계속 로딩되지 않는지
6. DM 목록 맨 위 헤더 줄이 숨겨지는지
7. 친구가 보낸 릴스를 볼 수 있고 아래로 넘어가지 않는지
8. 당겨서 새로고침, DM 사진 첨부, 캐시 전부 삭제 후 로그인 유지, 다크/라이트
9. 설정 화면에 화면 모드와 캐시 항목만 있고 빈 공간이 없는지
10. 스와이프를 DM 목록/Stories 홈에서 각 10회, 세로 스크롤·가로 스크롤(스토리 트레이)과 섞어 시도, 전환 20회 반복 후 로고·빈 화면 없는지
11. 이전 버전 위에 덮어쓰기 설치가 되는지(같은 서명 키)

## v0.9 변경 (기기 검증 전)
원인(코드 분석 기반 추정, 기기에서 재현·측정하지 못함)과 수정:
- A. 스와이프가 가끔만 됨: 이전 판정은 손을 뗄 때만, 고정 비율(35%/15%)과 시작 y 30% 제외, 한 번의 판정으로 끝났고, 멀티터치·취소 후 상태가 남을 수 있었다. 전환 진행 중 재스와이프도 막지 않았다.
  → 터치 슬롭 이후 방향 우세(가로 ≥ 세로×1.5)를 판정하고, 이동 중(MOVE)에도 거리(너비 30%) 또는 속도(최소 플링 속도×6, 12% 이상)를 넘으면 확정. 확정 시 WebView에 ACTION_CANCEL을 보내고 나머지 이벤트를 소비(제스처당 1회 전환). DOWN마다 상태 초기화, 포인터 ID 추적, 멀티터치·CANCEL 시 초기화, 전환 후 450ms 동안 새 전환 잠금. 허용 화면(DM 목록→오른쪽, Stories 홈→왼쪽) 밖에서는 터치를 전혀 가로채지 않는다.
- B. 피드가 Stories 아래에 가끔 로딩됨: 게시물을 `display:none`으로 숨기면 더 불러오기 감시 요소가 화면 안에 남아 계속 페이지를 불러올 수 있다(추정).
  → 게시물·로딩 스피너·"추천 게시물" 뒤의 요소(하단 메뉴 포함)를 모두 숨겨 감시 요소를 화면 밖으로 내리고, 숨김은 Stories 상세 화면에서도 유지. MutationObserver는 홈/DM 목록에서만 1개 등록하고 그 외 화면에서는 해제. 스크립트는 첫 화면 표시(`onPageCommitVisible`)에도 주입. 릴스 스크롤 잠금 리스너는 대화방/릴스 경로에서만 등록.
- C. DM↔Stories 연결: 주소 변경(pushState/popstate/Navigation API)을 즉시 감지해 클래스·감시 상태를 갱신. 전환 후 900ms 뒤 실제 경로를 확인해 목적지가 아니면 전체 로드로 대체. 시작 시 홈→DM 링크 클릭 방식과 기록 기반 이동(`goBackOrForward`)은 유지.
- 디버그 빌드 전용 로그(Logcat 태그 `InstaMini`, 페이지 로그는 `[im]` 접두사): 터치(DOWN 위치 비율·방향 판정·거리·속도·결정), 화면 종류(home/inbox/thread/story/login/other), 전환 성공/실패, Observer 등록/해제, 피드 숨김 결과, 네트워크 요청 경로(숫자 ID 가림, 본문·쿠키·쿼리 미기록). 배포용에서는 로그와 원격 디버깅 모두 꺼진다.
- 검증하지 못한 것: Kotlin 컴파일, 기기 동작(스와이프 10회·전환 20회·취소 케이스·피드 늦은 로딩·재진입 시 Observer 중복·콜드 재시작), 실제 Instagram DOM에서의 숨김. 숨김 JS의 논리만 jsdom에서 확인했다(Observer 1개·재등록, 클래스 전환, 표시/숨김 대상, 릴스 잠금 on/off).
- 디버그 빌드로 `adb logcat -s InstaMini` 를 보면 스와이프 판정과 피드 관련 요청(`net ...`)을 확인할 수 있다. 피드 요청을 막는 처리는 근거 없이 넣지 않았다(로그로 확인 후 결정).

## v0.7 성능 변경 (측정 전)
- 홈의 "추천 게시물" 탐색이 문서 전체 텍스트를 반복해서 훑던 것을 결과 캐시 + 2초 간격 제한으로 변경(CPU 감소 목적).
- 스와이프 전환: 35% 이상 끌어야만 반응하던 것을, 빠르게 쓸어도(1200px/s 이상, 15% 이상 이동) 반응하도록 변경. 터치는 여전히 WebView에 그대로 전달한다.
- 이 변경들의 효과(시간, 메모리, CPU)는 기기에서 측정하지 못했다. 측정은 아래 명령과 Chrome `chrome://inspect`의 Performance 탭으로 한다.

## 성능 측정용 디버그 빌드 (v0.8)
- 디버그 빌드에서만 WebView 원격 디버깅이 켜진다(`chrome://inspect`). 배포용 release APK에서는 꺼져 있다.
- `./gradlew assembleDebug` → `app/build/outputs/apk/debug/InstaMini-v0.9-debug.apk`. GitHub Actions는 `InstaMini-debug-apk`로도 올린다.
- 디버그 APK는 서명 키가 달라 배포용 앱 위에 설치할 수 없다. 배포용을 지우고(로그인 풀림) 설치해야 하고, 측정 후 다시 배포용을 설치하려면 디버그 앱을 지워야 한다.
- 측정 절차: 폰 USB 디버깅 허용 → PC Chrome `chrome://inspect#devices` → InstaMini 페이지 `inspect` → Performance 탭 녹화.

## 알려진 한계
- Stories 홈 첫 표시 속도는 Instagram 홈 페이지(피드 포함)를 통째로 불러오는 시간에 좌우되며, 앱에서 줄이지 못했다.
- Instagram 화면 구조를 직접 확인하지 못해 숨김 처리(추천 게시물, 하단 메뉴, DM 헤더)는 추정 기반이다. 안 숨겨지거나 다른 것이 숨겨지면 화면 캡처가 필요하다.
- DM 헤더를 숨기면서 새 메시지 작성 버튼과 뒤로가기 버튼도 사라진다(요청 사항).
- 뒤로가기는 Stories 홈과 DM 목록에서 앱을 종료한다. 대화방에서는 목록으로 돌아간다.
- 스토리·DM 전환이 즉시 되려면 둘이 같은 페이지의 기록에 있어야 한다. 기록에 없으면 링크 클릭, 그것도 안 되면 전체 로드(이때는 cover로 로고 화면만 가림).
- 홈에서 "추천 게시물" 블록을 문구("추천 게시물"/"Suggested Posts")로 찾는다. 다른 언어에서는 스피너·article 숨김만 동작한다.
- 숨김 스크립트는 홈/DM 목록에서만 화면 변화마다(0.25초 간격 제한) 실행된다. 네트워크 단계의 피드 요청은 막지 않으므로 데이터 사용은 줄지 않는다.
- 카메라 촬영 첨부 미지원, 다크 모드는 Instagram이 테마를 따라가는 경우에만 웹 화면까지 적용
- 테마 변경 시 화면이 한 번 다시 로드됨(로그인 유지)
