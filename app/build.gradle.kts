import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

val properties = Properties()
val propertiesFile = project.rootProject.file("local.properties")
if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
}

android {
    namespace = "com.streamlux.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.streamlux.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Secrets injection
        buildConfigField("String", "GROQ_API_KEY", "\"${properties.getProperty("GROQ_API_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${properties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${properties.getProperty("FIREBASE_API_KEY") ?: ""}\"")
        buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"${properties.getProperty("FIREBASE_MESSAGING_SENDER_ID") ?: ""}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${properties.getProperty("FIREBASE_APP_ID") ?: ""}\"")
        buildConfigField("String", "TMDB_API_KEY", "\"${properties.getProperty("TMDB_API_KEY") ?: ""}\"")
        buildConfigField("String", "OMD_API_KEY", "\"${properties.getProperty("OMD_API_KEY") ?: ""}\"")
        buildConfigField("String", "FANART_API_KEY", "\"${properties.getProperty("FANART_API_KEY") ?: ""}\"")
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${properties.getProperty("TRAKT_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "TASTEDIVE_API_KEY", "\"${properties.getProperty("TASTEDIVE_API_KEY") ?: ""}\"")
        buildConfigField("String", "WATCHMODE_API_KEY", "\"${properties.getProperty("WATCHMODE_API_KEY") ?: ""}\"")
        buildConfigField("String", "APISPORTS_KEY", "\"${properties.getProperty("APISPORTS_KEY") ?: ""}\"")
        buildConfigField("String", "SCOREBAT_TOKEN", "\"${properties.getProperty("SCOREBAT_TOKEN") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY_1", "\"${properties.getProperty("YOUTUBE_API_KEY_1") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY_2", "\"${properties.getProperty("YOUTUBE_API_KEY_2") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY_3", "\"${properties.getProperty("YOUTUBE_API_KEY_3") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY_4", "\"${properties.getProperty("YOUTUBE_API_KEY_4") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // Google Play Services (Auth & AdMob)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    
    // Credential Manager for Google Sign In
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Google Cast SDK
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")

    // Media3 (ExoPlayer)
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-cast:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // Room Database for Offline Library (History & Watchlist)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Native YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Chrome Custom Tabs for stable movie playback
    implementation("androidx.browser:browser:1.8.0")
}
