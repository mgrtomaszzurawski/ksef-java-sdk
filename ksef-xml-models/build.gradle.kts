// XSD → JAXB code generation for the KSeF invoice/UPO/auth schemas.
//
// Lives in its own module so the generated 2576-class UBL world (PEF +
// PEF_KOR) is compiled and cached as a JAR dependency consumed by
// ksef-client. As long as the XSDs and bindings under xsd/ do not change,
// this module is UP-TO-DATE and contributes nothing to ksef-client's
// incremental compile cycle.

plugins {
    `java-library`
}

description = "JAXB-generated XML models for KSeF schemas (FA2, FA3, PEF, PEF_KOR, UPO, AUTH)"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    modularity.inferModulePath.set(true)
}

val jakartaXmlBindVersion = "4.0.2"
val jaxbRuntimeVersion = "4.0.5"

dependencies {
    api("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
}

// XJC tool classpath — used by the per-schema JavaExec tasks below.
val xjcTool: Configuration by configurations.creating
dependencies {
    xjcTool("org.glassfish.jaxb:jaxb-xjc:$jaxbRuntimeVersion")
    xjcTool("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    xjcTool("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaXmlBindVersion")
}

// XSD source-of-truth lives under the legacy ksef-client/xsd/ tree — keep
// the path during the module-extraction migration; can move to this
// module's own xsd/ in a follow-up.
val xsdRoot = layout.projectDirectory.dir("../ksef-client/xsd")

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
    schema = xsdRoot.file("FA/schemat_FA(2)_v1-0E.xsd").let { providers.provider { it } },
    bindingFile = xsdRoot.file("FA/fa2-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa2",
    outputDirName = "xjc-fa2",
)

val xjcFa3 = registerXjc(
    name = "xjcFa3",
    schema = xsdRoot.file("FA/schemat_FA(3)_v1-0E.xsd").let { providers.provider { it } },
    bindingFile = xsdRoot.file("FA/fa3-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa3",
    outputDirName = "xjc-fa3",
)

val xjcPef = registerXjc(
    name = "xjcPef",
    schema = xsdRoot.file("PEF/Schemat_PEF(3)_v2-1.xsd").let { providers.provider { it } },
    bindingFile = xsdRoot.file("PEF/pef-bindings.xjb").let { providers.provider { it } },
    outputDirName = "xjc-pef",
)

val xjcPefKor = registerXjc(
    name = "xjcPefKor",
    schema = xsdRoot.file("PEF/Schemat_PEF_KOR(3)_v2-1.xsd").let { providers.provider { it } },
    bindingFile = xsdRoot.file("PEF/pef-kor-bindings.xjb").let { providers.provider { it } },
    outputDirName = "xjc-pef-kor",
)

val xjcUpo = registerXjc(
    name = "xjcUpo",
    schemaDir = xsdRoot.dir("upo").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.upo",
    outputDirName = "xjc-upo",
)

val xjcAuth = registerXjc(
    name = "xjcAuth",
    schemaDir = xsdRoot.dir("auth").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.auth",
    outputDirName = "xjc-auth",
)

sourceSets.main {
    java.srcDirs(
        layout.buildDirectory.dir("generated-sources/xjc-fa2"),
        layout.buildDirectory.dir("generated-sources/xjc-fa3"),
        layout.buildDirectory.dir("generated-sources/xjc-pef"),
        layout.buildDirectory.dir("generated-sources/xjc-pef-kor"),
        layout.buildDirectory.dir("generated-sources/xjc-upo"),
        layout.buildDirectory.dir("generated-sources/xjc-auth"),
    )
}

tasks.named("compileJava") {
    dependsOn(xjcFa2, xjcFa3, xjcPef, xjcPefKor, xjcUpo, xjcAuth)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    // Generated UBL code triggers many compiler warnings (raw types, deprecation);
    // they are not actionable since the source is generated, so silence them.
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
}

// Bundle XSDs into the JAR so consumers can load them at runtime
// (KsefXmlValidator does this for FA2/FA3 and PEF schemas).
tasks.named<ProcessResources>("processResources") {
    from(xsdRoot) {
        include(
            "FA/schemat_FA(*)_*.xsd",
            "FA/bazowe/*.xsd",
            "PEF/Schemat_PEF(*)_*.xsd",
            "PEF/Schemat_PEF_KOR(*)_*.xsd",
            "PEF/bazowe/*.xsd",
        )
        into("xsd")
    }
}
