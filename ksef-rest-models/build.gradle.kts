// OpenAPI-generated REST client models (`*Raw` types + low-level `*Api`
// classes). Lives in its own module so the 303-class generation pass is
// cached as a JAR consumed by ksef-client; ksef-client only rebuilds these
// when the openapi/open-api.json spec changes.

plugins {
    `java-library`
    id("org.openapi.generator") version "7.12.0"
}

description = "OpenAPI-generated REST models (*Raw) for KSeF API v2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

val jacksonVersion = "2.18.2"
val jacksonNullableVersion = "0.2.6"
val jakartaAnnotationVersion = "2.1.1"

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    api("org.openapitools:jackson-databind-nullable:$jacksonNullableVersion")
    api("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationVersion")
}

// Spec file lives under the legacy ksef-client/openapi/ tree during module
// extraction — keep the path stable; can move to this module's own
// openapi/ directory in a follow-up.
val specFile = layout.projectDirectory.file("../ksef-client/openapi/open-api.json")

openApiGenerate {
    generatorName.set("java")
    library.set("native")
    inputSpec.set(specFile.asFile.absolutePath)
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

// Cache the OpenAPI generator output across `clean build` runs — keyed on
// the spec file + generator config. Without this the 303-class generation
// re-runs every time the build/ directory is wiped, even when the spec is
// unchanged.
tasks.named("openApiGenerate") {
    inputs.file(specFile).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.buildDirectory.dir("generated-sources/openapi"))
    outputs.cacheIf { true }
}

sourceSets.main {
    java.srcDirs(layout.buildDirectory.dir("generated-sources/openapi/src/main/java"))
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
}
