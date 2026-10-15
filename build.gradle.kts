import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.android.agp)
        classpath(libs.kotlin.kgp)
        classpath(libs.jacoco)
    }
}

plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    jacoco
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            configureAndroid(this)
        }
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            configureAndroid(this)
        }
    }

    plugins.withId("com.android.application") {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                allWarningsAsErrors.set(
                    providers.gradleProperty("warningsAsErrors")
                        .map(String::toBoolean)
                        .orElse(false)
                )
            }
        }
    }

    plugins.withId("com.android.library") {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                allWarningsAsErrors.set(
                    providers.gradleProperty("warningsAsErrors")
                        .map(String::toBoolean)
                        .orElse(false)
                )
            }
        }
    }
}

fun configureAndroid(extension: ApplicationExtension) {
    extension.compileSdk = 36
    extension.buildToolsVersion = "36.0.0"
    extension.defaultConfig.minSdk = 24
    extension.defaultConfig.targetSdk = 36
    extension.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

fun configureAndroid(extension: LibraryExtension) {
    extension.compileSdk = 36
    extension.buildToolsVersion = "36.0.0"
    extension.defaultConfig.minSdk = 24
    extension.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

jacoco {
    toolVersion = "0.8.7"
}


tasks.register<JacocoReport>("jacocoFullReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for the debug build"

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    dependsOn(":app:testDebugUnitTest")
    dependsOn(":app:connectedAndroidTest")
    dependsOn(":api:testDebugUnitTest")
    dependsOn(":db:testDebugUnitTest")
    dependsOn(":db:connectedAndroidTest")

    val excludeFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "android/**/*.*"
    )

    classDirectories.setFrom(
        files(
            fileTree("${project.rootDir}/app/build/intermediates/javac/debug") { exclude(excludeFilter) },
            fileTree("${project.rootDir}/app/build/tmp/kotlin-classes/debug") { exclude(excludeFilter) }
        ),
        fileTree("${project.rootDir}/api/build/tmp/kotlin-classes/debug")  { exclude(excludeFilter) },
        fileTree("${project.rootDir}/db/build/tmp/kotlin-classes/debug")  { exclude(excludeFilter) },
    )

    val coverageSourceDirs = listOf(
        "${project.rootDir}/app/src/main/java",
        "${project.rootDir}/api/src/main/java",
        "${project.rootDir}/db/src/main/java",
    )

    additionalSourceDirs.setFrom(files(coverageSourceDirs))
    sourceDirectories.setFrom(files(coverageSourceDirs))

    executionData.setFrom(
        fileTree(project.rootDir) {
            include(
                listOf(
                    "api/build/outputs/unit_test_code_coverage/**/*.exec",
                    "db/build/outputs/unit_test_code_coverage/**/*.exec",
                    "app/build/outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                    "db/build/outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
                )
            )
        }
    )
}
