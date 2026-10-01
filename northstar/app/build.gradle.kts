plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Firebase è opzionale: il plugin si attiva solo se hai messo app/google-services.json.
// Senza, l'app gira in "modalità locale" (dati sul telefono, niente login reale).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// AdMob: di default gli ID di TEST di Google. Per la release metti i tuoi in
// ~/.gradle/gradle.properties (ADMOB_APP_ID, ADMOB_NATIVE_ID), non nel repo.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"
val admobAppId = providers.gradleProperty("ADMOB_APP_ID").getOrElse(testAppId)
val admobNativeId = providers.gradleProperty("ADMOB_NATIVE_ID").getOrElse(testNativeId)

android {
    namespace = "com.francescopaoli.northstar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.francescopaoli.northstar"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.0"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"$admobNativeId\"")
    }

    buildTypes {
        debug {
            // in debug sempre annunci di test: cliccare i propri annunci veri fa bannare l'account
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$testNativeId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            // serve a Robolectric per disegnare le schermate nei test
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.maxHeapSize = "3g"
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing)
    implementation(libs.haze)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    // screenshot delle schermate senza telefono (Robolectric + Roborazzi)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
