import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.alexvt.datapoints"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alexvt.datapoints"
        minSdk = 31
        targetSdk = 34
        versionCode = (System.currentTimeMillis() / 10_000).toInt() // 10-second timestamp
        versionName = "1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val secretsProperties = Properties()
    val secretsPropertiesFile = file("../secrets.properties")
    if (secretsPropertiesFile.exists()) secretsProperties.load(FileInputStream(secretsPropertiesFile))

    signingConfigs {
        create("release") {
            storeFile =
                if (secretsPropertiesFile.exists()) {
                    file(secretsProperties["signingStoreLocation"] as String)
                } else {
                    null
                }
            storePassword = secretsProperties["signingStorePassword"] as? String
            keyAlias = secretsProperties["signingKeyAlias"] as? String
            keyPassword = secretsProperties["signingKeyPassword"] as? String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " debug"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_debug"
            buildConfigField("String", "CSV_RELATIVE_PATH", "\"${
                secretsProperties["csvRelativePathDebug"] as? String ?: "DataPoints-debug"
            }\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            buildConfigField("String", "CSV_RELATIVE_PATH", "\"${
                secretsProperties["csvRelativePath"] as? String ?: "DataPoints"
            }\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint {
        checkReleaseBuilds = false
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.android.gms:play-services-location:21.0.1")
}