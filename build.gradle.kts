import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kVersion = "1.9.23"
plugins {
    val kVersion = "1.9.23"
    kotlin("jvm") version kVersion
    kotlin("plugin.serialization") version kVersion
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt").version("1.23.0")
    application
}


application {
    mainClass.set("de.tectoast.emolga.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_20
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
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
            jvmTarget = "20"
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

val ktorVersion = "2.3.8"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // JDA
    implementation("net.dv8tion:JDA:5.0.0-beta.23")
    implementation("club.minnced:jda-ktx:0.11.0-beta.20")

    // Google
    implementation("com.google.apis:google-api-services-sheets:v4-rev20230815-2.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240327-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240417-2.0.0")

    // Database
    // MySQL
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.49.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.49.0")
    // MongoDB
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.11.0")
    implementation("org.litote.kmongo:kmongo-id-serialization:4.11.0")

    // Ktor
    ktor()

    // Utils
    implementation("org.jsoup:jsoup:1.17.2")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("io.kotest:kotest-framework-datatest:5.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

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
