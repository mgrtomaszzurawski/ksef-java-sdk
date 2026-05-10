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
// module's own xsd/ in a follow-up. JAXB binding files (.xjb) are
// project-local config (not from CIRFMF/ksef-docs) and now live in
// jaxb-bindings/ inside this module, outside the spec-sacred xsd/.
val xsdRoot = layout.projectDirectory.dir("../ksef-client/xsd")
val bindingsRoot = layout.projectDirectory.dir("jaxb-bindings")

// Heap and XML parser knobs for every XJC fork.
//
// -Xmx2g — PEF / PEF_KOR XJC walks ~1300 generated classes per schema in
// the JVM and emits ~700 MB of CodeModel intermediate state; the JVM
// default heap (~256 MB) reliably OOMs.
//
// jdk.xml.* set to 0 disables the JDK's safe-default XML parser ceilings
// (max-occur 5000, entity-expansion 64000) that the KSeF FA(2)/FA(3)/PEF
// schemas legitimately exceed. The inputs to XJC are trusted spec files
// from CIRFMF/ksef-docs; the unrestricted parser is build-time only and
// does NOT relax runtime XML validation (ADR-029 keeps StAX hardening on
// the consumer side).
private val xjcJvmArgs = listOf(
    "-Xmx2g",
    "-DentityExpansionLimit=0",
    "-Djdk.xml.entityExpansionLimit=0",
    "-Djdk.xml.maxOccurLimit=0",
    "-Djdk.xml.totalEntitySizeLimit=0",
)

fun registerXjc(
    name: String,
    schemas: List<Provider<RegularFile>> = emptyList(),
    schemaDir: Provider<Directory>? = null,
    bindingFile: Provider<RegularFile>? = null,
    pkg: String? = null,
    outputDirName: String,
    transitiveSchemaDirs: List<Provider<Directory>> = emptyList(),
): TaskProvider<JavaExec> = tasks.register<JavaExec>(name) {
    group = "build"
    description = "Generates JAXB classes for $outputDirName"
    classpath = xjcTool
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    jvmArgs(xjcJvmArgs)

    val outputDir = layout.buildDirectory.dir("generated-sources/$outputDirName")
    outputs.dir(outputDir)
    outputs.cacheIf { true }

    val schemaInputs = mutableListOf<File>()
    schemas.forEach { schemaInputs += it.get().asFile }
    if (schemaDir != null) {
        // fileTree(dir).matching{} is a lazy FileCollection — Gradle
        // resolves it at task-execution time, so adding new .xsd files to
        // the directory invalidates the configuration cache properly.
        // Using .files here would freeze the list at config time.
        val tree = fileTree(schemaDir).matching { include("*.xsd") }
        inputs.files(tree)
            .withPropertyName("schemaDir-${schemaDir.get().asFile.name}")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        schemaInputs += tree.files
    }
    schemaInputs.filter { schemaDir == null || it.parentFile.name != schemaDir.get().asFile.name }
        .forEach { inputs.file(it) }
    bindingFile?.let { inputs.file(it.get().asFile) }

    // Schemas reached transitively via xs:import / xs:include — FA2/FA3
    // import bazowe/StrukturyDanych_v10-0E.xsd; PEF/PEF_KOR import the
    // PEF/bazowe content; UPO imports from upo/. Without these as
    // declared inputs, editing a base schema produces stale generated
    // classes that the build cache happily reuses.
    transitiveSchemaDirs.forEach { dir ->
        inputs.files(fileTree(dir).matching { include("**/*.xsd") })
            .withPropertyName("transitiveSchemas-${dir.get().asFile.name}")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }

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

private val faBazowe = xsdRoot.dir("FA/bazowe").let { providers.provider { it } }
private val pefBazowe = xsdRoot.dir("PEF/bazowe").let { providers.provider { it } }

val xjcFa2 = registerXjc(
    name = "xjcFa2",
    schemas = listOf(xsdRoot.file("FA/schemat_FA(2)_v1-0E.xsd").let { providers.provider { it } }),
    bindingFile = bindingsRoot.file("FA/fa2-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa2",
    outputDirName = "xjc-fa2",
    transitiveSchemaDirs = listOf(faBazowe),
)

val xjcFa3 = registerXjc(
    name = "xjcFa3",
    schemas = listOf(xsdRoot.file("FA/schemat_FA(3)_v1-0E.xsd").let { providers.provider { it } }),
    bindingFile = bindingsRoot.file("FA/fa3-bindings.xjb").let { providers.provider { it } },
    pkg = "io.github.mgrtomaszzurawski.ksef.xml.fa3",
    outputDirName = "xjc-fa3",
    transitiveSchemaDirs = listOf(faBazowe),
)

// Single XJC invocation over BOTH PEF (Invoice) and PEF_KOR (CreditNote)
// schemas. The shared UBL 2.1 base + Polish PEPPOL extensions land once
// in xml.ubl.* (cac, cbc, ext, sig, qdt, udt, sigcac, sigcbc, xades132,
// xades141, xmldsig, ccts, cacpl, cbcpl). Only the two top-level
// schemas keep distinct root packages (xml.pef.InvoiceType,
// xml.pefkor.CreditNoteType). Replaces the prior two-task setup that
// generated 1288 + 1288 = 2576 duplicated classes; ~1300 unique now.
val xjcPefUbl = registerXjc(
    name = "xjcPefUbl",
    schemas = listOf(
        xsdRoot.file("PEF/Schemat_PEF(3)_v2-1.xsd").let { providers.provider { it } },
        xsdRoot.file("PEF/Schemat_PEF_KOR(3)_v2-1.xsd").let { providers.provider { it } },
    ),
    bindingFile = bindingsRoot.file("PEF/pef-ubl-bindings.xjb").let { providers.provider { it } },
    outputDirName = "xjc-pef-ubl",
    transitiveSchemaDirs = listOf(pefBazowe),
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
        layout.buildDirectory.dir("generated-sources/xjc-pef-ubl"),
        layout.buildDirectory.dir("generated-sources/xjc-upo"),
        layout.buildDirectory.dir("generated-sources/xjc-auth"),
    )
}

tasks.named("compileJava") {
    dependsOn(xjcFa2, xjcFa3, xjcPefUbl, xjcUpo, xjcAuth)
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
