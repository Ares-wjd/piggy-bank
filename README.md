# Piggy bank

간단한 개인용 가계부 안드로이드 앱. 데이터는 기기에 저장하고, 사용자 본인의 구글 드라이브에 백업한다.

![앱 아이콘 9가지](docs/icon-preview.png)

- 기능 명세: [docs/SPEC.md](docs/SPEC.md)
- Google Cloud 설정(로그인·드라이브 백업): [docs/GOOGLE_CLOUD_SETUP.md](docs/GOOGLE_CLOUD_SETUP.md)
- 배포(설치 링크·앱 안 업데이트): [docs/RELEASE.md](docs/RELEASE.md)

**설치 링크** (항상 최신 빌드): https://github.com/Ares-wjd/piggy-bank/releases/download/dev-latest/piggybank.apk

## 주요 기능

- **가계부**: 월별 수입·지출·이체 기록, 한 줄 월 요약(숨기기 가능), 날짜별 묶음
- **자산**: 자산 그룹 > 세부 자산 2단계, 거래에 따라 잔액 자동 계산, 잔액 0원일 때만 삭제(기록은 유지)
- **설정**: 디자인 화면에서 테마 8가지·앱 아이콘 9가지 선택, 구글 계정 로그인(필수), 구글 드라이브 백업·복원, 자동 백업, 업데이트 확인
- **업데이트**: 앱을 켤 때 GitHub 릴리스 `dev-latest`에서 새 빌드를 확인하고, 앱 안에서 내려받아 설치
- **보안**: 기기 DB 암호화(SQLCipher + Android Keystore), 드라이브 앱 전용 폴더만 사용, 안드로이드 자동 백업 차단

## 기술 스택

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Room + SQLCipher(DB 암호화) · DataStore · WorkManager · Google Identity(AuthorizationClient) + Drive REST API · 최소 Android 8.0 (API 26)

단위 테스트는 JUnit과 Robolectric(DB 테스트)을 쓴다.

## 빌드

Android Studio에서 프로젝트 폴더를 열고 실행하거나, 명령줄에서:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # 단위 테스트
```

JDK 17 이상이 필요하다.

앱에서 로그인하려면 [Google Cloud 설정](docs/GOOGLE_CLOUD_SETUP.md)이 필요하다. 직접 빌드한 앱은 서명이 달라 로그인이 안 되므로, 실제 확인은 설치 링크로 받은 앱으로 한다.

## CI와 배포

push할 때마다 GitHub Actions가 다음을 실행한다.
1. 단위 테스트와 lint를 돌린다.
2. 디버그 APK를 빌드하고 고정 키로 서명한다.
3. GitHub 릴리스 `dev-latest`로 배포한다.

버전 번호는 빌드 번호로 자동으로 올라간다. 자세한 내용: [docs/RELEASE.md](docs/RELEASE.md)

## 라이선스 고지

- 글꼴(모두 SIL Open Font License 1.1): Jua ([라이선스](docs/licenses/Jua-OFL.txt)), 고운돋움 ([라이선스](docs/licenses/GowunDodum-OFL.txt)), 개구 ([라이선스](docs/licenses/Gaegu-OFL.txt)). 고운돋움·개구는 자주 쓰는 한글 2,350자로 줄여서 넣었다.

## 보안 주의

이 저장소는 공개 저장소다. 서명 키(`*.jks`, `*.keystore`), `local.properties`는 커밋하지 않는다(`.gitignore`에 등록됨). 서명 키는 GitHub Secrets에만 둔다. 앱 코드에는 클라이언트 ID나 시크릿이 없다. 구글은 패키지 이름과 서명 SHA-1로 앱을 식별한다.
