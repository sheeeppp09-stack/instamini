import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val appVersion = "0.3"

base {
    archivesName.set("InstaMini-v$appVersion")
}

// Signing credentials are injected by GitHub Actions Secrets.
// Never store the keystore or passwords in this public repository.
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
        versionCode = 4
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

// No external dependencies: no AndroidX, Compose, network, analytics, or ad SDKs.
