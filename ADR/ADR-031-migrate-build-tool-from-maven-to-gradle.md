# ADR-031: Migrate build tool from Maven to Gradle

**Status:** Accepted
**Date:** 2026-05-10

## Context

The project has been on Maven from inception. As the SDK has grown to four
modules (`ksef-client`, `ksef-demo`, `ksef-examples`, `ksef-jpms-consumer`)
and the `ksef-client` module produces ~3300 sources from XSD/OpenAPI inputs
plus ~360 hand-written sources, developer-iteration wall time has become the
dominant cost of any work cycle.

Concrete pain points observed across the 22-PR API redesign sequence and
verified empirically during the 2026-05-10 build-cycle audit:

- `mvn clean verify` takes 14–18 min cold; `clean` invalidates every
  generator cache so XJC and OpenAPI generator always re-run.
- Maven's compiler plugin re-compiles all 3357 sources when any one source
  changes (no per-class dependency graph).
- Maven Surefire re-runs all 751 tests on every invocation regardless of
  whether the inputs to a given test changed.
- Six XJC executions run serially within a single plugin instance.
- `license-maven-plugin` `format` goal re-scans 460 hand-written files even
  when none changed (~37 s on the audit run).
- `mvn install` is required between `ksef-client` changes and
  `mvn exec:java -pl ksef-demo` because Maven's reactor does not propagate
  `target/classes` to dependent modules at run-time without an installed
  artifact.
- Per-invocation JVM startup overhead — the `mvn` CLI is not daemon-backed
  by default.
- Full-clean builds also produce false-positive failures: a missing
  `assertTrue` import in `PefInvoiceTest` compiled fine with Maven's
  incremental test-compiler plugin during the prior session because the test
  had been disabled before; once it was re-enabled in PR #69 the import was
  needed but cold-clean was the only way to detect the regression. The PR
  `/review` cycle did not flag it because verify was running incrementally.

A 2026-05-10 plan listed seven Maven-bound workarounds for these classes of
problems (multi-module split of generated code, demo classpath bypass,
quick-smoke demo mode, drop-`clean` in iteration loop, JaCoCo skip flag,
parallel test forks, install Maven Daemon). All seven are working around
properties of Maven's lifecycle. None is a fundamental fix; together they
make the build harder to reason about and produce a Maven configuration
that drifts further from default conventions every iteration.

## Decision

Migrate the build tool from Apache Maven 3.9.x to Gradle 8.x with the
Kotlin DSL. Single change for all four modules; no transitional period
running both build systems in parallel.

The migration removes:

- Root `pom.xml`, `ksef-client/pom.xml`, `ksef-demo/pom.xml`,
  `ksef-examples/pom.xml`, `ksef-jpms-consumer/pom.xml`.

Adds:

- Root `settings.gradle.kts` declaring the four subprojects.
- Root `build.gradle.kts` with shared properties + version + repositories.
- One `build.gradle.kts` per subproject mirroring its current Maven
  configuration (plugins, dependencies, test config, publication where
  applicable).
- `gradle/wrapper/gradle-wrapper.{jar,properties}` and `gradlew` wrapper
  scripts so the project builds with a pinned Gradle version on any
  developer machine without prior Gradle installation.

## Consequences

### Solved by Gradle defaults (zero or near-zero configuration)

1. XJC and OpenAPI generator tasks produce up-to-date checks based on input
   hashing; unchanged spec files mean the task is skipped.
2. Local build cache survives `./gradlew clean`; cached outputs from
   matching input hashes are reused even after a clean.
3. Incremental Java compilation maintains per-class dependency graph;
   editing one source recompiles only that file plus dependents, not all
   3357 files.
4. Test caching: tests for which neither inputs nor classpath changed are
   reported as `UP-TO-DATE` and skipped.
5. Gradle daemon is on by default; no per-invocation JVM startup cost
   between commands.
6. Reactor / composite builds resolve project dependencies from
   `build/classes` directly; no `install` step needed before running the
   demo.
7. Configuration on demand: only the configured projects relevant to the
   requested task are loaded into memory.
8. Built-in `--profile` flag produces an HTML report of per-task timings;
   no SLF4J prefix + manual log analysis required.
9. Per-task incremental support is built into the SpotBugs / PMD /
   Checkstyle Gradle plugins (the Maven equivalents do not have this).
10. License header tasks are incremental and only touch changed files.

### Solved by single-flag / one-line configuration

11. Parallel project execution with `org.gradle.parallel=true` in
    `gradle.properties`.
12. Parallel test forks with `tasks.test { maxParallelForks = N }`.
13. Per-task JaCoCo agent skip via task DSL where intermediate-iteration
    speed matters.
14. Continuous build with `./gradlew build --continuous` re-runs the build
    on every saved file, eliminating manual invocation between fixes.

### Not addressed by this migration (orthogonal problems)

- Cold cost of generating ~750 classes from PEF UBL XSDs is still high
  (the XJC step itself takes 22 s per UBL schema). This is a property of
  the XSD complexity, not the build tool.
- Runtime memory cost of the JAXBContext at first XSD validation
  (~300–600 MB transient peak) is consumer-side, addressed by F-7 strategy
  decision.
- KSeF API latency in live demo runs is gated by the network and the
  remote service.
- The UBL JAXB unmarshal element-name resolution issue (F-1) is JAXB
  configuration, not a build tool concern.

### Migration costs

- ~600 LOC of `pom.xml` content across five files is replaced by an
  equivalent `build.gradle.kts` set of comparable size.
- Familiarity: developers (and any reviewer agents) need to read Gradle
  task DSL instead of Maven phase bindings.
- Maven Central publishing is configured via
  `io.github.gradle-nexus.publish-plugin` plus the built-in
  `maven-publish` and `signing` plugins; the Sonatype Central Portal flow
  is identical.
- The pre-existing GPG signing configuration in the Maven `release`
  profile is replicated in the Gradle `signing` plugin block; no key or
  account change required.
- JPMS module-info.java files are unchanged; Gradle handles them via
  `java { modularity.inferModulePath = true }`.
- WireMock, Mockito, JUnit, JaCoCo, ZXing, Bouncy Castle, EU DSS — all
  consumed as standard Maven coordinates, identical artifacts.

### Reversibility

Git history preserves the entire Maven configuration. If Gradle
substantively underdelivers, `git revert` of the migration commit returns
the project to its prior Maven state.

## Alternatives considered

- **Keep Maven, apply seven workarounds.** Rejected. Each workaround is a
  partial fix to one class of problem. Together they accumulate
  configuration debt and require ongoing maintenance. Gradle's defaults
  cover the same problem classes once and for all without project-level
  configuration. The cost of switching once is lower than the cost of
  maintaining seven workarounds across all future PRs.
- **Stay on Maven, install Maven Daemon (mvnd).** Rejected. mvnd reduces
  per-invocation startup overhead but does not address incremental compile,
  test caching, build cache survival, generator UP-TO-DATE checks, or any
  of the other classes. It is one of seven workarounds, not a substitute
  for the rest.
- **Migrate to Bazel.** Rejected. Bazel's correctness model is stronger
  than Gradle's but the migration cost (BUILD files, dependency rules,
  rules_jvm_external) is much higher and the gain over Gradle is small for
  a four-module Java project. Bazel is the right tool for monorepos with
  many languages or hermetic-build requirements; this project is neither.

## Notes

This decision is fully covered by SemVer pre-1.0 latitude — there is no
Maven Central artefact, no consumer build configuration to preserve. The
only externally visible changes are the build commands documented in
`README.md` and `CLAUDE.md`, which are updated as part of the migration.
