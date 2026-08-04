plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.atakolstudio.sure"
    // AGP 8.5.2, en fazla compileSdk 34 icin test edilmistir. 35 kullanmak,
    // AAR metadata kontrolunde dolayli bagimliliklarin cok daha yeni (ve bu AGP ile
    // uyumsuz) surumler istemesine yol acar. Bu yuzden burada 34 sabitlenmistir.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.atakolstudio.sure"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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
    }
    composeOptions {
        // Kotlin 1.9.24 ile eslesen Compose Compiler surumu
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Robolectric'in assets/ klasörüne erişebilmesi için
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Testing — Birim testleri (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.4")

    // Testing — Enstrümante (cihaz/emülatör) testleri
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// -----------------------------------------------------------------------
// GUVENLIK KILIDI: Bazi build ortamlari (ozellikle telefon uzerindeki Gradle
// calistiricilari) bagimlilik cozumlemesini beklenenden farkli yapabiliyor ve
// yukarida yazilan surumlerden cok daha yeni, bu AGP ile uyumsuz surumler
// (or. androidx.core:core-ktx:1.19.0, androidx.activity:activity:1.13.0) devreye
// girebiliyor. Asagidaki blok, bu tur bir "sessiz yukseltme" ihtimaline karsi
// yukaridaki tum kritik kutuphaneleri KESIN olarak zorunlu kilar.
// -----------------------------------------------------------------------
configurations.all {
    resolutionStrategy {
        force(
            "androidx.core:core-ktx:1.13.1",
            "androidx.core:core-splashscreen:1.0.1",
            "androidx.activity:activity-compose:1.9.1",
            "androidx.activity:activity:1.9.1",
            "androidx.activity:activity-ktx:1.9.1",
            "androidx.navigation:navigation-compose:2.7.7",
            "androidx.hilt:hilt-navigation-compose:1.2.0",
            "androidx.lifecycle:lifecycle-runtime-ktx:2.8.4",
            "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4",
            "androidx.room:room-runtime:2.6.1",
            "androidx.room:room-ktx:2.6.1"
        )
    }
}
