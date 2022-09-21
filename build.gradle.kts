plugins {
    kotlin("jvm") version "1.7.10"
    id("org.javamodularity.moduleplugin") version "1.8.12"

    id("signing")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.6.0"
}

group = "net.yakclient"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//    mavenLocal()
    maven {
        name = "Durgan McBroom GitHub Packages"
        url = uri("https://maven.pkg.github.com/durganmcbroom/artifact-resolver")
        credentials {
            username = project.findProperty("gpr.user") as? String ?: throw IllegalArgumentException("Need a Github package registry username!")
            password = project.findProperty("gpr.key") as? String ?: throw IllegalArgumentException("Need a Github package registry key!")
        }
    }
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
    implementation("io.arrow-kt:arrow-core:1.1.2")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("net.yakclient:archives:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.durganmcbroom:artifact-resolver:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.durganmcbroom:artifact-resolver-simple-maven:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("net.yakclient:common-util:1.0-SNAPSHOT") {
        isChanging = true
    }
    testImplementation(kotlin("test"))
}

task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

task<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("boot-maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = "boot"

            pom {
                name.set("Boot")
                description.set("YakClient's Boot module")
                url.set("https://github.com/yakclient/boot")

                packaging = "jar"

                developers {
                    developer {
                        id.set("Chestly")
                        name.set("Durgan McBroom")
                    }
                }

                licenses {
                    license {
                        name.set("GNU General Public License")
                        url.set("https://opensource.org/licenses/gpl-license")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yakclient/boot")
                    developerConnection.set("scm:git:ssh://github.com:yakclient/boot.git")
                    url.set("https://github.com/yakclient/boot")
                }
            }
        }
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "net.yakclient"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://repo.yakclient.net/snapshots")
        }
    }

    publishing {
        repositories {
            if (!project.hasProperty("maven-user") || !project.hasProperty("maven-pass")) return@repositories

            maven {
                val repo = if (project.findProperty("isSnapshot") == "true") "snapshots" else "releases"

                isAllowInsecureProtocol = true

                url = uri("http://repo.yakclient.net/$repo")

                credentials {
                    username = project.findProperty("maven-user") as String
                    password = project.findProperty("maven-pass") as String
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }

    kotlin {
        explicitApi()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        testImplementation(kotlin("test"))
    }

    tasks.compileKotlin {
        destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())

        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.compileJava {
        targetCompatibility = "17"
        sourceCompatibility = "17"
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
//
//
//tasks.test {
//    useJUnitPlatform()
//}
//
//tasks.compileJava {
//    targetCompatibility = "17"
//    sourceCompatibility = "17"
//}
//
//tasks.compileJava {
//    sourceCompatibility = "17"
//    targetCompatibility = "17"
//}
//
//tasks.compileKotlin {
//    destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())
//    kotlinOptions.jvmTarget = "17"
//}
//
//tasks.compileTestKotlin {
//    kotlinOptions.jvmTarget = "17"
//}