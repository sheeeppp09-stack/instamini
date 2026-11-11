# InstaMini v0.2 빌드 및 검증

## GitHub Actions 빌드

1. GitHub repository Settings → Secrets and variables → Actions 에서 아래 secret 4개 등록
   - KEYSTORE_BASE64
   - KEYSTORE_PASSWORD
   - KEY_ALIAS
   - KEY_PASSWORD
2. Actions → Build APK → Run workflow 실행
3. 성공한 실행에서 Artifacts 또는 Releases의 v0.2 APK 다운로드

Windows PowerShell에서 JKS Base64 복사:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\app\instamini.jks")) | Set-Clipboard
```

JKS 원본은 공개 저장소에 커밋하지 않는다. Actions runner에서만 임시로 복원한다.

## 버전

- versionName: 0.2
- versionCode: 3 (v0.1의 versionCode 2보다 증가)
- APK: app/build/outputs/apk/release/InstaMini-v0.2-release.apk

## 서명 주의

v0.1은 임시 디버그 키로 빌드됐고 v0.2부터는 고정 JKS로 서명한다. 따라서 최초 v0.2 설치 시 기존 v0.1 삭제가 필요할 수 있으며, 앱 삭제 시 로그인 정보가 지워진다. v0.2부터 동일한 JKS와 비밀번호를 유지하면 이후 업데이트에 같은 서명을 사용할 수 있다.

## 실기기 검증

1. 로그인 → 앱 완전 종료 → 재실행 → 로그인 유지 확인
2. DM 목록 및 메시지 전송
3. Stories 열람 및 답장
4. Reels/Explore 차단 확인
5. 홈 피드 동영상 자동재생 억제 확인
6. 릴스 단일 링크 세로 스크롤 차단 확인
7. 캐시 삭제 후 쿠키/로그인 유지 확인
8. 회전과 설정 왕복 시 불필요한 WebView 재로딩 여부 확인
