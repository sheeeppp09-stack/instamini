# InstaMini v0.7

Instagram 웹(WebView)을 감싼 초경량 Android 앱입니다. Kotlin 단일 Activity, 단일 WebView, XML 레이아웃을 사용하며 외부 라이브러리를 포함하지 않습니다.

## 앱 정보
- 패키지: `com.instamini.app`
- 최소 지원: Android 8.0 (API 26)
- versionName: `0.7`
- versionCode: `8`

## 주요 기능
- Instagram 웹 로그인 및 로그인 상태 유지
- DM / Stories 탭 전환 및 좌우 스와이프 전환
- Reels·Explore 진입 제한 및 단일 릴스 화면 스크롤 잠금
- 홈 추천 게시물과 불필요한 요소 숨김
- 당겨서 새로고침, DM 파일·사진 첨부
- 라이트/다크 모드
- 임시 캐시 삭제(로그인 쿠키와 Local Storage는 유지)

## v0.7 성능 변경
- 홈의 추천 게시물 텍스트 탐색 결과를 캐시하고, 전체 텍스트 탐색을 최대 2초 간격으로 제한해 DOM 탐색 반복을 줄였습니다.
- 좌우 스와이프에 VelocityTracker를 적용했습니다. 35% 이상 이동하거나 1200px/s 이상 속도로 화면 너비의 15% 이상 빠르게 쓸면 탭 전환을 시도합니다.
- 단일 WebView와 기존 페이지 상태 재사용 방식은 유지합니다.

**주의:** 위 내용은 코드 수준의 최적화입니다. Stories 첫 로딩 시간과 실제 프레임 끊김 개선 여부는 기기 및 네트워크 환경에서 측정해야 합니다. Instagram 웹페이지 자체의 다운로드·렌더링 지연을 앱 코드만으로 완전히 제거할 수는 없습니다.

## 공식 Instagram 앱 의존성
공식 Instagram 앱이 설치되어 있지 않아도 InstaMini 자체 WebView로 동작합니다. Android 알림 접근 권한이나 알림 표시 권한을 요청하지 않습니다. 앱이 선언하는 권한은 `INTERNET`뿐입니다.

**DM 백그라운드 알림은 지원하지 않습니다.** v0.5에서 시험했던 공식 Instagram 앱 알림 미러링 코드는 v0.6에서 제거했습니다. Instagram 웹 계정에 새 DM이 왔는지는 InstaMini를 열어 확인해야 합니다.

## 빌드 및 배포
- GitHub Actions: https://github.com/sheeeppp09-stack/instamini/actions
- GitHub Releases: https://github.com/sheeeppp09-stack/instamini/releases
- JDK 17, Android SDK 34, Gradle 8.9 사용
- Release APK는 기존 개인 서명 키를 GitHub Actions Secrets에서 복원해 빌드합니다.

빌드 및 수동 검증 방법은 `BUILD_AND_VERIFY.md`를 참고하세요.
