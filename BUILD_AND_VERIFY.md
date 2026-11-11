# InstaMini 빌드 및 검증

## 빌드
```
./gradlew assembleRelease
```
결과: `app/build/outputs/apk/release/InstaMini-v0.1-release.apk` (버전은 `app/build.gradle.kts`의 `appVersion`)

GitHub Actions에서는 JDK 17, Gradle 8.9, Android SDK 34로 빌드합니다.

## 측정 명령 (기기 USB 디버깅 연결 후)
- APK 용량: `ls -l app/build/outputs/apk/release/*.apk` / `apkanalyzer apk download-size <apk>`
- 메모리: `adb shell dumpsys meminfo | grep -iE "instamini|webview|sandboxed"`
  - 앱 프로세스와 별도로 WebView 렌더러(`sandboxed_process`) PSS를 같이 봐야 실제 사용량이 나옴
- CPU: `adb shell top -b -n 1 | grep -iE "instamini|sandboxed|webview"`

## 수동 검증 체크리스트
1. 로그인 → 앱 완전 종료(최근 앱에서 제거) → 재실행 → 로그인 유지
2. DM 목록 열림, 메시지 전송
3. Stories 탭 → 스토리 열람 → 답장
4. Reels / Explore 링크 숨김, 주소로 접근해도 차단(DM 복귀)
5. 홈의 피드 영상이 자동재생되지 않는지 확인
6. 단일 릴스 링크는 열리지만 세로 스와이프/휠/방향키로 다음 릴스로 넘어가지 않는지 확인
7. 설정 → 임시 캐시 삭제 → 크기 감소 → DM 탭에서 로그인 유지
8. 회전·설정 왕복 시 WebView가 재생성되지 않는지(페이지 재로딩 없음)

## 알려진 한계
- 사진/파일 첨부 전송 미지원(파일 선택기 미구현)
- 피드 게시물은 `article` 요소를 CSS로 숨기는 방식이라 Instagram DOM 변경 시 깨질 수 있음
- 공유받은 게시물(/p/)도 본문이 숨겨질 수 있음
- Facebook 로그인 버튼은 instagram.com 외 도메인이라 외부 브라우저로 열림(아이디/비밀번호 로그인 사용)
- release는 디버그 키로 서명됨(개인 설치용)
