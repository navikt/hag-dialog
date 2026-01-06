
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "no.nav.helsearbeidsgiver.AppKt"
}

group = "no.nav.helsearbeidsgiver"

version = "1.0.0"

repositories {
    val githubPassword: String by project

    mavenCentral()
    maven {
        setUrl("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

dependencies {
    // Interne avhengigheter
    val dialogportenClientVersion: String by project
    val utilsVersion: String by project
    implementation("no.nav.helsearbeidsgiver:dialogporten-client:$dialogportenClientVersion")
    implementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")

    // Eksterne avhengigheter
    val bakgrunnsjobbVersion: String by project
    val exposedVersion: String by project
    val flywayVersion: String by project
    val hikariVersion: String by project
    val kafkaVersion: String by project
    val ktorVersion: String by project
    val logbackEncoderVersion: String by project
    val logbackVersion: String by project
    val microMeterVersion: String by project
    val postgresqlVersion: String by project
    val unleashVersion: String by project

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.getunleash:unleash-client-java:$unleashVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$microMeterVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")
    implementation("no.nav.helsearbeidsgiver:hag-bakgrunnsjobb:$bakgrunnsjobbVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    // Test dependencies
    val kotestVersion: String by project
    val mockkVersion: String by project
    val testcontainersVersion: String by project
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation(testFixtures("no.nav.helsearbeidsgiver:utils:$utilsVersion"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
