/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JPMS exports gate (Codex round-9 follow-up): treats {@code module-info.java}
 * as the source of truth for the SDK's public-package surface, asserting:
 *
 * <ol>
 *   <li>every package listed under {@code exports ...} actually exists in
 *       {@code target/classes} and contains at least one public class
 *       (catches stale {@code module-info.java} entries that drift after a
 *       package is renamed or deleted);</li>
 *   <li>every {@code sdk.*} package that contains at least one public class
 *       is either exported by {@code module-info.java} or lives under
 *       {@code sdk.internal.*} (catches forgotten {@code exports} when a
 *       new public package is introduced).</li>
 * </ol>
 *
 * <p>Complements {@link PublicApiSurfaceTest} (which checks that exported
 * types do not leak {@code internal.*} or {@code *Raw} types) and the
 * compile-only {@code JpmsConsumerCompileFixture} downstream module
 * (which exercises the resolved exports against a real consumer
 * {@code module-info}).
 */
class JpmsExportsCoverageTest {

    private static final String MODULE_INFO_PATH = "src/main/java/module-info.java";
    private static final String SDK_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef.sdk";
    private static final String INTERNAL_PACKAGE_PREFIX = SDK_ROOT_PACKAGE + ".internal";
    private static final String CLASSES_DIR = "target/classes";
    private static final String JAVA_PACKAGE_SEPARATOR = ".";
    private static final String FS_SEPARATOR = "/";
    private static final String CLASS_FILE_SUFFIX = ".class";
    private static final String INNER_CLASS_MARKER = "$";
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String PACKAGE_INFO_CLASS = "package-info.class";
    /**
     * Matches {@code exports a.b.c;} declarations. Tolerates surrounding
     * whitespace; the no-target form (without a trailing {@code to ...}) is
     * what consumer-visible exports look like in this module.
     */
    private static final Pattern EXPORTS_DIRECTIVE = Pattern.compile(
            "^\\s*exports\\s+([\\w.]+)\\s*;\\s*$");

    @Test
    void everyExportedPackageHasAtLeastOnePublicClass() throws IOException {
        Set<String> exportedPackages = parseExportedPackages();
        assertFalse(exportedPackages.isEmpty(),
                "module-info.java declared no exports — surface gate is vacuous");

        List<String> empties = new ArrayList<>();
        for (String pkg : exportedPackages) {
            if (!packageHasAnyPublicClass(pkg)) {
                empties.add(pkg);
            }
        }
        if (!empties.isEmpty()) {
            fail("module-info.java exports packages with no public class on the classpath:\n  - "
                    + String.join("\n  - ", empties));
        }
    }

    @Test
    void everyPublicSdkPackageIsExported() throws IOException {
        Set<String> exportedPackages = parseExportedPackages();
        Set<String> publicPackagesWithClasses = scanSdkPackagesContainingPublicClasses();

        List<String> unexported = new ArrayList<>();
        for (String pkg : publicPackagesWithClasses) {
            if (pkg.startsWith(INTERNAL_PACKAGE_PREFIX)) {
                continue;
            }
            if (!exportedPackages.contains(pkg)) {
                unexported.add(pkg);
            }
        }
        if (!unexported.isEmpty()) {
            fail("Found public class(es) in non-exported, non-internal package(s):\n  - "
                    + String.join("\n  - ", unexported)
                    + "\nEither add `exports <pkg>;` to module-info.java or move the class under sdk.internal.*");
        }
    }

    private static Set<String> parseExportedPackages() throws IOException {
        Path moduleInfo = Paths.get(MODULE_INFO_PATH);
        assertTrue(Files.isRegularFile(moduleInfo),
                "module-info.java not found at " + moduleInfo.toAbsolutePath()
                        + " (test must run from ksef-client module root)");

        Set<String> exports = new HashSet<>();
        for (String line : Files.readAllLines(moduleInfo)) {
            Matcher matcher = EXPORTS_DIRECTIVE.matcher(line);
            if (matcher.matches()) {
                exports.add(matcher.group(1));
            }
        }
        return exports;
    }

    private static boolean packageHasAnyPublicClass(String pkg) throws IOException {
        Path packageDir = packageToDir(pkg);
        if (!Files.isDirectory(packageDir)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(packageDir)) {
            return entries.filter(Files::isRegularFile)
                    .filter(JpmsExportsCoverageTest::isClassFile)
                    .filter(JpmsExportsCoverageTest::isNotInfrastructureClassFile)
                    .map(JpmsExportsCoverageTest::pathToClassName)
                    .anyMatch(JpmsExportsCoverageTest::isPublicClass);
        }
    }

    private static Set<String> scanSdkPackagesContainingPublicClasses() throws IOException {
        Path sdkRoot = packageToDir(SDK_ROOT_PACKAGE);
        if (!Files.isDirectory(sdkRoot)) {
            return Set.of();
        }
        Set<String> packages = new HashSet<>();
        try (Stream<Path> walk = Files.walk(sdkRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(JpmsExportsCoverageTest::isClassFile)
                    .filter(JpmsExportsCoverageTest::isNotInfrastructureClassFile)
                    .map(JpmsExportsCoverageTest::pathToClassName)
                    .filter(JpmsExportsCoverageTest::isPublicClass)
                    .forEach(className -> packages.add(packageOf(className)));
        }
        return packages;
    }

    private static Path packageToDir(String pkg) {
        return Paths.get(CLASSES_DIR, pkg.split(Pattern.quote(JAVA_PACKAGE_SEPARATOR)));
    }

    private static boolean isClassFile(Path p) {
        String fileName = p.getFileName().toString();
        return fileName.endsWith(CLASS_FILE_SUFFIX) && !fileName.contains(INNER_CLASS_MARKER);
    }

    private static boolean isNotInfrastructureClassFile(Path p) {
        String fileName = p.getFileName().toString();
        return !MODULE_INFO_CLASS.equals(fileName) && !PACKAGE_INFO_CLASS.equals(fileName);
    }

    private static String pathToClassName(Path classFile) {
        Path classesRoot = Paths.get(CLASSES_DIR).toAbsolutePath().normalize();
        Path absolute = classFile.toAbsolutePath().normalize();
        Path relative = classesRoot.relativize(absolute);
        String withoutSuffix = relative.toString();
        withoutSuffix = withoutSuffix.substring(0, withoutSuffix.length() - CLASS_FILE_SUFFIX.length());
        return withoutSuffix.replace(FS_SEPARATOR, JAVA_PACKAGE_SEPARATOR)
                .replace(java.io.File.separator, JAVA_PACKAGE_SEPARATOR);
    }

    private static String packageOf(String className) {
        int lastDot = className.lastIndexOf(JAVA_PACKAGE_SEPARATOR);
        return lastDot < 0 ? "" : className.substring(0, lastDot);
    }

    private static boolean isPublicClass(String className) {
        try {
            Class<?> cls = Class.forName(className);
            return Modifier.isPublic(cls.getModifiers());
        } catch (ClassNotFoundException | NoClassDefFoundError unloadable) {
            // Classes that fail to load (missing transitive deps in test classpath)
            // cannot themselves be checked, so do not treat them as public for
            // the orphaned-package check — they will surface in other tests if
            // they are real.
            return false;
        }
    }

}
