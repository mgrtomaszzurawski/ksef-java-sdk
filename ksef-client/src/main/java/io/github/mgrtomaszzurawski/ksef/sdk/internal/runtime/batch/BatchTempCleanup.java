/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-shot cleanup of orphaned batch part temp files. Called by
 * {@code KsefClient} constructor to recover from previous JVM crashes
 * where {@code BatchPackage.cleanup()} did not run (kill -9, OOM-kill,
 * container abort).
 *
 * <p>Safe by construction:
 * <ul>
 *   <li>Only deletes files matching the SDK's exact prefix
 *       ({@link BatchPackageBuilder#TEMP_PART_PREFIX}) — won't touch
 *       any other tempfile.</li>
 *   <li>Only deletes files older than the configured age threshold —
 *       won't race against a concurrent batch in progress on a
 *       sibling JVM.</li>
 *   <li>Quiet — IO failures are logged at DEBUG and ignored. Cleanup
 *       is best-effort, never blocks construction.</li>
 * </ul>
 *
 * <p><strong>Custom temp directories:</strong> automatic constructor-time
 * cleanup only scans {@code java.io.tmpdir}. If callers configure a custom
 * directory via {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchAssemblyMode#onDisk(Path)},
 * they must call {@link #purgeOrphans(Path, Duration)} themselves at
 * application startup to recover crashed batches in that directory —
 * the SDK does not track caller-supplied directories across JVM restarts.
 *
 * @since 1.0.0
 */
public final class BatchTempCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchTempCleanup.class);
    private static final String LOG_DELETED = "Deleted orphaned batch temp file: {}";
    private static final String LOG_SKIPPED = "Skipping orphan cleanup, IO error scanning {}: {}";

    /**
     * Default minimum age for a tempfile to be considered orphaned. Set
     * to 24 hours so a long-running batch upload (slow link, large
     * multi-part batch) is not treated as orphaned by a freshly-started
     * sibling {@code KsefClient} on the same host.
     */
    public static final Duration DEFAULT_ORPHAN_AGE = Duration.ofHours(24);

    /**
     * Minimum interval between automatic constructor-time cleanups on
     * {@code java.io.tmpdir}. Caps thread/IO pressure for pathological
     * consumer pooling (one fresh {@code KsefClient} per request) — the
     * scan is quiet on second and subsequent constructions within the
     * window. Caller-driven {@link #purgeOrphans(Path, Duration)} calls
     * are NOT throttled.
     */
    private static final long AUTO_CLEANUP_THROTTLE_NANOS =
            Duration.ofHours(1).toNanos();

    /**
     * Last-run nanoTime for the auto-cleanup throttle. {@code 0} means
     * never run yet.
     */
    private static final AtomicLong LAST_AUTO_CLEANUP_NANOS = new AtomicLong(0);

    private static final String CLEANUP_THREAD_NAME = "ksef-batch-temp-cleanup";

    private BatchTempCleanup() { }

    /**
     * Scan the supplied {@code tempDirectory} (or
     * {@code java.io.tmpdir} when {@code null}) for files matching the
     * SDK's batch part prefix that are older than {@code minAge}, and
     * delete them. Does not recurse into subdirectories.
     */
    public static void purgeOrphans(Path tempDirectory, Duration minAge) {
        Path target = tempDirectory != null
                ? tempDirectory
                : Path.of(System.getProperty("java.io.tmpdir"));
        Instant ageCutoff = Instant.now().minus(minAge);
        try (Stream<Path> entries = Files.list(target)) {
            entries
                    .filter(BatchTempCleanup::matchesSdkPrefix)
                    .filter(path -> isOlderThan(path, ageCutoff))
                    .forEach(BatchTempCleanup::deleteQuietly);
        } catch (IOException scanFailure) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LOG_SKIPPED, target, scanFailure.getMessage());
            }
        }
    }

    private static boolean matchesSdkPrefix(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(BatchPackageBuilder.TEMP_PART_PREFIX);
    }

    private static boolean isOlderThan(Path path, Instant cutoff) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return attrs.lastModifiedTime().toInstant().isBefore(cutoff);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LOG_DELETED, path);
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    /**
     * Schedule an auto-cleanup of {@code java.io.tmpdir} on a fresh
     * short-lived daemon thread, throttled by
     * {@link #AUTO_CLEANUP_THROTTLE_NANOS} CAS. Called from
     * {@code KsefClient} constructor; safe to call from any number of
     * concurrent constructors — the throttle ensures at most one
     * spawn per window.
     *
     * <p><strong>Webapp-deployment note:</strong> a short-lived daemon
     * thread is used (rather than a static {@link java.util.concurrent.ExecutorService})
     * because static executors hold a hard reference to the thread
     * factory's classloader, which would prevent webapp unloading on
     * Tomcat/Jetty hot-redeploy. The daemon thread here exits as soon
     * as {@code purgeOrphans} returns (typically &lt; 1 s); only the
     * narrow window where a redeploy fires while the cleanup is
     * mid-execution risks a transient classloader pin, and the throttle
     * makes that probability extremely small (one cleanup per hour
     * across the JVM).
     */
    public static void scheduleAutoCleanup() {
        long currentNanos = System.nanoTime();
        long previous = LAST_AUTO_CLEANUP_NANOS.get();
        if (previous != 0 && currentNanos - previous < AUTO_CLEANUP_THROTTLE_NANOS) {
            return;
        }
        if (!LAST_AUTO_CLEANUP_NANOS.compareAndSet(previous, currentNanos)) {
            return;
        }
        Thread cleanupThread = new Thread(
                () -> purgeOrphans(null, DEFAULT_ORPHAN_AGE),
                CLEANUP_THREAD_NAME);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}
