import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// 릴리스 서명 정보. 저장소에 올리지 않는 keystore.properties(또는 환경변수)에서 읽는다. docs/RELEASE.md 참고.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "PIGGYBANK_KEYSTORE_FILE")

android {
    namespace = "io.github.areswjd.piggybank"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.areswjd.piggybank"
        minSdk = 26
        targetSdk = 36
        // 새 버전을 낼 때 versionCode를 1 올리고 versionName을 바꾼다. 릴리스 태그는 "v" + versionName. docs/RELEASE.md 참고.
        versionCode = 1
        versionName = "1.0.0"

        // 앱 안 업데이트 확인에 쓰는 공개 저장소(GitHub Releases).
        buildConfigField("String", "UPDATE_REPOSITORY", "\"Ares-wjd/piggy-bank\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "PIGGYBANK_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "PIGGYBANK_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "PIGGYBANK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // 디버그 빌드는 패키지 이름과 서명이 달라 릴리스로 업데이트할 수 없으므로 확인하지 않는다.
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
        }
        release {
            // 서명 정보가 없으면(CI 등) 서명하지 않은 APK를 만든다.
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "true")
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
