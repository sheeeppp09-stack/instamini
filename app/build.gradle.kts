import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 수정할 때마다 0.1씩 올림 (versionCode도 +1). APK 파일명에도 반영됨.
val appVersion = "0.7"

// 서명 정보는 GitHub Actions Secrets 환경 변수로만 받는다.
val signingFile = file(System.getenv("KEYSTORE_FILE") ?: "instamini.jks")
val signingStorePassword = System.getenv("KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("KEY_ALIAS")
val signingKeyPassword = System.getenv("KEY_PASSWORD")
val canSign = signingFile.exists() &&
    !signingStorePassword.isNullOrBlank() &&
    !signingKeyAlias.isNullOrBlank() &&
    !signingKeyPassword.isNullOrBlank()

base {
    archivesName.set("InstaMini-v$appVersion")
}

android {
    namespace = "com.instamini.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.instamini.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = appVersion
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = signingFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 서명 정보가 없으면 unsigned APK를 생성한다. 배포 Workflow는 서명 Secrets를 필수 검사한다.
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/*.kotlin_module",
                "/META-INF/*.version",
                "/kotlin/**",
                "/kotlin-tooling-metadata.json",
                "/DebugProbesKt.bin"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
}

// 외부 의존성 없음 (AndroidX, Compose, 네트워크/분석/광고 SDK 미사용)
