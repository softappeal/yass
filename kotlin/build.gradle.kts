import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.dokka") version "0.9.17"
    `maven-publish`
    signing
}

val kotlinxCoroutinesJdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.1.1"
val jupiterEngine = "org.junit.jupiter:junit-jupiter-engine:5.0.0"
val websocketApi = "javax.websocket:javax.websocket-api:1.0"
val jetty = "org.eclipse.jetty.websocket:javax-websocket-server-impl:9.4.14.v20181114"
val undertow = "io.undertow:undertow-websockets-jsr:2.0.17.Final"

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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

    configurations.all {
        // $todo resolutionStrategy.failOnVersionConflict()
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

    if (project.name in setOf("yass", "yass-generate", "yass-transport-ws")) {
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
        compile(yass)
        // compile("ch.softappeal.yass:yass:x.y.z")
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
