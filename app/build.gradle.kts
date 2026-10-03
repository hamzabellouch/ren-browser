plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tkno.ren"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tkno.ren"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.0.2-beta"
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }
    base {
        archivesName.set("ren-browser-v0.0.2-beta")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            jniLibs.directories.add("src/main/jniLibs")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    lint {
        disable += setOf("MissingTranslation", "ExtraTranslation")
        abortOnError = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("io.matthewnelson.kotlin-components:kmp-tor:4.8.10-0-1.4.5")
    implementation("io.matthewnelson.kotlin-components:kmp-tor-manager-android:1.4.5")
    implementation("io.matthewnelson.kotlin-components:kmp-tor-ext-callback-manager-android:1.4.5")
    implementation("io.matthewnelson.kotlin-components:kmp-tor-binary-android:4.8.10-0")
    implementation("io.matthewnelson.kotlin-components:kmp-tor-binary-geoip:4.8.10-0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}

