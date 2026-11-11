# InstaMini v0.3 빌드 및 검증

## 빌드 환경
- JDK 17
- Gradle 8.9
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.3
- versionCode: 4

## 빌드
Android Studio에서 프로젝트를 열고 Gradle의 `app > Tasks > build > assembleRelease`를 실행합니다.

빌드 결과:
`app/build/outputs/apk/release/InstaMini-v0.3-release.apk`

GitHub Actions는 저장소의 비공개 Actions Secrets에서 서명 키를 복원한 뒤 같은 키로 APK를 서명하고, 성공하면 GitHub Release에 자동 게시합니다.

## 서명 키 보안
- `app/instamini.jks`와 비밀번호는 공개 저장소에 커밋하지 않습니다.
- GitHub Actions에는 `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`를 Repository Secrets로 등록합니다.
- v0.2 이후 앱 업데이트 호환성을 위해 같은 서명 키를 계속 사용해야 합니다. 키를 교체하면 기존 앱 위에 업데이트 설치가 되지 않을 수 있습니다.

## 수동 검증 체크리스트
1. 로그인 후 앱을 완전히 종료하고 재실행해 로그인 유지 확인
2. DM 목록 열림 및 메시지 전송 확인
3. Stories 탭에서 스토리 열람 확인
4. Reels / Explore 접근 차단 확인
5. 설정에서 임시 캐시 삭제 후 로그인 유지 확인
6. 라이트/다크 모드 변경 확인

## 알려진 한계
- 알림(내용 표시, 알림창에서 바로 답장) 미구현
- DM 첨부는 파일 선택기(갤러리/파일)만 지원하며 카메라 촬영 첨부는 미지원
- Instagram DOM 변경에 따라 숨김 규칙이 동작하지 않을 수 있음
- Facebook 로그인 버튼은 외부 브라우저로 열릴 수 있음
