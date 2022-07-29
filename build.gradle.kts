import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }
    withType<ShadowJar> {
        manifest {
            attributes("Main-Class" to "de.tectoast.emolga.Main")
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

group = "de.tectoast"
version = "1.0-SNAPSHOT"

val gprUser: String by project
val gprPassword: String by project

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
    }
    maven {
        url = uri("https://maven.pkg.github.com/TecToast/Toastilities")
        credentials {
            username = gprUser
            password = gprPassword
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/TecToast/JSOLF")
        credentials {
            username = gprUser
            password = gprPassword
        }
    }
    maven("https://jitpack.io/")
}
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
    implementation("org.eclipse.jetty:jetty-server:11.0.11")
    implementation("org.eclipse.jetty:jetty-util:11.0.11")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-docs:v1-rev20220609-1.32.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220620-1.32.1")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20220719-1.32.1")
    implementation("org.jsoup:jsoup:1.15.2")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    //implementation("org.slf4j:slf4j-simple:1.7.32")

    implementation("ch.qos.logback:logback-classic:1.3.0-alpha16")
    //implementation("mysql:mysql-connector-java:8.0.29")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.0")
    implementation("de.tectoast:toastilities:2.2")
    implementation("de.tectoast:jsolf:1.2")
    implementation("net.dv8tion:JDA:5.0.0-alpha.17")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("com.github.minndevelopment:jda-ktx:03b07e7")
}