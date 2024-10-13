plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.quizyy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.quizyy"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.generativeai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
// Glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Google AI
    implementation("com.google.ai.client.generativeai:generativeai:0.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // iText PDF
    implementation("com.itextpdf:itext7-core:7.1.16")

    // Gson
    implementation("com.google.code.gson:gson:2.8.8")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

    // Lottie
    implementation("com.airbnb.android:lottie:6.1.0")

    // SmoothProgressBar
    implementation("com.github.castorflex.smoothprogressbar:library:1.1.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation ("androidx.compose.ui:ui:1.0.5")
    implementation ("androidx.compose.material:material:1.0.5")
    implementation ("androidx.navigation:navigation-compose:2.4.0-rc01")
    implementation ("androidx.compose.runtime:runtime-livedata:1.0.5")
    implementation("xyz.mcxross.kaptos:kaptos-android:0.1.0-beta")


}