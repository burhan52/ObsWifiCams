import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

// ---------------------------------------------------------------------------
// Release signing: read from keystore.properties (gitignored) or env vars.
// Without either, release builds fall back to the debug key with a warning so
// local `assembleRelease` still works; such an artifact must NOT be uploaded.
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    (keystoreProps[key] as String?) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "CC_KEYSTORE_PATH")

val envProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.isFile) envFile.inputStream().use { load(it) }
}
fun envValue(key: String, default: String = ""): String =
    (envProps[key] as String?)?.trim() ?: System.getenv(key) ?: default

@Suppress("DEPRECATION")
android {
    namespace = "com.stratatechlabs.obswificams"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.stratatechlabs.obswificams"
        minSdk = 24
        //noinspection TargetSdkVersion,OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val admobAppId = envValue("ADMOB_APP_ID", "ca-app-pub-2382308752605431~8550584950")
        val admobBackInterstitialId = envValue("ADMOB_BACK_INTERSTITIAL_ID", "ca-app-pub-2382308752605431/3114425441")
        val admobOpenAppId = envValue("ADMOB_OPEN_APP_ID", "ca-app-pub-2382308752605431/1985176601")
        val admobRewardTeaId = envValue("ADMOB_REWARD_TEA_ID", "ca-app-pub-2382308752605431/4419768250")

        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId

        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BACK_INTERSTITIAL_ID", "\"$admobBackInterstitialId\"")
        buildConfigField("String", "ADMOB_OPEN_APP_ID", "\"$admobOpenAppId\"")
        buildConfigField("String", "ADMOB_REWARD_TEA_ID", "\"$admobRewardTeaId\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "CC_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "CC_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "CC_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        // Release code (R8 + resource shrinking) under its own applicationId and the
        // debug key, so the shrunk build can be installed and smoke-tested beside
        // a Play-installed release.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/version-control-info.textproto",
            )
        }
    }


}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Warn only when a release artifact is actually being produced without a real key.
gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any { it.name.contains("Release") && (it.project == project) }
    if (buildingRelease && (releaseStoreFile == null)) {
        // CI release builds pass -PrequireReleaseSigning so a debug-signed bundle can never ship.
        if (project.hasProperty("requireReleaseSigning")) {
            throw GradleException("Release signing required but no keystore is configured (keystore.properties or CC_KEYSTORE_* env).")
        }
        logger.warn(
            "ObsWifiCams: no release keystore configured — the release build is signed " +
                "with the DEBUG key and must not be uploaded to Google Play.",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)
    implementation(libs.camerax.extensions)
    implementation(libs.camerax.mlkit.vision)
    implementation(libs.mlkit.barcode)
    implementation(libs.play.services.ads)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.uiautomator)
}
