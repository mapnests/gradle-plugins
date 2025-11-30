plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    // Add Android Gradle Plugin to access LibraryExtension
    implementation("com.android.tools.build:gradle:8.3.2") // <-- match your project AGP version
}

group = "com.mapnests"
version = "1.0.2"

gradlePlugin {
    website.set("https://github.com/mapnests/gradle-plugins")
    vcsUrl.set("https://github.com/mapnests/gradle-plugins")
    plugins {
        create("configLoader") {
            id = "com.mapnests.config-loader"
            implementationClass = "mapnests.configloader.ConfigLoaderPlugin"
            displayName = "Config Loader Plugin"
            description =
                "A Gradle plugin to load configuration from a JSON file and generate a source file."
            tags.set(listOf("config", "json", "android", "buildconfig"))
        }
    }
}