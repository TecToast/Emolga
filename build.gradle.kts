import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kVersion = "1.7.10"
plugins {
    val kVersion = "1.7.10"
    kotlin("jvm") version kVersion
    kotlin("plugin.serialization") version kVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}


application {
    mainClass.set("de.tectoast.emolga.Main")
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }
    withType<Jar> {
        manifest {
            attributes("Main-Class" to "de.tectoast.emolga.Main", "Class-Path" to "Emolga-all.jar")
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

val ktorVersion = "2.1.3"
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
    "ktor-server-cors"
)

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.eclipse.jetty:jetty-server:11.0.12")
    implementation("org.eclipse.jetty:jetty-util:11.0.12")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-docs:v1-rev20221004-2.0.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20220926-2.0.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    //implementation("org.slf4j:slf4j-simple:1.7.32")

    implementation("ch.qos.logback:logback-classic:1.4.4")
    //implementation("mysql:mysql-connector-java:8.0.29")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.1")
    implementation("com.github.TecToast:Toastilities:96e360b1f8")
    //implementation("com.github.TecToast:JSOLF:a43c3e06c7")
    implementation("net.dv8tion:JDA:5.0.0-alpha.22")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.8")
    implementation("com.github.MinnDevelopment:jda-ktx:-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kVersion")
    implementation("io.ktor:ktor-server-auth-jvm:2.1.3")
    implementation("io.ktor:ktor-server-locations-jvm:2.1.3")
    implementation("io.ktor:ktor-server-core-jvm:2.1.3")
    implementation("io.ktor:ktor-client-core-jvm:2.1.3")
    implementation("io.ktor:ktor-client-apache-jvm:2.1.3")
    implementation("io.ktor:ktor-server-cors-jvm:2.1.3")
    ktor()
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.40.1")
}

fun DependencyHandler.ktor() {
    ktorDependencies.forEach {
        implementation("io.ktor:$it:$ktorVersion")
    }
}
