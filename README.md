# Piggy bank

간단한 개인용 가계부 안드로이드 앱. 데이터는 기기에 저장하고, 사용자 본인의 구글 드라이브에 백업한다.

- 기능 명세: [docs/SPEC.md](docs/SPEC.md)

## 기술 스택

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Room + SQLCipher(DB 암호화) · 최소 Android 8.0 (API 26)

단위 테스트는 JUnit과 Robolectric(DB 테스트)을 쓴다.

## 빌드

Android Studio에서 프로젝트 폴더를 열고 실행하거나, 명령줄에서:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # 단위 테스트
```

JDK 17 이상이 필요하다.

## CI

푸시할 때마다 GitHub Actions가 빌드, 단위 테스트, lint를 실행한다. 빌드된 디버그 APK는 Actions 실행 화면의 **Artifacts**에서 받을 수 있다.

## 라이선스 고지

- Jua 폰트: SIL Open Font License 1.1 ([docs/licenses/Jua-OFL.txt](docs/licenses/Jua-OFL.txt))

## 보안 주의

이 저장소는 공개 저장소다. 서명 키(`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, `google-services.json`은 커밋하지 않는다(`.gitignore`에 등록됨).
