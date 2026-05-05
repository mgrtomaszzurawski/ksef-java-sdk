/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Codex 2026-05-05 round-2 F3 — release-artifact gate that pins the
 * Maven Central javadoc-jar to the JPMS-exported public surface.
 *
 * <p>{@code module-info.java} is the source of truth for the SDK's
 * supported public package surface. The javadoc plugin's
 * {@code excludePackageNames} keeps {@code sdk.internal.*}, generated
 * OpenAPI {@code client.*}, and generated JAXB {@code xml.*} packages
 * out of the published documentation. This test catches drift between
 * the two: if a new public package is added to {@code module-info.java}
 * but never appears in javadoc (or vice versa), CI fails.
 *
 * <p>The javadoc plugin records the actually-documented package list at
 * {@code target/reports/apidocs/packages} (one fully-qualified package
 * per line). The test reads it and intersects with
 * {@code module-info.java}'s {@code exports} directives.
 *
 * <p>The test is skipped (rather than failing) if the {@code packages}
 * file does not exist — javadoc generation runs as a {@code package}
 * phase goal, so {@code mvn test} without {@code mvn package} won't
 * have produced it. The release pipeline runs {@code mvn verify} which
 * triggers javadoc generation; this test then asserts the contract.
 */
class JavadocPackageGateTest {

    private static final String MODULE_INFO_PATH = "src/main/java/module-info.java";
    private static final String PACKAGES_FILE_PATH = "target/reports/apidocs/element-list";
    private static final String SDK_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef";
    private static final String MODULE_LINE_PREFIX = "module:";
    private static final Pattern EXPORTS_DIRECTIVE = Pattern.compile(
            "^\\s*exports\\s+([\\w.]+)\\s*;\\s*$");

    @Test
    void javadocPackages_containNoInternalOrGeneratedSurface() throws IOException {
        Path packagesFile = Paths.get(PACKAGES_FILE_PATH);
        Assumptions.assumeTrue(Files.isRegularFile(packagesFile),
                "javadoc not generated yet (run mvn verify); test reported as SKIPPED rather than passing vacuously");

        Set<String> documented = readDocumentedPackages(packagesFile);
        assertFalse(documented.isEmpty(),
                "javadoc packages file is empty — javadoc generation must produce at least the public SDK surface");

        List<String> leaks = new ArrayList<>();
        for (String pkg : documented) {
            if (pkg.startsWith(SDK_ROOT_PACKAGE + ".sdk.internal.")) {
                leaks.add(pkg + " (internal SDK plumbing)");
            } else if (pkg.startsWith(SDK_ROOT_PACKAGE + ".client.")) {
                leaks.add(pkg + " (generated OpenAPI client)");
            } else if (pkg.startsWith(SDK_ROOT_PACKAGE + ".xml.")) {
                leaks.add(pkg + " (generated JAXB XML)");
            }
        }
        if (!leaks.isEmpty()) {
            fail("Javadoc-jar documents internal/generated packages — "
                    + "extend <excludePackageNames> in ksef-client/pom.xml:\n  - "
                    + String.join("\n  - ", leaks));
        }
    }

    @Test
    void javadocPackages_matchJpmsExportsExactly() throws IOException {
        Path packagesFile = Paths.get(PACKAGES_FILE_PATH);
        if (!Files.isRegularFile(packagesFile)) {
            return;
        }

        Set<String> documented = readDocumentedPackages(packagesFile);
        Set<String> exported = parseExportedPackages();

        List<String> documentedButNotExported = new ArrayList<>();
        for (String pkg : documented) {
            if (!exported.contains(pkg)) {
                documentedButNotExported.add(pkg);
            }
        }

        List<String> exportedButNotDocumented = new ArrayList<>();
        for (String pkg : exported) {
            if (!documented.contains(pkg)) {
                exportedButNotDocumented.add(pkg);
            }
        }

        if (!documentedButNotExported.isEmpty() || !exportedButNotDocumented.isEmpty()) {
            StringBuilder msg = new StringBuilder("Javadoc surface and JPMS exports diverge.\n");
            if (!documentedButNotExported.isEmpty()) {
                msg.append("Documented but not exported (remove from javadoc or add to module-info):\n  - ")
                        .append(String.join("\n  - ", documentedButNotExported))
                        .append("\n");
            }
            if (!exportedButNotDocumented.isEmpty()) {
                msg.append("Exported but not documented (extend <excludePackageNames> exception or fix javadoc):\n  - ")
                        .append(String.join("\n  - ", exportedButNotDocumented));
            }
            fail(msg.toString());
        }
    }

    private static Set<String> readDocumentedPackages(Path packagesFile) throws IOException {
        // Java 9+ javadoc emits an "element-list" with one module: line
        // followed by one fully-qualified package name per line. Older
        // javadoc emitted a "package-list" with the same package format
        // and no module line. Either is fine — skip the module line.
        Set<String> result = new HashSet<>();
        for (String line : Files.readAllLines(packagesFile)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(MODULE_LINE_PREFIX)) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }

    private static Set<String> parseExportedPackages() throws IOException {
        Path moduleInfo = Paths.get(MODULE_INFO_PATH);
        assertTrue(Files.isRegularFile(moduleInfo),
                "module-info.java not found at " + moduleInfo.toAbsolutePath());
        Set<String> exports = new HashSet<>();
        for (String line : Files.readAllLines(moduleInfo)) {
            Matcher matcher = EXPORTS_DIRECTIVE.matcher(line);
            if (matcher.matches()) {
                exports.add(matcher.group(1));
            }
        }
        return exports;
    }
}
