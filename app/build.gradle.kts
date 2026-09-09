plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.usbmediaexplorer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.usbmediaexplorer"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Keep the APK small: the app ships English + Arabic only.
        resourceConfigurations += listOf("en", "ar")
    }

    // Release signing is secret-only; keys and fallback passwords must never be committed.
    val releaseKeystorePath = System.getenv("USBMEDIA_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("USBMEDIA_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("USBMEDIA_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("USBMEDIA_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseKeystorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
    ).all { !it.isNullOrBlank() } && releaseKeystorePath?.let { file(it).isFile } == true
    signingConfigs {
        create("release") {
            storeFile = file(releaseKeystorePath ?: "missing-release-keystore.p12")
            storePassword = releaseStorePassword.orEmpty()
            keyAlias = releaseKeyAlias.orEmpty()
            keyPassword = releaseKeyPassword.orEmpty()
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    tasks.matching { it.name == "packageRelease" }.configureEach {
        doFirst {
            check(releaseSigningConfigured) {
                "Release signing requires USBMEDIA_KEYSTORE_PATH, USBMEDIA_STORE_PASSWORD, " +
                    "USBMEDIA_KEY_ALIAS, and USBMEDIA_KEY_PASSWORD."
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Opt in globally: Media3 and Material3 annotate a lot of the APIs this app depends on.
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=androidx.media3.common.util.UnstableApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Keep CI failure logs readable: only failed/skipped tests print, with short stack traces.
    tasks.withType<Test>().configureEach {
        testLogging {
            events("failed", "skipped")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        // Plain-text report: mined by the CI failure-triage step on lint crashes.
        textReport = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource)
    implementation(libs.media3.common)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.okio)
    implementation(libs.androidx.exifinterface)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Real org.json (android.jar stubs return null) + Mockito for android.net.Uri fakes.
    testImplementation(libs.org.json)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
