import org.jetbrains.kotlin.gradle.tasks.*

val kotlinVersion = "1.3.30"
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.30"
    id("org.jetbrains.dokka") version "0.9.18"
    `maven-publish`
    signing
}

val kotlinxCoroutinesJdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.1.1"
val jupiterEngine = "org.junit.jupiter:junit-jupiter-engine:5.0.0"
val websocketApi = "javax.websocket:javax.websocket-api:1.0"
val jetty = "org.eclipse.jetty.websocket:javax-websocket-server-impl:9.4.16.v20190411"
val undertow = "io.undertow:undertow-websockets-jsr:2.0.20.Final"
val ktorVersion = "1.1.3"

val ktorNetwork = "io.ktor:ktor-network:$ktorVersion"
val ktorClient = "io.ktor:ktor-client:$ktorVersion"
val ktorServerCore = "io.ktor:ktor-server-core:$ktorVersion"
val ktorServerCio = "io.ktor:ktor-server-cio:$ktorVersion"
val ktorClientCio = "io.ktor:ktor-client-cio:$ktorVersion"

repositories {
    jcenter() // needed for dokka
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            if (true) { // $todo: needed because not all kotlin dependencies are yet in sync
                force(
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
                    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
                )
            }
        }
    }

    sourceSets {
        main { java.srcDir("main") }
        test { java.srcDir("test") }
    }

    dependencies {
        compile(kotlin("stdlib-jdk8"))
        testCompile(kotlin("test-junit5"))
        testRuntimeOnly(jupiterEngine)
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint")
        options.compilerArgs.add("-parameters")
        options.encoding = "US-ASCII"
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
            javaParameters = true
            allWarningsAsErrors = true
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.jar {
        manifest {
            attributes["Automatic-Module-Name"] = "ch.softappeal." + project.name.replace("-", ".")
        }
    }

    tasks.dokka {
        outputFormat = "html"
        reportUndocumented = false
        outputDirectory = "$buildDir/dokka"
    }

    if (project.name.startsWith("yass")) {
        group = "ch.softappeal.yass"

        tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        tasks.register<Jar>("dokkaJar") {
            dependsOn(tasks.dokka)
            archiveClassifier.set("javadoc")
            from("$buildDir/dokka")
        }

        val publication = "mavenJava"

        publishing {
            publications {
                create<MavenPublication>(publication) {
                    from(components["java"])
                    artifact(tasks["sourcesJar"])
                    artifact(tasks["dokkaJar"])
                    pom {
                        name.set(project.name)
                        description.set("Yet Another Service Solution")
                        url.set("https://github.com/softappeal/yass")
                        licenses { license { name.set("BSD-3-Clause") } }
                        scm { url.set("https://github.com/softappeal/yass") }
                        organization { name.set("softappeal GmbH Switzerland") }
                        developers { developer { name.set("Angelo Salvade") } }
                    }
                }
            }
            repositories {
                maven {
                    if (true) {
                        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                        credentials {
                            username = (project.findProperty("ossrhUsername") as String?) ?: "dummy"
                            password = (project.findProperty("ossrhPassword") as String?) ?: "dummy"
                        }
                    } else
                        url = uri("$buildDir/repos")
                }
            }
        }

        signing {
            sign(publishing.publications[publication])
        }
    }
}

tasks.clean {
    doLast {
        ant.withGroovyBuilder {
            "delete"("dir" to ".", "includes" to "*/out/", "includeemptydirs" to true)
        }
    }
}

val yass = project(":kotlin:yass") {
    dependencies {
        testCompile(kotlinxCoroutinesJdk8)
    }
}

val yassTestRuntime = yass.sourceSets.test.get().runtimeClasspath

val yassTransportKtor = project(":kotlin:yass-transport-ktor") {
    dependencies {
        compile(yass)
        compile(ktorNetwork)
        compile(ktorClient)
        compile(ktorServerCore)
        testCompile(ktorServerCio)
        testCompile(ktorClientCio)
        testCompile(yassTestRuntime)
    }
}

val yassTransportWs = project(":kotlin:yass-transport-ws") {
    dependencies {
        compile(yass)
        compileOnly(websocketApi)
        testCompile(yassTestRuntime)
        testCompile(jetty)
        testCompile(undertow)
    }
}

val yassGenerate = project(":kotlin:yass-generate") {
    dependencies {
        compile(yass)
    }
}

project(":kotlin:tutorial-kotlin") {
    dependencies {
        compile(yassTransportKtor)
        // compile("ch.softappeal.yass:yass-transport-ktor:x.y.z")
        compile(ktorServerCio)
        compile(ktorClientCio)
    }
}

project(":kotlin:tutorial") {
    dependencies {
        compile(yassTransportWs)
        compile(yassGenerate)
        compile(jetty)
        compile(undertow)
    }
}
