/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architecture gate that asserts every path in the upstream OpenAPI
 * spec ({@code ksef-client/openapi/open-api.json}) is registered as
 * exercised by at least one WireMock contract test.
 *
 * <p>Coverage is registered explicitly in
 * {@code src/test/resources/openapi-coverage-registry.txt}. Each line
 * is one spec path, with placeholders intact. Adding an endpoint to
 * the SDK without adding it here (and a corresponding test) fails the
 * gate.
 *
 * <p>The previous implementation tried to grep test sources for path
 * literals — that gave false positives (Codex F3) because the SDK
 * builds URLs from string-concat constants that never appear as a
 * single literal substring in the source. The registry approach is
 * exactly as strong as developers keep it: it forces them to declare
 * test coverage explicitly, and the gate only protects against
 * silently dropping an endpoint without a test.
 *
 * <p>Spec citation: TC-ARCH-005.
 */
class OpenApiPathCoverageTest {

    private static final String OPENAPI_PATH = "openapi/open-api.json";
    private static final String COVERAGE_REGISTRY_RESOURCE = "openapi-coverage-registry.txt";
    private static final String COMMENT_MARKER = "#";
    private static final java.util.Set<String> HTTP_METHODS = java.util.Set.of(
            "get", "post", "put", "patch", "delete", "head", "options");

    @Test
    void everyOpenApiPath_isRegisteredAsExercisedByAWireMockTest() throws IOException {
        // given / when
        Set<String> registeredPaths = registryPathsCovered();
        List<String> specPaths = loadSpecPaths();

        // then
        List<String> missing = new ArrayList<>();
        for (String path : specPaths) {
            if (!registeredPaths.contains(path)) {
                missing.add(path);
            }
        }

        if (!missing.isEmpty()) {
            fail("OpenAPI spec paths NOT registered in "
                    + COVERAGE_REGISTRY_RESOURCE
                    + ":\n  - " + String.join("\n  - ", missing)
                    + "\n\nAdd the path + the test class name to the registry, "
                    + "or remove the endpoint from the spec.");
        }
    }

    @Test
    void registry_hasNoEntriesForPathsTheSpecHasRemoved() throws IOException {
        // given / when
        Set<String> registeredPaths = registryPathsCovered();
        Set<String> specPaths = new HashSet<>(loadSpecPaths());

        // then
        List<String> stale = new ArrayList<>();
        for (String registered : registeredPaths) {
            if (!specPaths.contains(registered)) {
                stale.add(registered);
            }
        }

        if (!stale.isEmpty()) {
            fail("Registry references paths that no longer exist in the OpenAPI spec:\n  - "
                    + String.join("\n  - ", stale)
                    + "\n\nRemove the registry entry (and consider removing the dead test).");
        }
    }

