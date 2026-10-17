// Sluer - root build file.
//
// AGP 9.x compiles Kotlin through its built-in Kotlin support, so we do NOT
// apply the org.jetbrains.kotlin.android plugin anywhere. AGP 9.4.1 bundles
// KGP 2.2.10; the classpath entry below upgrades it to 2.4.10 so it matches
// the Compose compiler plugin version used by the app module.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
