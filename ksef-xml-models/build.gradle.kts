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

// XML parser knobs shared across every XJC fork.
//
// jdk.xml.* set to 0 disables the JDK's safe-default XML parser ceilings
// (max-occur 5000, entity-expansion 64000) that the KSeF FA(2)/FA(3)/PEF
// schemas legitimately exceed. The inputs to XJC are trusted spec files
// from CIRFMF/ksef-docs; the unrestricted parser is build-time only and
// does NOT relax runtime XML validation (ADR-029 keeps StAX hardening on
// the consumer side).
private val xjcXmlArgs = listOf(
    "-DentityExpansionLimit=0",
    "-Djdk.xml.entityExpansionLimit=0",
    "-Djdk.xml.maxOccurLimit=0",
    "-Djdk.xml.totalEntitySizeLimit=0",
)

// Heap sizing per XJC fork. The PEF+PEF_KOR consolidated task walks the
// full UBL 2.1 + Polish PEPPOL world (~1300 classes, ~700 MB CodeModel
// peak) so it gets 2 GB. The other four schemas (FA2 ~50 classes, FA3
// ~80 classes, UPO ~3 classes, AUTH ~5 classes) fit comfortably in 1 GB
// — explicit sizing avoids the iter-1 finding where every XJC fork
// claimed 2 GB and parallel runs over-committed memory in CI.
private val XJC_HEAP_LARGE = "-Xmx2g"
private val XJC_HEAP_SMALL = "-Xmx1g"

fun registerXjc(
    name: String,
    schemas: List<Provider<RegularFile>> = emptyList(),
    schemaDir: Provider<Directory>? = null,
    bindingFile: Provider<RegularFile>? = null,
    pkg: String? = null,
    outputDirName: String,
    transitiveSchemaDirs: List<Provider<Directory>> = emptyList(),
    heapArg: String = XJC_HEAP_SMALL,
): TaskProvider<JavaExec> = tasks.register<JavaExec>(name) {
    group = "build"
    description = "Generates JAXB classes for $outputDirName"
    classpath = xjcTool
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    jvmArgs(heapArg)
    jvmArgs(xjcXmlArgs)

    val outputDir = layout.buildDirectory.dir("generated-sources/$outputDirName")
    outputs.dir(outputDir)
    outputs.cacheIf { true }

    // Schema files: explicit schemas listed at config time go in inputs.file()
    // immediately. Directory-based schemas (UPO, AUTH) use a lazy FileCollection
    // so adding a new .xsd to the dir is detected; the schema list passed to
    // XJC at execution is rebuilt fresh from the dir at task-execution time.
    schemas.forEach { inputs.file(it) }
    bindingFile?.let { inputs.file(it) }

    val schemaDirTree = schemaDir?.let { dir ->
        val tree = fileTree(dir).matching { include("*.xsd") }
        inputs.files(tree)
            .withPropertyName("schemaDir-${dir.get().asFile.name}")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        tree
    }

    // Schemas reached transitively via xs:import / xs:include — FA2/FA3
    // import bazowe/StrukturyDanych_v10-0E.xsd; PEF/PEF_KOR import the
    // PEF/bazowe content. Without these as declared inputs, editing a
    // base schema produces stale generated classes that the build cache
    // happily reuses.
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
        // Resolve schema file paths at execution time so a new .xsd added
        // to a schemaDir between configs gets picked up on the next run.
        schemas.forEach { args += it.get().asFile.absolutePath }
        schemaDirTree?.files?.forEach { args += it.absolutePath }
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
    heapArg = XJC_HEAP_LARGE,
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
