import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.instamini.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.instamini.app"
        minSdk = 26
        // 34 유지: 35 이상은 edge-to-edge가 강제되어 상단바가 상태표시줄과 겹침
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 개인 설치용: 디버그 키로 서명해야 release APK를 바로 설치할 수 있음
            signingConfig = signingConfigs.getByName("debug")
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

// 외부 의존성 없음 (androidx, Compose, 네트워크/분석/광고 SDK 모두 미사용)
