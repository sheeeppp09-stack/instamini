# InstaMini v0.4 빌드 및 검증

## 빌드 환경
- JDK 17
- Android SDK 34
- 최소 Android 버전: Android 8.0 (API 26)
- versionName: 0.4
- versionCode: 5

## 빌드
```bash
./gradlew assembleRelease
```

결과: `app/build/outputs/apk/release/InstaMini-v0.4-release.apk`

GitHub Actions는 Repository Secrets에서 기존 서명 키를 복원한 뒤 APK를 빌드하고 v0.4 Release에 게시합니다.

## 서명 키 보안
- `app/instamini.jks`는 저장소에 커밋하지 않습니다.
- 비밀번호를 Gradle 파일에 직접 입력하지 않습니다.
- GitHub Repository Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- 기존 v0.3과 같은 서명 키를 사용해야 업데이트 설치가 가능합니다.

## 수동 검증
1. 기존 앱 위에 업데이트 설치가 되는지 확인
2. 로그인 후 앱을 완전히 종료하고 재실행해 로그인 유지 확인
3. DM 목록 열림 및 메시지 전송 확인
4. Stories 탭에서 스토리 열람 확인
5. Reels / Explore 접근 차단 확인
6. 설정에서 임시 캐시 삭제 후 로그인 유지 확인
7. 라이트/다크 모드 변경 확인

## 알려진 한계
- 백그라운드 DM 감지 및 알림 미구현
- DM 첨부는 파일 선택기(갤러리/파일)만 지원하며 카메라 촬영 첨부는 미지원
- Instagram DOM 변경에 따라 일부 UI 기능이 깨질 수 있음
