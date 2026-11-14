# InstaMini v0.9

Instagram 웹(WebView)을 감싼 초경량 Android 앱. Kotlin, 단일 Activity, 단일 WebView, XML 레이아웃, 외부 라이브러리 없음. 패키지 `com.instamini.app`, minSdk 26.

## 기능
- Instagram 공식 웹 로그인과 로그인 유지(쿠키, Local/DOM Storage)
- DM, Stories 탭 전환(탭 또는 좌우 스와이프. 터치 슬롭·방향 우세·거리/속도로 판정), 설정 탭
- Reels·Explore 차단, 친구가 보낸 릴스는 열람만 가능(스크롤 잠금)
- 추천 게시물·불필요한 피드 요소 숨김
- 당겨서 새로고침, DM 사진/파일 첨부
- 라이트/다크 모드, 임시 캐시 삭제(로그인 데이터는 유지)

## 독립 실행
공식 Instagram 앱 없이 동작하며 알림 관련 권한을 요청하지 않는다(선언 권한: `INTERNET`).
**DM 백그라운드 알림은 지원하지 않는다.** v0.5의 알림 미러링 기능은 v0.6에서 제거했다.

빌드와 검증 방법은 `BUILD_AND_VERIFY.md` 참고.
