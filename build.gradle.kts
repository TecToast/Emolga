
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kVersion = "2.1.21"
plugins {
    val kVersion = "2.1.20"
    kotlin("jvm") version kVersion
    kotlin("plugin.serialization") version kVersion
    id("maven-publish")
    id("com.google.cloud.tools.jib") version "3.4.5"
    application
}

jib {
    System.setProperty("jib.console", "plain")
    from {
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
        }
    }
    to {
        image = "tectoast/emolga"
    }
    container {
        mainClass = "de.tectoast.emolga.MainKt"
        jvmFlags = listOf(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
            "-Dlogback.configurationFile=logback.xml"
        )
        ports = listOf("58700", "58701", "5005")
        volumes = listOf("/logs", "/logback.xml")
    }
}

application {
    mainClass.set("de.tectoast.emolga.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
}

group = "de.tectoast"
version = "3.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val exposedVersion = "0.61.0"
val ktorVersion = "3.1.2"
val ktorDependencies = listOf(
    // Client
    "ktor-client-core",
    "ktor-client-cio",
    "ktor-serialization-kotlinx-json",
    "ktor-client-content-negotiation",
    // Server
    "ktor-server-core",
    "ktor-server-cio",
    "ktor-server-auth",
    "ktor-server-sessions",
    "ktor-server-content-negotiation",
    "ktor-serialization-kotlinx-json",
    "ktor-server-cors",
    "ktor-server-call-logging",
    "ktor-server-call-logging-jvm"

)

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.0-0.6.x-compat")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // JDA
    implementation("net.dv8tion:JDA:5.6.1")
    implementation("club.minnced:jda-ktx:0.12.0")

    // Google
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20250511-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20250422-2.0.0")

    // Database
    // MySQL
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-migration:$exposedVersion")
    // MongoDB
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:5.2.1")
    implementation("org.litote.kmongo:kmongo-id-serialization:5.2.1")

    // Ktor
    ktor()

    // Utils
    implementation("org.jsoup:jsoup:1.20.1")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.0.M4")
    testImplementation("io.kotest:kotest-assertions-core:6.0.0.M4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

fun DependencyHandler.ktor() {
    ktorDependencies.forEach {
        implementation("io.ktor:$it:$ktorVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("emolga") {
            from(components["java"])
        }
    }
}
