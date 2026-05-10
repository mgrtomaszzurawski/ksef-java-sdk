plugins {
    `java-library`
}

description = """
    Tiny named module that imports the KSeF SDK as a real JPMS consumer.
    Has its own module-info.java with `requires
    io.github.mgrtomaszzurawski.ksef`. If a public type returned by the
    SDK lives in a non-exported package, this module fails to compile —
    catching the regression `PublicApiSurfaceTest` cannot (e.g. `AuthSession`
    leak in PR46). Not published to Maven Central.
""".trimIndent()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(project(":ksef-client"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