    /**
     * Reduce the registry's mixed-format entries (path-only + {@code METHOD path}
     * tuples) to the underlying set of paths covered. A tuple covers its path;
     * a path-only line covers itself.
     */
    private static Set<String> registryPathsCovered() throws IOException {
        Set<String> result = new HashSet<>();
        for (String entry : loadCoverageRegistry()) {
            int firstSpace = entry.indexOf(' ');
            if (firstSpace > 0 && HTTP_METHODS.contains(entry.substring(0, firstSpace).toLowerCase(java.util.Locale.ROOT))) {
                result.add(entry.substring(firstSpace + 1));
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Codex round-9 fresh-review F4 — strict operation-level coverage.
     *
     * <p>For multi-method spec paths a path-only registry entry no longer
     * counts as covering every method — each operation must have its own
     * {@code METHOD path} tuple (or all operations on that path may be
     * covered by their respective tuples). Single-method paths are still
     * allowed to use the path-only short form because the implication is
     * unambiguous.
     */
    @Test
    void everyOpenApiOperation_hasARegisteredTuple() throws IOException {
        // given / when
        Set<String> registeredEntries = loadCoverageRegistry();
        java.util.Map<String, java.util.List<String>> methodsByPath = loadMethodsByPath();

        // then
        List<String> missing = new ArrayList<>();
        for (var entry : methodsByPath.entrySet()) {
            String path = entry.getKey();
            List<String> methods = entry.getValue();
            boolean isMultiMethod = methods.size() > 1;
            for (String method : methods) {
                String tuple = method + " " + path;
                if (registeredEntries.contains(tuple)) {
                    continue;
                }
                if (!isMultiMethod && registeredEntries.contains(path)) {
                    // Single-method path allowed to use path-only short form.
                    continue;
                }
                missing.add(tuple);
            }
        }

        if (!missing.isEmpty()) {
            fail("OpenAPI operations NOT registered as method-specific tuples in "
                    + COVERAGE_REGISTRY_RESOURCE
                    + ":\n  - " + String.join("\n  - ", missing)
                    + "\n\nMulti-method paths require one tuple per method "
                    + "(e.g. 'GET /tokens' AND 'POST /tokens'). "
                    + "Single-method paths may keep the path-only short form.");
        }
    }

    /**
     * Codex round-9 fresh-review F4 — also fail if the registry contains a
     * path-only entry for a path that the spec defines with more than one
     * method (the path-only entry would silently mask drift on a sibling
     * method).
     */
    @Test
    void registry_hasNoPathOnlyEntriesForMultiMethodSpecPaths() throws IOException {
        // given / when
        Set<String> registeredEntries = loadCoverageRegistry();
        java.util.Map<String, java.util.List<String>> methodsByPath = loadMethodsByPath();

        // then
        List<String> overlyBroad = new ArrayList<>();
        for (var entry : methodsByPath.entrySet()) {
            String path = entry.getKey();
            if (entry.getValue().size() > 1 && registeredEntries.contains(path)) {
                overlyBroad.add(path + " (methods in spec: " + entry.getValue() + ")");
            }
        }

        if (!overlyBroad.isEmpty()) {
            fail("Registry uses path-only entries for multi-method spec paths "
                    + "(must be method-specific tuples instead):\n  - "
                    + String.join("\n  - ", overlyBroad));
        }
    }

    @Test
    void specHasAtLeastTheKnownNumberOfPaths() throws IOException {
        // given / when — sanity check that openapi.json must not silently shrink.
        // Spec at 1.0.0 has 73 paths; soft floor of 50 to allow routine spec
        // refinements without churn.
        List<String> specPaths = loadSpecPaths();

        // then
        assertTrue(specPaths.size() >= 50,
                "OpenAPI spec lost paths: only " + specPaths.size() + " present");
    }

    @Test
    void registry_namedTestClassesAllExist() throws IOException {
        // given — Codex round-9 F2 — registry was a declaration gate that could not detect
        // when a named test class was renamed/deleted. This second-stage check
        // walks src/test/java, builds the set of all simple test class names that
        // exist on disk, then asserts every comment-cited class name in the
        // registry actually corresponds to one of them.
        java.util.Set<String> testClassesOnDisk = scanTestClassNames();
        java.util.List<String> badRefs = new ArrayList<>();

        // when
        try (var lines = Files.lines(Path.of("src/test/resources/" + COVERAGE_REGISTRY_RESOURCE))) {
            lines.forEach(rawLine -> {
                int hash = rawLine.indexOf(COMMENT_MARKER);
                if (hash < 0) {
                    return;
                }
                String pathPart = rawLine.substring(0, hash).trim();
                String commentPart = rawLine.substring(hash + 1).trim();
                if (pathPart.isEmpty() || commentPart.isEmpty()) {
                    return;
                }
                for (String classRef : commentPart.split(",")) {
                    String trimmed = classRef.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (!testClassesOnDisk.contains(trimmed)) {
                        badRefs.add(pathPart + " -> " + trimmed);
                    }
                }
            });
        }

        // then
        if (!badRefs.isEmpty()) {
            fail("Registry references test classes that are not present in src/test/java:\n  - "
                    + String.join("\n  - ", badRefs));
        }
    }

    private static java.util.Set<String> scanTestClassNames() throws IOException {
        java.util.Set<String> result = new java.util.HashSet<>();
        Path testRoot = Path.of("src/test/java");
        if (!Files.isDirectory(testRoot)) {
            return result;
        }
        try (var stream = Files.walk(testRoot)) {
            stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        result.add(name.substring(0, name.length() - ".java".length()));
                    });
        }
        return result;
    }

    private static List<String> loadSpecPaths() throws IOException {
        Path specPath = Path.of(OPENAPI_PATH);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readAllBytes(specPath));
        JsonNode paths = root.path("paths");
        List<String> result = new ArrayList<>();
        Iterator<String> names = paths.fieldNames();
        while (names.hasNext()) {
            result.add(names.next());
        }
        return result;
    }

    /**
     * Returns a map from {@code /path} → sorted-uppercase method list (e.g.
     * {@code [DELETE, GET, POST]}). Used by the multi-method-vs-single-method
     * coverage gates.
     */
    private static java.util.Map<String, java.util.List<String>> loadMethodsByPath() throws IOException {
        Path specPath = Path.of(OPENAPI_PATH);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readAllBytes(specPath));
        JsonNode paths = root.path("paths");
        java.util.Map<String, java.util.List<String>> result = new java.util.LinkedHashMap<>();
        Iterator<String> pathNames = paths.fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathNode = paths.path(path);
            java.util.List<String> methods = new ArrayList<>();
            Iterator<String> methodNames = pathNode.fieldNames();
            while (methodNames.hasNext()) {
                String method = methodNames.next();
                if (HTTP_METHODS.contains(method.toLowerCase(java.util.Locale.ROOT))) {
                    methods.add(method.toUpperCase(java.util.Locale.ROOT));
                }
            }
            java.util.Collections.sort(methods);
            result.put(path, methods);
        }
        return result;
    }

    private static Set<String> loadCoverageRegistry() throws IOException {
        Set<String> result = new HashSet<>();
        try (var stream = OpenApiPathCoverageTest.class
                .getClassLoader()
                .getResourceAsStream(COVERAGE_REGISTRY_RESOURCE)) {
            if (stream == null) {
                fail("Coverage registry resource not found on classpath: " + COVERAGE_REGISTRY_RESOURCE);
                return result;
            }
            String contents = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String rawLine : contents.split("\n")) {
                String line = stripComment(rawLine).trim();
                if (line.isEmpty()) {
                    continue;
                }
                result.add(line);
            }
        }
        return result;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf(COMMENT_MARKER);
        return hash < 0 ? line : line.substring(0, hash);
    }
}
