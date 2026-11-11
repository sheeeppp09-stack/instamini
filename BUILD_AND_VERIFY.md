# InstaMini 빌드 및 검증

## GitHub Actions 빌드

저장소의 Actions 탭에서 Build APK를 선택하고 Run workflow를 실행합니다.
완료된 실행의 Artifacts에서 InstaMini-apk를 내려받으면 app-release.apk를 얻을 수 있습니다.

GitHub Actions는 JDK 17, Gradle 8.9, Android SDK 34를 사용합니다.

## APK 결과 경로

app/build/outputs/apk/release/app-release.apk

## 기기 검증 체크리스트

1. 로그인 후 앱을 완전히 종료하고 다시 실행해 로그인 상태가 유지되는지 확인
2. DM 목록 열기 및 메시지 전송
3. Stories 탭 열람 및 답장
4. Reels / Explore 링크 숨김과 직접 주소 접근 차단 확인
5. 설정에서 임시 캐시를 삭제한 뒤 로그인 상태 유지 확인
6. 화면 회전 및 설정 왕복 시 페이지가 불필요하게 재로딩되지 않는지 확인

## 알려진 한계

- 사진/파일 첨부 전송 미지원(파일 선택기 미구현)
- 피드 게시물은 article 요소를 CSS로 숨기는 방식이라 Instagram DOM 변경 시 깨질 수 있음
- 공유받은 게시물(/p/)도 본문이 숨겨질 수 있음
- Facebook 로그인 버튼은 instagram.com 외 도메인이라 외부 브라우저로 열림(아이디/비밀번호 로그인 사용)
- Release APK는 디버그 키로 서명됨(개인 설치용)
