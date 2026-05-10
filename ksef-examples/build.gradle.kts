plugins {
    `java-library`
}

description = "Compile-time-checked examples in ../examples/ — not published to Maven Central."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":ksef-client"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

// Examples live in ../examples/ as JBang-style standalone scripts. This
// module compiles them with the published ksef-client API on the classpath,
// so any example calling a non-existent method fails the build. README
// discipline (ADR-022).
sourceSets.main {
    java.setSrcDirs(listOf(rootProject.layout.projectDirectory.dir("examples")))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
