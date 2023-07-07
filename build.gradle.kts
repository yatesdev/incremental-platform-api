group = "com.incremental"
plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.incremental.api.IncrementalPlatformApiKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    tasks {
        compileKotlin {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
        compileTestKotlin {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs += "-Xjvm-default=all"
            }
        }
    }
}

tasks {
    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set(null as String?)
        archiveVersion.set(null as String?)
        mergeServiceFiles()
        dependsOn(distZip, distTar)
    }

    test {
        useJUnitPlatform()
    }
}

val kotlinVersion: String by project
val http4kVersion: String by project
val junitVersion: String by project
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.http4k:http4k-client-okhttp:$http4kVersion")
    implementation("org.http4k:http4k-contract:$http4kVersion")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson-yaml:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.http4k:http4k-graphql:$http4kVersion")
    implementation("org.http4k:http4k-multipart:$http4kVersion")
    implementation("org.http4k:http4k-security-oauth:$http4kVersion")
    implementation("org.http4k:http4k-server-undertow:$http4kVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("com.expediagroup:graphql-kotlin-schema-generator:6.5.3")
    testImplementation("org.http4k:http4k-testing-approval:$http4kVersion")
    testImplementation("org.http4k:http4k-testing-hamkrest:$http4kVersion")
    testImplementation("org.http4k:http4k-testing-kotest:$http4kVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
