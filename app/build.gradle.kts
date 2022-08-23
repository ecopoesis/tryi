plugins {
    kotlin("jvm") version "1.6.10"
    application
    `jvm-test-suite`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("io.arrow-kt:arrow-core:1.0.1")

    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}
