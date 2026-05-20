/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that the canonical batch-threading warning text from PR11 lives in
 * the five surfaces it must be impossible to miss:
 * <ol>
 *   <li>Method javadoc on {@code submit} (in {@code InvoiceBatch}).</li>
 *   <li>Method javadoc on {@code submitFromFiles}.</li>
 *   <li>Class-level javadoc on {@code InvoiceBatch}.</li>
 *   <li>{@code README.md} batch-upload section.</li>
 *   <li>{@code CHANGELOG.md} unreleased / 1.0.0 entry.</li>
 * </ol>
 *
 * <p>The grep is anchored on a load-bearing phrase substring rather than an
 * exact-text match, so paragraph breaks and minor edits do not break the
 * gate while still catching surfaces that omit the warning entirely.
 */
class BatchThreadingWarningPlacementTest {

    /**
     * Whitespace-collapsed substring of the canonical warning. We collapse
     * newlines + multiple spaces to a single space before searching so the
     * test does not break when javadoc paragraph wrap reflows.
     */
    private static final String CANONICAL_PHRASE_FRAGMENT_BLOCK =
            "calling thread for minutes to hours";
    private static final String CANONICAL_PHRASE_FRAGMENT_5GB = "5 GB";
    private static final String CANONICAL_PHRASE_FRAGMENT_NOT_UI =
            "Do not call from UI threads";

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void invoiceBatchSourceFile_containsThreadingWarningOnBothBatchMethods() throws IOException {
        String collapsed = collapseWhitespace(readInvoiceBatchSource());
        // Three distinct occurrences expected:
        //   - class-level javadoc on InvoiceBatch
        //   - submit method javadoc
        //   - submitFromFiles method javadoc
        int occurrences = countOccurrences(collapsed, CANONICAL_PHRASE_FRAGMENT_BLOCK);
        final int expectedSurfacesInInvoiceBatch = 3;
        assertTrue(occurrences >= expectedSurfacesInInvoiceBatch,
                "InvoiceBatch must contain the threading-warning phrase at least 3 times "
                        + "(class-level + submit + submitFromFiles); found " + occurrences);
        assertTrue(collapsed.contains(CANONICAL_PHRASE_FRAGMENT_5GB),
                "InvoiceBatch must mention the 5 GB batch ceiling");
        assertTrue(collapsed.contains(CANONICAL_PHRASE_FRAGMENT_NOT_UI),
                "InvoiceBatch must contain the 'Do not call from UI threads' phrase");
    }

    @Test
    void readme_containsThreadingWarningInBatchSection() throws IOException {
        String readme = collapseWhitespace(Files.readString(findRepoFile("README.md"), StandardCharsets.UTF_8));
        assertTrue(readme.contains(CANONICAL_PHRASE_FRAGMENT_BLOCK),
                "README.md must contain the canonical batch-threading warning");
        assertTrue(readme.contains(CANONICAL_PHRASE_FRAGMENT_NOT_UI),
                "README.md must explicitly forbid UI / HTTP / reactive dispatch threads");
    }

    @Test
    void changelog_containsThreadingWarningInUnreleased() throws IOException {
        String changelog = collapseWhitespace(
                Files.readString(findRepoFile("CHANGELOG.md"), StandardCharsets.UTF_8));
        assertTrue(changelog.contains(CANONICAL_PHRASE_FRAGMENT_BLOCK),
                "CHANGELOG.md must contain the canonical batch-threading warning");
        assertTrue(changelog.contains(CANONICAL_PHRASE_FRAGMENT_NOT_UI),
                "CHANGELOG.md must explicitly forbid UI / HTTP / reactive dispatch threads");
    }

    /**
     * Collapse runs of whitespace (including markdown / javadoc paragraph
     * wrap and blockquote markers) to a single space so the canonical
     * warning text matches even when the surface formats it across multiple
     * lines.
     */
    private static String collapseWhitespace(String input) {
        return input.replaceAll("[\\s>*]+", " ");
    }

    private static String readInvoiceBatchSource() throws IOException {
        Path source = findRepoFile(
                "ksef-client/src/main/java/io/github/mgrtomaszzurawski/ksef/sdk/domain/invoicing/archive/InvoiceBatch.java");
        return Files.readString(source, StandardCharsets.UTF_8);
    }

    /**
     * Test working directory is unstable across `mvn test` invocation styles
     * (module subdir vs. root) — walk up from cwd until the supplied relative
     * path resolves.
     */
    private static Path findRepoFile(String relative) {
        Path candidate = PROJECT_ROOT;
        for (int depth = 0; depth < 4; depth++) {
            Path attempt = candidate.resolve(relative);
            if (Files.exists(attempt)) {
                return attempt;
            }
            Path parent = candidate.getParent();
            if (parent == null) {
                break;
            }
            candidate = parent;
        }
        throw new IllegalStateException(
                "Could not locate " + relative + " from cwd " + PROJECT_ROOT);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int index = haystack.indexOf(needle, from);
            if (index < 0) {
                return count;
            }
            count++;
            from = index + needle.length();
        }
    }
}
