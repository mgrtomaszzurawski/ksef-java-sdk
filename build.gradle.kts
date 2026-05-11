// Aggregate root build for the KSeF Java SDK.
//
// Each subproject (ksef-xml-models, ksef-rest-models, ksef-client,
// ksef-demo, ksef-examples, ksef-jpms-consumer) owns its own
// build.gradle.kts. The root holds only project metadata that applies
// uniformly to every module, plus the manual-run OWASP dependency-check
// task that replaces the deleted Maven security-scan profile.

plugins {
    base
    id("org.owasp.dependencycheck") version "11.1.1"
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.projectKey", "ksef-java-sdk")
        property("sonar.projectName", "KSeF Java SDK")
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}

// Manual-run OWASP scan, NOT bound to the default verify cycle to keep
// CI fast. Release engineer invokes `./gradlew dependencyCheckAggregate`
// before each tag and reviews the HTML/JSON report. Fails the build on
// CVSS ≥ 7 (HIGH/CRITICAL).
dependencyCheck {
    failBuildOnCVSS = 7.0f
    skipConfigurations = listOf("testRuntimeClasspath", "testCompileClasspath")
    suppressionFile = rootProject.file("owasp-suppressions.xml").absolutePath
    formats = listOf("HTML", "JSON")
}
