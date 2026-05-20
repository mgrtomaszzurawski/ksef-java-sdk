// OpenAPI-generated REST client models (`*Raw` types + low-level `*Api`
// classes). Lives in its own module so the 303-class generation pass is
// cached as a JAR consumed by ksef-client; ksef-client only rebuilds these
// when the openapi/open-api.json spec changes.

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    id("org.openapi.generator") version "7.12.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

description = "OpenAPI-generated REST models — companion to unofficial ksef-client (preview)"

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
// unchanged. doFirst clears stale generated files before each run so a
// removed schema definition does not leave orphan *Raw classes behind.
tasks.named("openApiGenerate") {
    inputs.file(specFile).withPathSensitivity(PathSensitivity.RELATIVE)
    val outDir = layout.buildDirectory.dir("generated-sources/openapi")
    outputs.dir(outDir)
    outputs.cacheIf { true }
    doFirst {
        outDir.get().asFile.also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }
}

sourceSets.main {
    java.srcDirs(layout.buildDirectory.dir("generated-sources/openapi/src/main/java"))
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// vanniktech's configure(JavaLibrary(...)) registers the `sourcesJar` task
// lazily during configuration; `javadoc` task exists eagerly. Wire both to
// depend on openApiGenerate so Gradle 8 doesn't flag implicit-dependency
// validation errors. `matching` skips silently when the task isn't there.
tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn("openApiGenerate")
}
tasks.named("javadoc") {
    dependsOn("openApiGenerate")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
}

// Generated *Raw sources contain non-ASCII (Polish field-name comments).
// Javadoc default encoding is US-ASCII; force UTF-8 + silence the
// thousands of lint warnings on generated code.
tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// ---------- Maven Central publication (companion to ksef-client) ----------

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))
    publishToMavenCentral(automaticRelease = false)
    // Signing is required by Maven Central but breaks the local smoke test
    // (publishToMavenLocal) when no GPG key is configured. Gate it on a
    // property — release pipeline passes -PsigningEnabled=true.
    if (providers.gradleProperty("signingEnabled").orNull == "true") {
        signAllPublications()
    }

    coordinates(
        groupId = "io.github.mgrtomaszzurawski",
        artifactId = "ksef-rest-models",
        version = project.version.toString()
    )

    pom {
        name.set("KSeF REST Models")
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
                email.set("mgrtomaszzurawski@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/mgrtomaszzurawski/ksef-java-sdk.git")
            developerConnection.set("scm:git:ssh://github.com/mgrtomaszzurawski/ksef-java-sdk.git")
            url.set("https://github.com/mgrtomaszzurawski/ksef-java-sdk")
        }
    }
}
