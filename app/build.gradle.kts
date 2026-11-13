plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing comes from CI secrets when present, otherwise the debug key is used
// so the release APK is always installable.
val keystorePath: String? = System.getenv("PIVOT_KEYSTORE_PATH")
val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "app.pivot.orientation"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.pivot.orientation"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("PIVOT_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("PIVOT_VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("pivotRelease") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("PIVOT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PIVOT_KEY_ALIAS")
                keyPassword = System.getenv("PIVOT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "pivotRelease" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
}
