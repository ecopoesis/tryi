plugins {
    kotlin("jvm") version "1.6.10"
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-math3:3.6.1")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest()
        }
    }
}

application {
    // Define the main class for the application.
    mainClass.set("org.miker.tryi.AppKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}
