import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.compose.compiler)
    id("com.mikepenz.aboutlibraries.plugin")
}

val props = Properties().apply {
    runCatching {
        load(FileInputStream(rootProject.file("local.properties")))
    }
}

fun readProperty(name: String): String? = props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

val releaseStoreFile = readProperty("release.storeFile")
val releaseStorePassword = readProperty("release.storePassword")
val releaseKeyAlias = readProperty("release.keyAlias")
val releaseKeyPassword = readProperty("release.keyPassword")
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }
val fdroidUnsigned = providers.gradleProperty("fdroidUnsigned").orNull == "true"
val useReleaseSigningConfig = hasReleaseSigningConfig && !fdroidUnsigned


android {
    namespace = "com.readrops.app"

    if (useReleaseSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.droidrops.app"

        versionCode = 26
        versionName = "2.2.5"

        testInstrumentationRunner = "com.readrops.app.ReadropsTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            // Keep native function names and source locations for Play Console crash reports.
            ndk {
                debugSymbolLevel = "FULL"
            }

            if (useReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true

            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        create("beta") {
            initWith(getByName("release"))

            applicationIdSuffix = ".beta"
            signingConfig = signingConfigs.getByName("debug")
        }

        configureEach {
            val shouldSource = name == "debug" || name == "beta"
            val values = mapOf("url" to "https://", "login" to "", "password" to "")
            val accounts = listOf("local", "nextcloud_news", "freshrss", "fever", "greader")

            accounts.forEach { account ->
                values.forEach { (param, default) ->
                    val key = "debug.$account.$param"
                    val value = if (shouldSource) props.getProperty(key, default) else default
                    resValue("string", key, value)
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":db"))

    coreLibraryDesugaring(libs.jdk.desugar)

    implementation(libs.corektx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.palette)
    implementation(libs.workmanager)
    implementation(libs.encrypted.preferences)
    implementation(libs.datastore)
    implementation(libs.browser)
    implementation(libs.splashscreen)
    implementation(libs.preferences)


    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.bundles.voyager)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.coil)

    implementation(libs.bundles.coroutines)

    implementation(libs.bundles.room)
    implementation(libs.bundles.paging)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    implementation(libs.aboutlibraries.composem3)
    implementation(libs.jsoup)
    implementation(libs.colorpicker)

    implementation(libs.autofill)
    implementation(libs.template)
    implementation(libs.slf4j.android)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit4)

    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.kointest)
    androidTestImplementation(libs.okhttp.mockserver)
    androidTestImplementation(libs.coil.test)
    androidTestImplementation(libs.workmanager.test)
}
