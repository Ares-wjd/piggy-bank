# (현재 배포는 디버그 빌드라 이 파일은 쓰이지 않는다. 릴리스 빌드를 만들 때를 위해 남겨 둔다.)
# 릴리스 빌드에서 android.util.Log 호출을 제거한다 (금액·내용 등이 로그로 새지 않도록).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# SQLCipher: 네이티브 코드가 JNI로 찾는 클래스라 이름을 유지해야 한다.
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }
