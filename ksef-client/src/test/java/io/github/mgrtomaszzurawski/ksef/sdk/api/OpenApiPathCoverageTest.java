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

    @Test
    void everyOpenApiPath_isRegisteredAsExercisedByAWireMockTest() throws IOException {
        Set<String> registeredPaths = loadCoverageRegistry();
        List<String> specPaths = loadSpecPaths();

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
        Set<String> registeredPaths = loadCoverageRegistry();
        Set<String> specPaths = new HashSet<>(loadSpecPaths());

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

    @Test
    void specHasAtLeastTheKnownNumberOfPaths() throws IOException {
        // Sanity check — the openapi.json must not silently shrink. Spec at
        // 1.0.0 has 73 paths; soft floor of 50 to allow routine spec
        // refinements without churn.
        List<String> specPaths = loadSpecPaths();
        assertTrue(specPaths.size() >= 50,
                "OpenAPI spec lost paths: only " + specPaths.size() + " present");
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
