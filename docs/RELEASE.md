# 릴리스 APK 만들기와 배포

## 1. 서명 키 만들기 (처음 한 번)

```bash
keytool -genkeypair -v -keystore piggybank-release.jks -alias piggybank \
  -keyalg RSA -keysize 4096 -validity 10000
```

- 비밀번호와 이름 등을 묻는다. 비밀번호는 안전한 곳에 적어 둔다.
- ⚠️ **이 키 파일과 비밀번호를 잃어버리면 앱을 업데이트할 수 없다.** 사용자는 앱을 지우고 새로 설치해야 하고, 그러면 기기 데이터가 사라진다(드라이브 백업으로 복원은 가능). 키 파일을 클라우드 등 안전한 곳에 따로 백업해 둔다.
- ⚠️ 키 파일은 **절대 저장소에 커밋하지 않는다.** `*.jks`는 `.gitignore`에 등록되어 있다.

## 2. keystore.properties 만들기

프로젝트 최상위 폴더(`settings.gradle.kts`가 있는 곳)에 `keystore.properties` 파일을 만든다. 이 파일도 `.gitignore`에 등록되어 있다.

```properties
storeFile=piggybank-release.jks
storePassword=키스토어_비밀번호
keyAlias=piggybank
keyPassword=키_비밀번호
```

`storeFile`은 프로젝트 최상위 폴더 기준 경로다. 키를 다른 곳에 두었다면 절대 경로를 써도 된다.

파일 대신 환경변수 `PIGGYBANK_KEYSTORE_FILE`, `PIGGYBANK_KEYSTORE_PASSWORD`, `PIGGYBANK_KEY_ALIAS`, `PIGGYBANK_KEY_PASSWORD`를 써도 된다.

## 3. Google Cloud에 SHA-1 등록

[GOOGLE_CLOUD_SETUP.md](GOOGLE_CLOUD_SETUP.md)의 4번에 따라 릴리스 키의 SHA-1을 등록한다. 이 단계를 빼면 릴리스 APK에서 로그인이 되지 않는다.

## 4. 빌드

```bash
./gradlew assembleRelease
```

결과물: `app/build/outputs/apk/release/app-release.apk`

Android Studio에서는 **Build → Generate Signed App Bundle / APK → APK**로도 만들 수 있다.

## 5. 배포

1. `app-release.apk`를 사용자에게 전달한다(메신저, 구글 드라이브 링크 등).
2. 사용자는 휴대폰에서 APK를 열고, "출처를 알 수 없는 앱 설치"를 허용한 뒤 설치한다.
3. 앱을 열고 자기 구글 계정으로 로그인한다.

## 6. 업데이트 배포

1. `app/build.gradle.kts`의 `versionCode`를 1 올리고 `versionName`을 바꾼다(예: `1.0.1`).
2. **같은 서명 키로** 다시 빌드해서 전달한다. 사용자는 덮어서 설치하면 되고 데이터는 유지된다.

## 7. DB 구조를 바꿀 때

- Room 스키마 기록이 빌드할 때 `app/schemas/`에 생성된다. 첫 빌드 후 이 폴더를 커밋해 둔다.
- 엔티티를 바꾸면 `AppDatabase`의 `version`을 올리고 Migration을 추가한다. Migration 없이 버전만 올리면 앱이 실행되지 않는다.
- 백업 파일 형식(`BackupFile`)을 바꾸면 `SCHEMA_VERSION`을 올리고, 이전 버전 백업도 읽을 수 있게 한다.
