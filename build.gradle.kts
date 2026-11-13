// Top-level build file.
// AGP 9 ships with built-in Kotlin. Declaring the Kotlin Gradle plugin here (apply false)
// puts Kotlin 2.4.0 on the build classpath so the built-in Kotlin and the Compose compiler
// plugin both resolve to the same, newer version.
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}
