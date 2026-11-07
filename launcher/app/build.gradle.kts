plugins {
    id("com.android.application")
}

android {
    namespace = "com.procastia.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.procastia.launcher"
        minSdk = 22
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
