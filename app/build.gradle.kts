import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 새 버전마다 versionName과 versionCode를 함께 올린다.
val appVersion = "0.2"

base {
    archivesName.set("InstaMini-v$appVersion")
}

val signingFile = file("instamini.jks")
val signingStorePassword = System.getenv("KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("KEY_ALIAS")
val signingKeyPassword = System.getenv("KEY_PASSWORD")
val hasPrivateSigningKey = signingFile.exists() &&
    !signingStorePassword.isNullOrBlank() &&
    !signingKeyAlias.isNullOrBlank() &&
    !signingKeyPassword.isNullOrBlank()

android {
    namespace = "com.instamini.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.instamini.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = appVersion
    }

    signingConfigs {
        if (hasPrivateSigningKey) {
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
            // Actions에서는 비밀값으로 고정 키를 사용한다.
            // 키 파일/비밀값이 없는 로컬 빌드는 디버그 키로만 빌드된다.
            signingConfig = if (hasPrivateSigningKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
