import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 수정할 때마다 0.1씩 올림 (versionCode도 +1). APK 파일명에도 반영됨.
val appVersion = "0.8"

// 서명 정보는 환경 변수(GitHub Actions Secrets)로만 받는다. 소스에 비밀번호/키 파일을 두지 않는다.
//  KEYSTORE_FILE(선택, 기본 app/instamini.jks), KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
val ksFile = file(System.getenv("KEYSTORE_FILE") ?: "instamini.jks")
val ksPassword: String? = System.getenv("KEYSTORE_PASSWORD")
val canSign = ksPassword != null && ksFile.exists()

base {
    archivesName.set("InstaMini-v$appVersion")
}

android {
    namespace = "com.instamini.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.instamini.app"
        minSdk = 26
        // 34 유지: 35 이상은 edge-to-edge가 강제되어 상단바가 상태표시줄과 겹침
        targetSdk = 34
        versionCode = 9
        versionName = appVersion
    }

    // 같은 키로 서명해야 업데이트 설치가 되고 로그인 데이터가 유지됨 (키가 바뀌면 패키지 충돌)
    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
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
            // 환경 변수가 없으면 서명되지 않은 release APK가 만들어진다(설치 불가).
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

// 외부 의존성 없음 (androidx, Compose, 네트워크/분석/광고 SDK 모두 미사용)
