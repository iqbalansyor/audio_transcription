plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.whispercpp"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    ndkVersion = "25.1.8937393"
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/whisper/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
