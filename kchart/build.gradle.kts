plugins {
    id("org.jetbrains.kotlin.plugin.compose").version("2.2.21")
    id("org.jetbrains.compose").version("1.9.3")
    kotlin("jvm").version("2.2.21")
}

repositories {
    mavenCentral()
    google()
}

group = "org.example"
version = "1.0-SNAPSHOT"

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}


dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
}