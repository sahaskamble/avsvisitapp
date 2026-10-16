plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.avs.visitapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.avs.visitapp"
        minSdk = 21
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "2").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            // 1. CHANGE FILE NAME: Point this to your new file
            // Ensure 'upload-keystore.jks' is inside your 'app' folder
            storeFile = file("upload-keystore.jks")

            // 2. CHANGE PASSWORDS: Use the password you just created in the terminal
            storePassword = "AVS@12345"
            keyPassword = "AVS@12345"

            // 3. CHANGE ALIAS: This MUST be 'upload' (as used in your command)
            keyAlias = "upload"

            // 4. REMOVE or UPDATE storeType:
            // Since your file extension is .jks, it is safest to remove this line
            // and let Gradle detect the type automatically.
            // storeType = "PKCS12"  <-- Delete this line
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
    }
}

dependencies {
    // AndroidX
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Google Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
    implementation("com.google.code.gson:gson:2.10.1")

    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Media3
    implementation("androidx.media3:media3-common:1.3.1")

    // UI Enhancements
    implementation("de.hdodenhof:circleimageview:3.1.0")
//    implementation("com.github.prolificinteractive:MaterialCalendarView:v1.4.3")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    // PDF Handling
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("com.github.librepdf:openpdf:1.3.30")

    // YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Places API
    implementation("com.google.android.libraries.places:places:3.5.0")

    implementation("net.sourceforge.jexcelapi:jxl:2.6.12")

    implementation("com.github.GrenderG:Toasty:1.5.2")

    implementation("androidx.annotation:annotation:1.7.1")
}
