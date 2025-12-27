plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.android.pixivviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.pixivviewer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // 用於解析 JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // 用於除錯看 Log

    // Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.compose.foundation:foundation:1.10.0")

    // Navigation (Compose)
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.browser:browser:1.9.0")
    // Lifecycle (ViewModel)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("me.saket.telephoto:zoomable-image-coil:0.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}