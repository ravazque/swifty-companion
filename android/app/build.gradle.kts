import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// API credentials come from the git-ignored .env at the repository root.
val env = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("../.env")).asText.orNull
        ?.let { load(it.reader()) }
}

fun envString(key: String): String {
    val value = env.getProperty(key).orEmpty().trim().removeSurrounding("\"")
    if (value.isEmpty()) logger.warn("w: $key is not set in .env, API requests will fail")
    return "\"$value\""
}

android {
    namespace = "com.ravazque.swiftycompanion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ravazque.swiftycompanion"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "INTRA_CLIENT_ID", envString("INTRA_CLIENT_ID"))
        buildConfigField("String", "INTRA_CLIENT_SECRET", envString("INTRA_CLIENT_SECRET"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
