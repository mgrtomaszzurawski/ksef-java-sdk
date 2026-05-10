// Aggregate root build for the KSeF Java SDK.
//
// Each subproject (ksef-client, ksef-demo, ksef-examples, ksef-jpms-consumer)
// owns its own build.gradle.kts. The root holds only project metadata that
// applies uniformly to every module.

plugins {
    base
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
