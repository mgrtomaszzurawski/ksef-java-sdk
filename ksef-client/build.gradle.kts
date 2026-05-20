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
    // Generated XML models — JAXB-generated invoice types are surfaced
    // through the *Invoice / *InvoiceDocument escape hatch (unsafeJaxbView /
    // toJaxbCopy), so consumers compile against them transitively.
    api(project(":ksef-xml-models"))
    // Generated REST models — *Raw types are internal per ADR-005;
    // PublicApiSurfaceTest enforces zero leakage into the public surface.
    implementation(project(":ksef-rest-models"))

    // JAXB API: surfaced through ksef-xml-models JAXB roots that consumers
    // can reach via unsafeJaxbView() / toJaxbCopy().
    api("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")

    // Crypto (AES-GCM, RSA-OAEP, ECDH) — used internally for invoice
    // encryption and ECDSA-P → IEEE-P1363 signature reformatting in
    // QrSigningService. No BouncyCastle type appears in a public signature.
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")

    // XAdES signing via DSS — wrapped behind SigningService; no DSS type
    // is exposed on the public surface.
    implementation("eu.europa.ec.joinup.sd-dss:dss-xades:$dssVersion")
    implementation("eu.europa.ec.joinup.sd-dss:dss-token:$dssVersion")
    implementation("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons:$dssVersion")

    // QR code (ZXing) — wrapped behind QrCodeService; consumers receive
    // byte[] PNGs, never raw ZXing types.
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.google.zxing:javase:$zxingVersion")

    // Logging — kept as api so consumers configure their own SLF4J
    // backend without re-declaring the API dependency.
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
    // Two heap dominators run concurrently in tests:
    //   - UBL JAXBContext (~150-300 MB post-consolidation)
    //   - Xerces FA(3) XSD content-model DFA (~300-600 MB transient peak
    //     during Fa3InvoiceBuilder validation tests)
    // Plus JaCoCo instrumentation overhead. 2 GB per fork OOMs on
    // XSDFACM.calcFollowList; 4 GB clears with margin. 2 forks × 4 GB
    // = 8 GB committed when tests run alone, inside a workstation budget.
    maxParallelForks = 2
    forkEvery = 0L
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
    reports {
        // SonarQube reads the XML report via sonar.coverage.jacoco.xmlReportPaths
        // (configured at the root sonar {} block). Enable explicitly because
        // the JaCoCo plugin disables it by default in some Gradle versions.
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            // Pre-PR21 thresholds restored — F-2 closed via
            // DecryptedInvoiceSyncSpliteratorTest, KsefExceptionSafeResponseBodyTest,
            // and four InvoiceDocument flat-accessor tests covering the
            // read-side of ADR-030.
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
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
        // R1-7: group module-summary pages into four named sections so
        // the alphabetised default does not interleave `sdk`,
        // `sdk.common`, `sdk.config`, and the eight `sdk.domain.*`
        // trees in one long list.
        group("Entry point", "io.github.mgrtomaszzurawski.ksef.sdk")
        group("Configuration", "io.github.mgrtomaszzurawski.ksef.sdk.config")
        group(
            "Common types",
            "io.github.mgrtomaszzurawski.ksef.sdk.common:io.github.mgrtomaszzurawski.ksef.sdk.exception:io.github.mgrtomaszzurawski.ksef.sdk.crypto"
        )
        group("Operational domain APIs", "io.github.mgrtomaszzurawski.ksef.sdk.domain*")
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

// Internal-aware Javadoc — generates the full HTML tree including the
// non-exported sdk.internal.* packages so SDK developers can browse the
// transport, crypto, batch, signing, and other plumbing classes locally.
// NOT used for the Maven Central javadoc-jar (which keeps the JPMS-exported
// surface only via the standard `javadoc` task above).
//
// Usage:  ./gradlew :ksef-client:javadocAll
// Output: ksef-client/build/docs/javadoc-internal/index.html
tasks.register<Javadoc>("javadocAll") {
    group = "documentation"
    description = "Generates Javadoc HTML for the full source tree, including sdk.internal.*."
    source = sourceSets.main.get().allJava
    classpath = sourceSets.main.get().compileClasspath
    setDestinationDir(layout.buildDirectory.dir("docs/javadoc-internal").get().asFile)
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        // JPMS-aware tooling defaults to showing only exported packages /
        // public+protected members. `--show-packages=all` surfaces every
        // package in the package listing (including non-exported
        // sdk.internal.*), and `-private` surfaces package-private + private
        // members so internal collaborators are clickable from internal call
        // sites.
        addStringOption("-show-packages", "all")
        addBooleanOption("private", true)
        // Group labels MUST be ASCII-only. JDK 17's javadoc crashes with
        // "error: cannot read Input length = 1" on non-ASCII bytes in
        // group titles (e.g. an em-dash U+2014), producing an empty
        // output directory. The crash is silent unless isFailOnError=true.
        group("Entry point", "io.github.mgrtomaszzurawski.ksef.sdk")
        group("Configuration", "io.github.mgrtomaszzurawski.ksef.sdk.config")
        group(
            "Common types",
            "io.github.mgrtomaszzurawski.ksef.sdk.common:io.github.mgrtomaszzurawski.ksef.sdk.exception:io.github.mgrtomaszzurawski.ksef.sdk.crypto"
        )
        group("Operational domain APIs", "io.github.mgrtomaszzurawski.ksef.sdk.domain*")
        group("Internal - runtime + client", "io.github.mgrtomaszzurawski.ksef.sdk.internal*")
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
        addStringOption("Xdoclint:none", "-quiet")
    }
    // Fail loudly on real javadoc errors. The previous isFailOnError=false
    // masked the em-dash crash and shipped an empty javadoc-internal/
    // directory while the build reported SUCCESS.
    isFailOnError = true
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
