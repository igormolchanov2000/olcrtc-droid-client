import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("release-signing.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingValue(propertyName: String, envName: String): String? {
    return releaseSigningProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
}

val releaseStoreFilePath = signingValue("storeFile", "OLCRTC_RELEASE_STORE_FILE")
val releaseStoreFile = releaseStoreFilePath?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("storePassword", "OLCRTC_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "OLCRTC_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "OLCRTC_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }
    && releaseStoreFile?.exists() == true

val buildMobileBindings = tasks.register<Exec>("buildMobileBindings") {
    workingDir(rootProject.projectDir)
    commandLine("bash", rootProject.file("scripts/build-mobile-bindings.sh").absolutePath)
}

val buildTun2Socks = tasks.register<Exec>("buildTun2Socks") {
    workingDir(rootProject.projectDir)
    commandLine("bash", rootProject.file("scripts/build-tun2socks.sh").absolutePath)
}

tasks.named("preBuild") {
    dependsOn(buildMobileBindings)
    dependsOn(buildTun2Socks)
}

android {
    namespace = "org.openlibrecommunity.olcrtc"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.openlibrecommunity.olcrtc"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("releaseForTesting") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            versionNameSuffix = "-testing"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
