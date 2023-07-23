import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kVersion = "1.9.0"
plugins {
    val kVersion = "1.9.0"
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
    /*withType(KotlinCompile::class.java) {
        dependsOn("clean")
    }*/
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

group = "de.tectoast"
version = "1.0-JSONTEST"

val gprUser: String by project
val gprPassword: String by project

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
    }
    maven("https://jitpack.io/")
}

val ktorVersion = "2.2.4"
val ktorDependencies = listOf(
    // Client
    "ktor-client-core",
    "ktor-client-cio",
    "ktor-serialization-kotlinx-json",
    "ktor-client-content-negotiation",
    // Server
    "ktor-server-core",
    "ktor-server-netty",
    "ktor-server-auth",
    "ktor-server-sessions",
    "ktor-server-content-negotiation",
    "ktor-serialization-kotlinx-json",
    "ktor-server-cors",
    "ktor-server-call-logging",
    "ktor-server-call-logging-jvm"

)

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20230526-2.0.0")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    //implementation("org.slf4j:slf4j-simple:1.7.32")

    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    //implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.github.TecToast:Toastilities:96e360b1f8")
    //implementation("com.github.TecToast:JSOLF:a43c3e06c7")
    implementation("net.dv8tion:JDA:5.0.0-beta.12")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    implementation("com.github.MinnDevelopment:jda-ktx:0.10.0-beta.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kVersion")
    ktor()
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
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
