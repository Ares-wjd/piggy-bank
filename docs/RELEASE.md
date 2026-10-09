# 배포: 자동 빌드, 설치 링크, 앱 안 업데이트

## 구조

- **push할 때마다** GitHub Actions(`.github/workflows/android.yml`)가 다음을 차례로 한다.
  1. 테스트하고 디버그 APK를 만든다(약 3~5분).
  2. 고정 서명 키로 서명한다.
  3. GitHub 릴리스 **`dev-latest`**(사전 릴리스)를 새로 만든다.
- 버전 번호(versionCode)는 GitHub Actions 빌드 번호다. 빌드할 때마다 1씩 올라가므로 손으로 바꿀 일이 없다.
- **설치 링크**(항상 최신 빌드):

  ```
  https://github.com/Ares-wjd/piggy-bank/releases/download/dev-latest/piggybank.apk
  ```

- 앱은 켤 때마다 `dev-latest`를 확인한다. 더 높은 빌드가 있으면 **[업데이트] [이 버전 건너뛰기] [나중에]** 팝업을 띄운다. 설정 탭의 **업데이트 확인** 버튼으로도 확인할 수 있다.
- 안드로이드는 처음 설치한 APK와 **같은 키로 서명된 APK만** 업데이트로 받아들인다. 그래서 서명 키는 한 번 만들면 계속 같은 것을 써야 한다.
- ⚠️ 어느 브랜치든 push하면 바로 가족 폰에 업데이트 알림이 간다.

## 처음 한 번: 서명 키 만들기

`keytool`이 필요하다. Android Studio에 들어 있고, JDK(예: Temurin 17)를 설치해도 된다.

- Windows (Android Studio 기본 설치 경로):
  ```powershell
  & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore piggybank.jks -alias piggybank -keyalg RSA -keysize 4096 -validity 10000
  ```
- macOS:
  ```bash
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore piggybank.jks -alias piggybank -keyalg RSA -keysize 4096 -validity 10000
  ```

- 비밀번호를 정하라고 하면 정하고 안전한 곳에 적어 둔다.
- 이름, 조직 등은 아무렇게나 입력해도 된다.
- 마지막에 키 비밀번호를 따로 물으면 **Enter**를 눌러 같은 비밀번호를 쓴다. CI는 비밀번호 하나만 쓴다.

> ⚠️ **키 파일(`piggybank.jks`)과 비밀번호를 잃어버리면 업데이트를 낼 수 없다.** 그러면 가족이 앱을 지우고 새로 설치해야 한다. 기기 데이터는 사라지지만 드라이브 백업으로 복원할 수 있다. 키 파일은 USB나 개인 클라우드 등 안전한 곳에 따로 보관한다.
>
> ⚠️ 키 파일은 **저장소에 절대 커밋하지 않는다**(`.gitignore`에 `*.jks` 등록됨).

## 처음 한 번: GitHub Secrets 등록

GitHub Actions가 서명할 수 있도록 키를 **암호화된 Secrets**에 넣는다. 공개 저장소여도 Secrets는 다른 사람이 볼 수 없고, 외부 PR 빌드에는 전달되지 않는다.

1. 키 파일을 Base64 문자열로 바꿔 클립보드에 복사한다.
   - Windows PowerShell(`piggybank.jks`가 있는 폴더에서):
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("piggybank.jks")) | Set-Clipboard
     ```
   - macOS:
     ```bash
     base64 -i piggybank.jks | pbcopy
     ```
2. 저장소 → **Settings → Secrets and variables → Actions → New repository secret**에서 2개를 만든다.

   | Name | Secret 값 |
   |---|---|
   | `SIGNING_KEYSTORE_BASE64` | 1에서 복사한 긴 문자열 |
   | `SIGNING_KEYSTORE_PASSWORD` | 키 비밀번호 |

키 별칭(alias)은 `piggybank`로 정해져 있다. 다른 별칭으로 만들었다면 Secret `SIGNING_KEY_ALIAS`도 추가하고 워크플로에 넘겨야 한다.

## 처음 한 번: Google Cloud에 SHA-1 등록

```bash
keytool -list -v -keystore piggybank.jks -alias piggybank
```

출력의 `SHA1:` 값을 [GOOGLE_CLOUD_SETUP.md](GOOGLE_CLOUD_SETUP.md) 4번에 따라 등록한다. 이것을 빼면 앱에서 로그인이 안 된다.

## 첫 배포와 설치

1. Secrets를 등록한 뒤 아무 push나 하거나, 저장소 **Actions → Android CI → Run workflow**로 직접 실행한다.
2. 실행이 초록색이 되면 저장소 **Releases**에 "개발 버전 (빌드 #N)"이 생긴다.
3. 가족 폰에서 설치 링크를 열어 내려받는다. "출처를 알 수 없는 앱 설치"를 허용하고 설치한다.

## 이후 업데이트

- 코드를 고쳐 push하면 자동으로 새 빌드가 올라간다.
- 가족이 앱을 켜면 업데이트 팝업이 뜬다. **업데이트**를 누르면 앱 안에서 받아 설치 화면이 열린다.
  - 처음 한 번은 "이 출처 허용" 설정이 필요하고, 앱이 그 화면으로 안내한다.
- 기록은 그대로 유지된다.

## 참고

- Secrets가 없거나 PR 빌드이면 임시 키로 빌드만 하고 배포하지 않는다.
- Android Studio에서 직접 빌드한 앱은 서명이 달라서 업데이트를 확인하지 않는다.
- 배포하는 것은 디버그 빌드다. 가족이 쓰는 앱이고 개발자가 직접 관리하므로 이 방식을 쓴다.
- DB 구조를 바꿀 때는 `AppDatabase`의 `version`을 올리고 Migration을 추가한다. 백업 파일 형식(`BackupFile`)을 바꾸면 `SCHEMA_VERSION`을 올린다.
