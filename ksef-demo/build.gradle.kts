plugins {
    `java-library`
    application
    id("com.diffplug.spotless") version "6.25.0"
}

description = "Demo application exercising the KSeF Java SDK against the live demo server"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val slf4jVersion = "2.0.16"
val logbackVersion = "1.5.6"
val jacksonVersion = "2.18.2"
val junitVersion = "5.11.4"
val bouncycastleVersion = "1.80"

dependencies {
    implementation(project(":ksef-client"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    // BouncyCastle — required by SelfSignedCerts utility for the TEST-env
    // auto-auth flow (X509v3CertificateBuilder, etc.). Declared directly so
    // demo compile does not depend on BC being a transitive of ksef-client.
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("io.github.mgrtomaszzurawski.ksef.sample.DemoApp")
}

// `mvn exec:java -Ddemo.mode=AUTH_SAFE` equivalent. Gradle resolves the
// ksef-client dependency from the reactor's build/classes — no install step
// required.
tasks.named<JavaExec>("run") {
    systemProperty("demo.mode", project.findProperty("demo.mode") ?: "READ_ONLY")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        licenseHeaderFile(rootProject.file("LICENSE-HEADER.txt"))
    }
}
