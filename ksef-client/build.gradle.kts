plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    checkstyle
    pmd
    id("com.github.spotbugs") version "6.0.26"
    id("org.openapi.generator") version "7.12.0"
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

// Centralised version coordinates — mirrors ksef-client/pom.xml properties block.
val jacksonVersion = "2.18.2"
val jacksonNullableVersion = "0.2.6"
val jakartaAnnotationVersion = "2.1.1"
val jakartaXmlBindVersion = "4.0.2"
val jaxbRuntimeVersion = "4.0.5"
val slf4jVersion = "2.0.16"
val jspecifyVersion = "1.0.0"
val bouncycastleVersion = "1.80"
val dssVersion = "6.3"
val zxingVersion = "3.5.3"
val junitVersion = "5.11.4"
val wiremockVersion = "3.12.1"
val mockitoVersion = "5.14.2"

dependencies {
    // JSON serialization
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    api("org.openapitools:jackson-databind-nullable:$jacksonNullableVersion")

    // Annotations
    api("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationVersion")

    // JAXB (XML binding for XSD-generated invoice models)
    api("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")

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

// XJC tool classpath — used by the per-schema JavaExec tasks below.
// Kept off the main + test classpaths because XJC ships compiler internals
// that we don't want bleeding into production code.
val xjcTool: Configuration by configurations.creating
dependencies {
    xjcTool("org.glassfish.jaxb:jaxb-xjc:$jaxbRuntimeVersion")
    xjcTool("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    xjcTool("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")
}

// ---------- Code generation ----------

// OpenAPI: REST API models from openapi/open-api.json
openApiGenerate {
    generatorName.set("java")
    library.set("native")
    inputSpec.set(layout.projectDirectory.file("openapi/open-api.json").asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated-sources/openapi").map { it.asFile.absolutePath })
    apiPackage.set("io.github.mgrtomaszzurawski.ksef.client.api")
    modelPackage.set("io.github.mgrtomaszzurawski.ksef.client.model")
    modelNameSuffix.set("Raw")
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    skipOverwrite.set(false)
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useJakartaEe" to "true",
    ))
}

// JAXB XJC: invoice XML models from XSD schemas. Six independent generations
// targeting six distinct packages — matches the Maven jaxb-maven-plugin
// executions (xjc-fa2, xjc-fa3, xjc-pef, xjc-pef-kor, xjc-upo, xjc-auth).
//
// Each task is a plain JavaExec invoking com.sun.tools.xjc.XJCFacade with
// the canonical XJC CLI arguments. Inputs (xsd + binding files) and output
// directory are declared so Gradle's UP-TO-DATE check skips the task when
// nothing changed — this is the build-cycle win that motivated the migration.
fun registerXjc(
    name: String,
    schema: Provider<RegularFile>? = null,
    schemaDir: Provider<Directory>? = null,
    bindingFile: Provider<RegularFile>? = null,
    pkg: String? = null,
    outputDirName: String,
): TaskProvider<JavaExec> = tasks.register<JavaExec>(name) {
    group = "build"
    description = "Generates JAXB classes for $outputDirName"
    classpath = xjcTool
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    // KSeF FA(2)/FA(3)/PEF schemas have content models exceeding the JDK
    // XML parser's default 5000-node ceiling. Disable both occurrence and
    // entity expansion limits for the XJC JVM (build-time only — production
    // runtime uses StAX with its own hardening per ADR-029).
    jvmArgs(
        "-DentityExpansionLimit=0",
        "-Djdk.xml.entityExpansionLimit=0",
        "-Djdk.xml.maxOccurLimit=0",
        "-Djdk.xml.totalEntitySizeLimit=0",
    )

    val outputDir = layout.buildDirectory.dir("generated-sources/$outputDirName")
    outputs.dir(outputDir)
    outputs.cacheIf { true }

    val schemaInputs = mutableListOf<File>()
    if (schema != null) {
        schemaInputs += schema.get().asFile
    }
    if (schemaDir != null) {
        schemaInputs += fileTree(schemaDir).matching { include("*.xsd") }.files
    }
    schemaInputs.forEach { inputs.file(it) }
    bindingFile?.let { inputs.file(it.get().asFile) }

    doFirst {
        outputDir.get().asFile.also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    argumentProviders.add {
        val args = mutableListOf<String>()
        args += listOf("-d", outputDir.get().asFile.absolutePath)
        args += "-extension"
        args += "-no-header"
        if (pkg != null) {
            args += listOf("-p", pkg)
        }
        bindingFile?.let {
            args += listOf("-b", it.get().asFile.absolutePath)
        }
        args += schemaInputs.map { it.absolutePath }
        args
    }
}

val xjcFa2 = registerXjc(
    name = "xjcFa2",
    schema = layout.projectDirectory.file("xsd/FA/schemat_FA(2)_v1-0E.xsd").let { providers.provider { it } },
    bindingFile = layout.projectDirectory.file("xsd/FA/fa2-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa2",
    outputDirName = "xjc-fa2",
)

val xjcFa3 = registerXjc(
    name = "xjcFa3",
    schema = layout.projectDirectory.file("xsd/FA/schemat_FA(3)_v1-0E.xsd").let { providers.provider { it } },
    bindingFile = layout.projectDirectory.file("xsd/FA/fa3-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa3",
    outputDirName = "xjc-fa3",
)

val xjcPef = registerXjc(
    name = "xjcPef",
    schema = layout.projectDirectory.file("xsd/PEF/Schemat_PEF(3)_v2-1.xsd").let { providers.provider { it } },
    bindingFile = layout.projectDirectory.file("xsd/PEF/pef-bindings.xjb").let { providers.provider { it } },
    outputDirName = "xjc-pef",
)

val xjcPefKor = registerXjc(
    name = "xjcPefKor",
    schema = layout.projectDirectory.file("xsd/PEF/Schemat_PEF_KOR(3)_v2-1.xsd").let { providers.provider { it } },
    bindingFile = layout.projectDirectory.file("xsd/PEF/pef-kor-bindings.xjb").let { providers.provider { it } },
    outputDirName = "xjc-pef-kor",
)

val xjcUpo = registerXjc(
    name = "xjcUpo",
    schemaDir = layout.projectDirectory.dir("xsd/upo").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.upo",
    outputDirName = "xjc-upo",
)

val xjcAuth = registerXjc(
    name = "xjcAuth",
    schemaDir = layout.projectDirectory.dir("xsd/auth").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.auth",
    outputDirName = "xjc-auth",
)

// Wire generated sources into the main source set.
sourceSets.main {
    java.srcDirs(
        layout.buildDirectory.dir("generated-sources/openapi/src/main/java"),
        layout.buildDirectory.dir("generated-sources/xjc-fa2"),
        layout.buildDirectory.dir("generated-sources/xjc-fa3"),
        layout.buildDirectory.dir("generated-sources/xjc-pef"),
        layout.buildDirectory.dir("generated-sources/xjc-pef-kor"),
        layout.buildDirectory.dir("generated-sources/xjc-upo"),
        layout.buildDirectory.dir("generated-sources/xjc-auth"),
    )
}

tasks.named("compileJava") {
    dependsOn(
        "openApiGenerate",
        xjcFa2, xjcFa3, xjcPef, xjcPefKor, xjcUpo, xjcAuth,
    )
}

// ---------- Resources: bundle XSDs into the published JAR ----------

tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory) {
        include(
            "xsd/FA/schemat_FA(*)_*.xsd",
            "xsd/FA/bazowe/*.xsd",
            "xsd/PEF/Schemat_PEF(*)_*.xsd",
            "xsd/PEF/Schemat_PEF_KOR(*)_*.xsd",
            "xsd/PEF/bazowe/*.xsd",
        )
    }
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
        licenseHeaderFile(rootProject.file("LICENSE-HEADER.txt"))
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

tasks.withType<Pmd>().configureEach {
    // Exclude generated sources — only handwritten SDK code is gated.
    exclude("**/generated-sources/**")
    exclude("io/github/mgrtomaszzurawski/ksef/client/**")
    exclude("io/github/mgrtomaszzurawski/ksef/xml/**")
}

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

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "io/github/mgrtomaszzurawski/ksef/client/**",
                    "io/github/mgrtomaszzurawski/ksef/xml/**",
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "io/github/mgrtomaszzurawski/ksef/client/**",
                    "io/github/mgrtomaszzurawski/ksef/xml/**",
                )
            }
        })
    )
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
// surface. Internal SDK plumbing (sdk.internal.*), generated OpenAPI client
// (client.*), and generated JAXB XML packages (xml.*) are implementation
// detail. Excluded here; the JavadocPackageGateTest regression-tests this
// list against actual generated output.

tasks.javadoc {
    exclude(
        "io/github/mgrtomaszzurawski/ksef/sdk/internal/**",
        "io/github/mgrtomaszzurawski/ksef/client/**",
        "io/github/mgrtomaszzurawski/ksef/xml/**",
    )
    (options as StandardJavadocDocletOptions).apply {
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
        addStringOption("Xdoclint:none", "-quiet")
    }
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

// GPG signing — only active when `release` Gradle property is set, mirroring
// the Maven `release` profile gate. Avoids a compulsory GPG dependency on
// every developer build.
signing {
    setRequired({ project.hasProperty("release") })
    sign(publishing.publications["mavenJava"])
}
