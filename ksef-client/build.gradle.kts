plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    checkstyle
    pmd
    id("com.github.spotbugs") version "6.0.26"
    id("com.diffplug.spotless") version "6.25.0"
}

description = "Java SDK for the Polish National e-Invoicing System (KSeF) REST API v2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
    withSourcesJar()
    withJavadocJar()
}

// Centralised version coordinates — mirrors the pre-Gradle ksef-client/pom.xml
// <properties> block.
val jakartaXmlBindVersion = "4.0.2"
val slf4jVersion = "2.0.16"
val jspecifyVersion = "1.0.0"
val bouncycastleVersion = "1.80"
val dssVersion = "6.3"
val zxingVersion = "3.5.3"
val junitVersion = "5.11.4"
val wiremockVersion = "3.12.1"
val mockitoVersion = "5.14.2"

dependencies {
    // Generated REST + XML models — own modules, cached as JAR deps so that
    // editing handwritten SDK code does not retrigger XJC or OpenAPI generation.
    api(project(":ksef-xml-models"))
    api(project(":ksef-rest-models"))

    // JAXB API surfaced through ksef-xml-models; pull the runtime in here
    // for ksef-client's own marshal/unmarshal callers.
    api("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")

    // Crypto (AES-GCM, RSA-OAEP, ECDH)
    api("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
    api("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")

    // XAdES signing
    api("eu.europa.ec.joinup.sd-dss:dss-xades:$dssVersion")
    api("eu.europa.ec.joinup.sd-dss:dss-token:$dssVersion")
    api("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons:$dssVersion")

    // QR code
    api("com.google.zxing:core:$zxingVersion")
    api("com.google.zxing:javase:$zxingVersion")

    // Logging
    api("org.slf4j:slf4j-api:$slf4jVersion")

    // Null-safety annotations (JSpecify, ADR-017)
    compileOnly("org.jspecify:jspecify:$jspecifyVersion")
    testCompileOnly("org.jspecify:jspecify:$jspecifyVersion")

    // Test
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---------- Resources: bundle SDK metadata files into the published JAR ----------
//
// XSDs themselves are bundled by ksef-xml-models; ksef-client only adds the
// license + third-party notices required by AGPL §5(a) / Apache 2.0 §4.4 /
// LGPL 2.1.

tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("..")) {
        into("META-INF")
        include("LICENSE.txt", "THIRD-PARTY-NOTICES.md")
    }
}

// ---------- Compile + test ----------

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xmx4g", "-XX:+HeapDumpOnOutOfMemoryError")
    finalizedBy(tasks.jacocoTestReport)
}

// ---------- License headers ----------

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        // Inline header so Spotless inserts a real Java block comment; the
        // bare LICENSE-HEADER.txt is plain text and would be written verbatim
        // (no wrappers) — broken Java.
        licenseHeader(
            """
            /*
             * Copyright (c) 2026 Tomasz Zurawski
             * SPDX-License-Identifier: AGPL-3.0-only
             */
            """.trimIndent()
        )
    }
}

// ---------- Static analysis ----------

checkstyle {
    toolVersion = "10.20.1"
    configFile = rootProject.file("checkstyle.xml")
    sourceSets = listOf(
        project.sourceSets.main.get(),
        project.sourceSets.test.get(),
    )
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/module-info.java")
}

pmd {
    toolVersion = "7.7.0"
    ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
    ruleSets = emptyList()
    isIgnoreFailures = false
}

// PMD + SpotBugs + Checkstyle only analyse main source — the Maven setup
// did the same; test files use a different style and the gates are not
// meant to police them.
tasks.named("pmdTest") { enabled = false }
tasks.named("spotbugsTest") { enabled = false }
tasks.named("checkstyleTest") { enabled = false }

spotbugs {
    toolVersion.set("4.8.6")
    excludeFilter.set(rootProject.file("spotbugs-exclude.xml"))
    onlyAnalyze.set(listOf("io.github.mgrtomaszzurawski.ksef.sdk.-"))
}

// ---------- JaCoCo coverage gates ----------
//
// Bundle-level floor: 70% INSTRUCTION / 75% METHOD (post-PR21 lowering;
// F-2 follow-up restores to 75% / 80%). Per-class METHOD=1.00 ratchet on
// every domain.*.builder.*Builder and every domain.*.*Client (PLAN A.9).
// Generated modules are separate Gradle projects so their classes do not
// show up in this report at all — no client.*/xml.* exclude needed.

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            includes = listOf(
                "io.github.mgrtomaszzurawski.ksef.sdk.domain.*.builder.*Builder",
                "io.github.mgrtomaszzurawski.ksef.sdk.domain.*.builder.*Builder\$*",
                "io.github.mgrtomaszzurawski.ksef.sdk.domain.*.*Client",
            )
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ---------- Javadoc ----------
//
// Maven Central javadoc-jar must document only the JPMS-exported public
// surface. Internal SDK plumbing (sdk.internal.*) excluded here; generated
// code lives in separate modules and is not on this module's source path.

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
        addStringOption("Xdoclint:none", "-quiet")
    }
    // Suppress strict checks — javadoc errors caused by missing @param tags
    // or @link references inside SDK code should not block the build at this
    // stage. The Maven setup ran with the same lenient configuration.
    isFailOnError = false
}

// ---------- Maven Central publication ----------

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("KSeF Client")
                description.set(project.description)
                url.set("https://github.com/mgrtomaszzurawski/ksef-java-sdk")
                licenses {
                    license {
                        name.set("GNU Affero General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("mgrtomaszzurawski")
                        name.set("Tomasz Zurawski")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/mgrtomaszzurawski/ksef-java-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com/mgrtomaszzurawski/ksef-java-sdk.git")
                    url.set("https://github.com/mgrtomaszzurawski/ksef-java-sdk")
                }
            }
        }
    }
}

signing {
    setRequired({ project.hasProperty("release") })
    sign(publishing.publications["mavenJava"])
}
