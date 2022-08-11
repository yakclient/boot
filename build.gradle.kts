plugins {
    kotlin("jvm") version "1.7.10"
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "net.yakclient"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
//    maven {
//        name = "Durgan McBroom GitHub Packages"
//        url = uri("https://maven.pkg.github.com/durganmcbroom/artifact-resolver")
//        credentials {
//            username = project.findProperty("gpr.user") as? String ?: throw IllegalArgumentException("Need a Github package registry username!")
//            password = project.findProperty("gpr.key") as? String ?: throw IllegalArgumentException("Need a Github package registry key!")
//        }
//    }
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://repo.yakclient.net/snapshots")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    api("net.yakclient:archives:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.durganmcbroom:artifact-resolver:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.durganmcbroom:artifact-resolver-simple-maven:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.6")
    implementation("net.yakclient:common-util:1.0-SNAPSHOT") {
        isChanging = true
    }
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    targetCompatibility = "17"
    sourceCompatibility = "17"
}

tasks.compileKotlin {
    destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())
    kotlinOptions.jvmTarget = "17"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "17"
}

kotlin {
    explicitApi()
}