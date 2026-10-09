import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// 서명 키는 CI(GitHub Actions)가 Secrets에서 꺼내 임시 파일로 넘겨준다. 저장소에는 키가 없다. docs/RELEASE.md 참고.
val signingKeystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")
val signingKeystorePassword: String? = System.getenv("SIGNING_KEYSTORE_PASSWORD")

// 빌드 번호. CI에서는 GitHub Actions 실행 번호라 빌드할 때마다 1씩 올라간다. 로컬 빌드는 1.
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "io.github.areswjd.piggybank"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.areswjd.piggybank"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0"

        // 앱 안 업데이트 확인에 쓰는 공개 저장소(GitHub 릴리스 dev-latest).
        buildConfigField("String", "UPDATE_REPOSITORY", "\"Ares-wjd/piggy-bank\"")
        // 업데이트 확인은 CI에서 고정 키로 서명한 빌드에서만 한다. 직접 빌드한 앱은 서명이 달라 업데이트를 설치할 수 없다.
        buildConfigField("boolean", "UPDATE_CHECK_ENABLED", (signingKeystorePath != null).toString())
    }

    signingConfigs {
        signingKeystorePath?.let { path ->
            create("shared") {
                storeFile = file(path)
                storePassword = signingKeystorePassword
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "piggybank"
                keyPassword = signingKeystorePassword
            }
        }
    }

    buildTypes {
        // 배포하는 것은 디버그 빌드다. 항상 같은 키(shared)로 서명해서 폰에서 덮어쓰기 업데이트가 된다.
        debug {
            signingConfigs.findByName("shared")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric(DB 테스트)이 안드로이드 리소스를 읽을 수 있게 한다.
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    // DB 스키마 기록. 스키마를 바꿀 때 마이그레이션 기준이 된다. 생성된 app/schemas 는 커밋한다.
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)

    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
}

// CI 로그에서 테스트별 결과를 볼 수 있게 한다.
tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
