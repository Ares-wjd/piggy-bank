# Google Cloud 설정 (구글 로그인 · 드라이브 백업)

Piggy bank는 사용자 **본인의** 구글 계정으로 로그인하고, 그 계정의 구글 드라이브 "앱 전용 폴더(appDataFolder)"에 백업한다.
이를 위해 Google Cloud 프로젝트를 한 번 만들어 두어야 한다. 코드에는 클라이언트 ID나 시크릿을 넣지 않는다.
앱은 **패키지 이름 + 서명 인증서 SHA-1**로 식별된다.

> 콘솔 메뉴 이름은 Google이 자주 바꾼다. 아래 이름이 보이지 않으면 콘솔 검색창에 "OAuth 동의 화면", "사용자 인증 정보"를 검색한다.

## 1. 프로젝트 만들기

1. <https://console.cloud.google.com/> 에 개발자 계정으로 로그인
2. 상단 프로젝트 선택 → **새 프로젝트** → 이름 예: `piggy-bank`

## 2. Google Drive API 사용 설정

1. **API 및 서비스 → 라이브러리**
2. `Google Drive API` 검색 → **사용**

## 3. OAuth 동의 화면 (Google 인증 플랫폼)

1. **API 및 서비스 → OAuth 동의 화면** (새 콘솔에서는 **Google 인증 플랫폼**)
2. **브랜딩**: 앱 이름 `Piggy bank`, 사용자 지원 이메일, 개발자 연락처 이메일 입력
3. **대상(사용자 유형)**: **외부**
4. **데이터 액세스(범위)**: **범위 추가** → `https://www.googleapis.com/auth/drive.appdata` 선택
   - 앱 전용 숨김 폴더만 접근하는 좁은 권한이다. 사용자 드라이브의 다른 파일은 볼 수 없다.
5. **게시 상태를 "프로덕션"으로 변경** ← 중요
   - "테스트" 상태에서는 테스트 사용자로 등록한 계정만 로그인할 수 있고, 권한이 7일마다 만료된다.
   - 다른 사람이 자기 계정으로 쓰려면 반드시 프로덕션이어야 한다.
   - 콘솔에서 앱 확인(verification)을 요구하면 안내에 따른다. 확인 전이라도 사용자에게 "확인되지 않은 앱" 경고가 나올 수 있으며, **고급 → 이동**으로 계속할 수 있다.

## 4. Android OAuth 클라이언트 만들기

**API 및 서비스 → 사용자 인증 정보 → 사용자 인증 정보 만들기 → OAuth 클라이언트 ID** (새 콘솔: **Google 인증 플랫폼 → 클라이언트 → 클라이언트 만들기**)

빌드 종류마다 하나씩, 두 개를 만든다.

| 용도 | 애플리케이션 유형 | 패키지 이름 | SHA-1 |
|---|---|---|---|
| 배포용(릴리스) | Android | `io.github.areswjd.piggybank` | 릴리스 서명 키의 SHA-1 |
| 개발용(디버그) | Android | `io.github.areswjd.piggybank.debug` | 내 PC 디버그 키의 SHA-1 |

### SHA-1 확인 방법

릴리스 키 (만드는 방법은 [RELEASE.md](RELEASE.md)):

```bash
keytool -list -v -keystore piggybank-release.jks -alias piggybank
```

디버그 키 (Android Studio가 자동으로 만든 키):

```bash
# macOS / Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

또는 프로젝트 폴더에서 `./gradlew signingReport` 를 실행하면 두 키의 SHA-1이 모두 나온다.

출력의 `SHA1: AA:BB:...` 값을 그대로 붙여 넣는다.

## 5. 확인

1. 휴대폰에 앱을 설치하고 **Google 계정으로 시작하기**
2. 계정 선택 → 드라이브 권한 허용 → 가계부 화면이 열리면 성공
3. 설정 탭 → **지금 백업** → "백업했어요"가 나오면 드라이브 연결 성공

## 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| 계정을 고른 뒤 "로그인하지 못했어요" | 패키지 이름이나 SHA-1이 등록한 값과 다르다. 특히 디버그/릴리스를 섞지 않았는지 확인. 등록 후 반영까지 몇 분 걸릴 수 있다. |
| 개발자 계정만 로그인되고 다른 계정은 안 됨 | OAuth 동의 화면이 아직 "테스트" 상태다. 3-5번 참고. |
| GitHub Actions에서 받은 디버그 APK로 로그인 안 됨 | CI는 매번 다른 임시 디버그 키로 서명한다. 로그인은 직접 빌드한 APK나 릴리스 APK로 확인한다. |
| "Google Drive API has not been used..." 오류로 백업 실패 | 2번(Drive API 사용 설정)을 하지 않았다. |
| 휴대폰에 Google Play 서비스가 없음 | Google 로그인을 쓸 수 없다(일부 중국 판매 기기 등). |
