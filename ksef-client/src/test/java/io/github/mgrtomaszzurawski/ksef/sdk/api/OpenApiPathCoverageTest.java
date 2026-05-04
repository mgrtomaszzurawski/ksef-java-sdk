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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architecture gate that asserts every path in the upstream OpenAPI
 * spec ({@code ksef-client/openapi/open-api.json}) is reachable from
 * the SDK's WireMock contract tests.
 *
 * <p>Strategy: for each spec path, derive the URL the SDK would call
 * (server URL ends in {@code /v2}, so spec path {@code /auth/challenge}
 * becomes {@code /v2/auth/challenge}). Split on path placeholders
 * ({@code {referenceNumber}}) to get fixed anchor chunks. The test
 * passes if every chunk appears in order somewhere in the concatenated
 * test sources (covering both {@code urlEqualTo("/v2/...")} stubs and
 * verifications that build URLs from constants like
 * {@code SESSIONS_BASE + "/" + REF + "/invoices"}).
 *
 * <p>This is intentionally a coverage gate, not a strictness gate: it
 * asks "does some test exercise this path?", not "does the right
 * domain client exercise it with the right method/headers/body". That
 * stricter assertion lives in the per-domain WireMock tests.
 *
 * <p>Spec citation: TC-ARCH-005.
 */
class OpenApiPathCoverageTest {

    private static final String OPENAPI_PATH = "openapi/open-api.json";
    private static final Path TEST_SOURCES = Path.of("src/test/java");
    private static final String JAVA_SUFFIX = ".java";
    private static final String VERSION_PREFIX = "/v2";
    private static final String PLACEHOLDER_PATTERN = "\\{[^}]+\\}";
    /**
     * Hand-curated allow-list — paths the SDK does not expose because the
     * corresponding feature is intentionally not implemented or is exercised
     * implicitly. Each entry must be justified.
     */
    private static final List<String> ALLOW_LIST = List.of(
            // None — every spec path is currently expected to be exercised in tests.
    );

    @Test
    void everyOpenApiPath_isExercisedFromAtLeastOneWireMockTest() throws IOException {
        List<String> specPaths = loadSpecPaths();
        String concatenatedTestSource = concatenateSources(TEST_SOURCES);

        List<String> missing = new ArrayList<>();
        for (String path : specPaths) {
            if (ALLOW_LIST.contains(path)) {
                continue;
            }
            if (!isExercisedByTests(path, concatenatedTestSource)) {
                missing.add(path);
            }
        }

        if (!missing.isEmpty()) {
            fail("OpenAPI spec paths NOT exercised by any WireMock test:\n  - "
                    + String.join("\n  - ", missing));
        }
    }

    @Test
    void specHasAtLeastTheKnownNumberOfPaths() throws IOException {
        // Sanity check — the openapi.json must not silently shrink.
        // Spec at 1.0.0 has 73 paths; using a soft floor of 50 to allow
        // routine spec refinements without churn.
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

    private static String concatenateSources(Path root) throws IOException {
        StringBuilder builder = new StringBuilder(2_000_000);
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path javaFile : stream.filter(p -> p.toString().endsWith(JAVA_SUFFIX)).toList()) {
                builder.append(Files.readString(javaFile)).append('\n');
            }
        }
        return builder.toString();
    }

    /**
     * A path is considered exercised when one of two conditions holds:
     * <ol>
     *   <li>the full {@code /v2/<spec-path>} appears as an in-order
     *       substring (with placeholder regions free), or</li>
     *   <li>each non-placeholder slash-segment appears at least once in
     *       the test sources (covers the idiomatic SDK pattern of
     *       composing URLs from {@code BASE + "/x/" + ref + "/y"}
     *       constants split across multiple string literals).</li>
     * </ol>
     */
    private static boolean isExercisedByTests(String specPath, String testSource) {
        if (matchesAsContiguousAnchors(specPath, testSource)) {
            return true;
        }
        return matchesAsIndividualSegments(specPath, testSource);
    }

    private static boolean matchesAsContiguousAnchors(String specPath, String testSource) {
        String fullPath = VERSION_PREFIX + specPath;
        String[] anchors = fullPath.split(PLACEHOLDER_PATTERN);
        int cursor = 0;
        for (String anchor : anchors) {
            if (anchor.isEmpty()) {
                continue;
            }
            int found = testSource.indexOf(anchor, cursor);
            if (found < 0) {
                return false;
            }
            cursor = found + anchor.length();
        }
        return true;
    }

    private static boolean matchesAsIndividualSegments(String specPath, String testSource) {
        String[] segments = specPath.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || segment.matches("\\{[^}]+\\}")) {
                continue;
            }
            String slashSegment = "/" + segment;
            if (!testSource.contains(slashSegment)) {
                return false;
            }
        }
        return true;
    }
}
