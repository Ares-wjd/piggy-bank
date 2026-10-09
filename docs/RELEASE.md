# 배포: 설치 링크와 앱 안 업데이트

## 구조

- 배포 창구는 이 저장소의 **GitHub Releases**다.
- GitHub 웹에서 Release를 **발행(Publish)**하면 GitHub Actions(`.github/workflows/release.yml`)가 서명된 APK를 빌드해 `piggybank.apk`로 첨부한다(약 5분).
- **설치 링크**(항상 최신 버전):

  ```
  https://github.com/Ares-wjd/piggy-bank/releases/latest/download/piggybank.apk
  ```

- 앱은 켤 때마다(그리고 설정 탭의 **업데이트 확인**을 누르면) 최신 릴리스를 확인한다. 새 버전이 있으면 "업데이트 / 나중에" 팝업을 띄운다. 업데이트를 누르면 APK를 받아 안드로이드 설치 화면을 연다. 기록은 그대로 유지된다.
- 안드로이드는 처음 설치한 APK와 **같은 키로 서명된 APK만** 업데이트로 받아들인다. 그래서 서명 키는 한 번 만들면 계속 같은 것을 써야 한다.

## 1. 서명 키 만들기 (처음 한 번)

`keytool`이 필요하다. Android Studio에 들어 있고, JDK(예: Temurin 17)를 설치해도 된다.

- Windows (Android Studio 기본 설치 경로):
  ```powershell
  & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore piggybank-release.jks -alias piggybank -keyalg RSA -keysize 4096 -validity 10000
  ```
- macOS:
  ```bash
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore piggybank-release.jks -alias piggybank -keyalg RSA -keysize 4096 -validity 10000
  ```

비밀번호와 이름 등을 묻는다. 비밀번호는 안전한 곳에 적어 둔다.

> ⚠️ **키 파일(`piggybank-release.jks`)과 비밀번호를 잃어버리면 업데이트를 낼 수 없다.** 사용자는 앱을 지우고 새로 설치해야 한다. 기기 데이터는 사라지지만 드라이브 백업으로 복원할 수 있다. 키 파일은 USB나 개인 클라우드 등 안전한 곳에 따로 보관한다.
>
> ⚠️ 키 파일은 **저장소에 절대 커밋하지 않는다**(`.gitignore`에 `*.jks` 등록됨).

## 2. SHA-1을 Google Cloud에 등록

```bash
keytool -list -v -keystore piggybank-release.jks -alias piggybank
```

출력의 `SHA1:` 값을 [GOOGLE_CLOUD_SETUP.md](GOOGLE_CLOUD_SETUP.md) 4번에 따라 배포용 Android 클라이언트에 등록한다. 이것을 빼면 배포한 앱에서 로그인이 안 된다.

## 3. GitHub Secrets 등록 (처음 한 번)

GitHub Actions가 서명할 수 있도록 키를 **암호화된 Secrets**에 넣는다. 공개 저장소여도 Secrets는 다른 사람이 볼 수 없다.

1. 키 파일을 Base64 문자열로 바꾼다.
   - Windows PowerShell (`piggybank-release.jks`가 있는 폴더에서):
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("piggybank-release.jks")) | Set-Clipboard
     ```
     이렇게 하면 결과가 클립보드에 복사된다.
   - macOS:
     ```bash
     base64 -i piggybank-release.jks | pbcopy
     ```
2. 저장소 → **Settings → Secrets and variables → Actions → New repository secret**에서 4개를 만든다.

   | Name | Secret 값 |
   |---|---|
   | `PIGGYBANK_KEYSTORE_BASE64` | 1에서 복사한 긴 문자열 |
   | `PIGGYBANK_KEYSTORE_PASSWORD` | 키스토어 비밀번호 |
   | `PIGGYBANK_KEY_ALIAS` | `piggybank` |
   | `PIGGYBANK_KEY_PASSWORD` | 키 비밀번호(따로 정하지 않았다면 키스토어 비밀번호와 같음) |

## 4. 릴리스 발행

1. 저장소 → 오른쪽 **Releases** → **Draft a new release**
2. **Choose a tag**에 `v1.0.0`처럼 `v` + 앱 버전을 입력하고 **Create new tag: v1.0.0 on publish**를 고른다. Target은 `main`.
   - 앱 버전은 `app/build.gradle.kts`의 `versionName`이다. 태그와 다르면 빌드가 실패한다.
3. **Release title**: 예) `Piggy bank 1.0.0`
4. 설명 칸에 바뀐 내용을 한국어로 쓴다. **이 내용이 앱의 업데이트 팝업에 그대로 보인다.**
5. **Set as a pre-release**는 체크하지 않는다(체크하면 앱이 업데이트로 보지 않는다).
6. **Publish release**
7. **Actions** 탭에서 "Release APK"가 초록색이 되면(약 5분) 릴리스에 `piggybank.apk`가 붙는다.

## 5. 설치 링크 공유

사용자에게 다음 링크를 보낸다.

```
https://github.com/Ares-wjd/piggy-bank/releases/latest/download/piggybank.apk
```

사용자는 휴대폰에서 링크를 열어 받은 파일을 누르고, "출처를 알 수 없는 앱 설치"를 허용한 뒤 설치한다.

## 6. 새 버전 내기

1. `app/build.gradle.kts`에서 `versionCode`를 1 올리고 `versionName`을 바꾼다(예: `1.0.1`). 기능 수정과 함께 PR로 `main`에 합친다.
2. 4번처럼 태그 `v1.0.1`로 릴리스를 발행한다.
3. 사용자가 앱을 켜면 업데이트 팝업이 뜬다. 처음 한 번은 "이 출처 허용" 설정이 필요하고, 앱이 그 화면으로 안내한다.

## 참고

- 디버그 빌드(Android Studio에서 바로 실행한 앱)는 업데이트 확인을 하지 않는다. 패키지 이름이 달라서(`.debug`) 릴리스로 업데이트할 수 없기 때문이다.
- 내 PC에서 직접 서명된 APK를 만들려면, 프로젝트 최상위 폴더에 `keystore.properties`를 만들고 `./gradlew assembleRelease`를 실행한다. 이 파일도 `.gitignore`에 등록되어 있다.

  ```properties
  storeFile=piggybank-release.jks
  storePassword=키스토어_비밀번호
  keyAlias=piggybank
  keyPassword=키_비밀번호
  ```

- DB 구조를 바꿀 때는 `AppDatabase`의 `version`을 올리고 Migration을 추가한다. 백업 파일 형식(`BackupFile`)을 바꾸면 `SCHEMA_VERSION`을 올린다.
