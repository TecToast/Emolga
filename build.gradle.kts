import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kVersion = "2.1.0"
plugins {
    val kVersion = "2.1.0"
    kotlin("jvm") version kVersion
    kotlin("plugin.serialization") version kVersion
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
    application
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
    withType<Jar> {
        manifest {
            attributes("Main-Class" to "de.tectoast.emolga.MainKt", "Class-Path" to "Emolga-all.jar")
        }
        exclude("natives/linux-arm/libconnector.so")
    }
    withType<ShadowJar> {
        exclude("de/tectoast/emolga/**")
        archiveVersion.set("")
    }
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
    /*withType(KotlinCompile::class.java) {
        dependsOn("clean")
    }*/
}

group = "de.tectoast"
version = "3.0"

val gprUser: String by project
val gprPassword: String by project

repositories {
    mavenLocal()
    mavenCentral()
}

val exposedVersion = "0.57.0"
val ktorVersion = "2.3.12"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // JDA
    implementation("net.dv8tion:JDA:5.2.2")
    implementation("club.minnced:jda-ktx:0.12.0")

    // Google
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250106-2.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20241206-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20250101-2.0.0")

    // Database
    // MySQL
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    // MongoDB
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:5.2.1")
    implementation("org.litote.kmongo:kmongo-id-serialization:5.2.0")

    // Ktor
    ktor()

    // Utils
    implementation("org.jsoup:jsoup:1.18.3")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.0.M1")
    testImplementation("io.kotest:kotest-assertions-core:6.0.0.M1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

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
