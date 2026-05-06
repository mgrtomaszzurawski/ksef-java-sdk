/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchTempCleanup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchTempCleanupTest {

    @Test
    void purgeOrphans_deletesAgedSdkPrefixedFiles(@TempDir Path tempDir) throws IOException {
        // given
        Path orphan = Files.createTempFile(tempDir, BatchPackageBuilder.TEMP_PART_PREFIX, ".bin");
        Files.setLastModifiedTime(orphan, FileTime.from(Instant.now().minus(Duration.ofHours(2))));

        // when
        BatchTempCleanup.purgeOrphans(tempDir, Duration.ofHours(1));

        // then
        assertFalse(Files.exists(orphan), "old SDK-prefixed orphan must be deleted");
    }

    @Test
    void purgeOrphans_keepsFreshSdkPrefixedFiles(@TempDir Path tempDir) throws IOException {
        // given
        Path fresh = Files.createTempFile(tempDir, BatchPackageBuilder.TEMP_PART_PREFIX, ".bin");
        // mtime defaults to now → newer than the cutoff

        // when
        BatchTempCleanup.purgeOrphans(tempDir, Duration.ofHours(1));

        // then
        assertTrue(Files.exists(fresh), "fresh orphan must NOT be deleted (might be active batch)");
    }

    @Test
    void purgeOrphans_ignoresOtherPrefixes(@TempDir Path tempDir) throws IOException {
        // given
        Path other = Files.createTempFile(tempDir, "some-other-app-", ".bin");
        Files.setLastModifiedTime(other, FileTime.from(Instant.now().minus(Duration.ofHours(2))));

        // when
        BatchTempCleanup.purgeOrphans(tempDir, Duration.ofHours(1));

        // then
        assertTrue(Files.exists(other), "non-SDK files must NOT be touched");
    }

    @Test
    void purgeOrphans_missingDirectory_doesNotThrow() {
        // given
        Path nonExistent = Path.of("/definitely/does/not/exist/" + System.nanoTime());

        // when / then — contract is "best-effort, never throws"
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                BatchTempCleanup.purgeOrphans(nonExistent, Duration.ofHours(1)));
    }
}
