
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.jib)
    `maven-publish`
    application
    alias(libs.plugins.k18n)
    alias(libs.plugins.gradleversions)
}

jib {
    System.setProperty("jib.console", "plain")
    from {
        image = "eclipse-temurin:26"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
        }
    }
    container {
        mainClass = "de.tectoast.emolga.MainKt"
        jvmFlags = listOf(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
            "-Dlogback.configurationFile=logback.xml",
            "-Xmx2G",
        )
        ports = listOf("58700", "58701", "5005")
        volumes = listOf("/logs", "/logback.xml")
        user = "1000:1000"
    }
}

application {
    mainClass.set("de.tectoast.emolga.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_26
    withSourcesJar()
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xexplicit-backing-fields")
            freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        }
    }
}

group = "de.tectoast"
version = "4.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)

    // Logging
    implementation(libs.bundles.logging)

    // JDA
    implementation(libs.jda)
    implementation(libs.jda.ktx)

    // Google
    implementation(libs.bundles.google)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.annotations)

    // Database
    // MySQL
    implementation(libs.bundles.database)

    // Ktor
    implementation(libs.bundles.ktor)

    // Utils
    implementation(libs.jsoup)

    // Testing
    testImplementation(libs.bundles.testing)

}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(project.layout.buildDirectory.dir("generated/k18n"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("kotest.framework.config.fqn", "de.tectoast.emolga.KotestProjectConfig")
}


publishing {
    publications {
        create<MavenPublication>("emolga") {
            from(components["java"])
        }
    }
}
