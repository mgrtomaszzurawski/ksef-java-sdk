/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSink;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Streaming {@link Spliterator} over {@link DecryptedInvoice} elements
 * produced by an {@link InvoiceSyncClient} run.
 *
 * <p>Drives the existing per-window sync algorithm without changing
 * {@link InvoiceSyncClient}'s callback shape: a single producer thread
 * runs {@code sync(plan, store, queueingSink)}, and the
 * {@link Consumer}-driven {@link #tryAdvance(Consumer)} polls a bounded
 * blocking queue. This keeps the lazy contract from
 * {@link InvoiceClient#syncAsStream} honest — the paginator stops as
 * soon as the consumer breaks out via
 * {@link java.util.stream.Stream#limit(long)} or close.
 *
 * <p>Per-element semantics (matching the docstring on
 * {@link InvoiceClient#syncAsStream}):
 * <ul>
 *   <li>Each element handed to {@link #tryAdvance(Consumer)} is the
 *       just-emitted decrypted invoice; the underlying
 *       {@link InvoiceSyncClient#sync} commits the per-window
 *       checkpoint after every invoice in the window has been accepted
 *       by the queueing sink, so the checkpoint advances atomically per
 *       fully-consumed window.</li>
 *   <li>{@link #close()} signals the producer thread to abort, drains
 *       the queue, and joins the producer — releasing any per-window
 *       directory the orchestrator opened.</li>
 * </ul>
 *
 * <p>Internal — only constructed by {@code InvoiceClientImpl#syncAsStream}.
 *
 * @since 1.0.0
 */
final class DecryptedInvoiceSyncSpliterator implements Spliterator<DecryptedInvoice>, AutoCloseable {

    /** Queue capacity — bounded so the producer cannot run far ahead of the consumer. */
    private static final int QUEUE_CAPACITY = 16;
    /** Poll interval when waiting for the producer to enqueue another invoice. */
    private static final long POLL_TIMEOUT_MILLIS = 250L;
    /** Producer-thread name template — load-bearing for log diagnostics. */
    private static final String PRODUCER_THREAD_NAME = "DecryptedInvoiceSyncProducer";
    /** Sentinel posted to the queue by the producer when the sync run completes. */
    private static final DecryptedInvoice POISON_PILL = null;
    /** Join timeout for the producer thread on close — bounded so close cannot hang. */
    private static final long PRODUCER_JOIN_TIMEOUT_SECONDS = 30L;

    private static final String ERR_READ_XML_FAILED = "Failed to read decrypted invoice XML at ";
    private static final String ERR_INTERRUPTED = "Interrupted while waiting for next decrypted invoice";

    private final BlockingQueue<DecryptedInvoice> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicReference<Throwable> producerFailure = new AtomicReference<>();
    private final ExecutorService executor;
    private final CompletableFuture<Void> producerFuture;
    private volatile boolean cancelled;
    private boolean producerCompleted;

    DecryptedInvoiceSyncSpliterator(InvoiceClient invoiceClient,
                                    ObjectMapper objectMapper,
                                    IncrementalSyncPlan plan,
                                    CheckpointStore checkpointStore) {
        Objects.requireNonNull(invoiceClient, "invoiceClient must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(checkpointStore, "checkpointStore must not be null");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, PRODUCER_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        InvoiceSink queueingSink = newQueueingSink();
        this.producerFuture = CompletableFuture.runAsync(
                () -> new InvoiceSyncClient(invoiceClient, objectMapper).sync(plan, checkpointStore, queueingSink),
                executor)
                .whenComplete((unused, failure) -> {
                    if (failure != null) {
                        producerFailure.set(failure);
                    }
                    enqueuePoisonPill();
                });
    }

    @Override
    public boolean tryAdvance(Consumer<? super DecryptedInvoice> action) {
        Objects.requireNonNull(action, "action must not be null");
        if (producerCompleted) {
            return false;
        }
        DecryptedInvoice next;
        try {
            next = queue.poll(POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            while (next == POISON_PILL && !producerFuture.isDone()) {
                next = queue.poll(POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            cancelled = true;
            throw new KsefException(ERR_INTERRUPTED, interrupted);
        }
        if (next == POISON_PILL) {
            producerCompleted = true;
            propagateProducerFailure();
            return false;
        }
        action.accept(next);
        return true;
    }

    @Override
    public Spliterator<DecryptedInvoice> trySplit() {
        // Single-source streaming spliterator — splitting is not supported.
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED | NONNULL;
    }

    @Override
    public void close() {
        cancelled = true;
        // Drain whatever the producer already enqueued so a put() does not block on capacity.
        queue.clear();
        try {
            producerFuture.get(PRODUCER_JOIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | java.util.concurrent.TimeoutException ignored) {
            // Producer failure is surfaced via tryAdvance; close itself never throws.
        } finally {
            executor.shutdownNow();
        }
    }

    private InvoiceSink newQueueingSink() {
        return (ksefNumber, metadata, xmlPath) -> {
            if (cancelled) {
                throw new CancelledStreamException();
            }
            byte[] xml = readXmlBytes(xmlPath);
            DecryptedInvoice invoice = new DecryptedInvoice(ksefNumber, metadata, xml,
                    xmlPath != null ? Optional.of(xmlPath) : Optional.empty());
            try {
                while (!cancelled) {
                    if (queue.offer(invoice, POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                        return;
                    }
                }
                throw new CancelledStreamException();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new CancelledStreamException();
            }
        };
    }

    private void enqueuePoisonPill() {
        // Best-effort enqueue of the sentinel; tryAdvance also re-polls on
        // producerFuture.isDone() so a full queue does not deadlock.
        queue.offer(POISON_PILL);
    }

    private void propagateProducerFailure() {
        Throwable failure = producerFailure.get();
        if (failure == null) {
            return;
        }
        Throwable rootCause = failure instanceof java.util.concurrent.CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        if (rootCause instanceof CancelledStreamException) {
            return;
        }
        if (rootCause instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (rootCause instanceof Error errorFailure) {
            throw errorFailure;
        }
        throw new KsefException("Sync producer thread failed", rootCause);
    }

    private static byte[] readXmlBytes(Path xmlPath) {
        if (xmlPath == null) {
            return new byte[0];
        }
        try {
            return Files.readAllBytes(xmlPath);
        } catch (java.io.IOException ioFailure) {
            throw new IllegalStateException(ERR_READ_XML_FAILED + xmlPath, ioFailure);
        }
    }

    /**
     * Internal control-flow exception thrown by the queueing sink when the
     * downstream consumer closed the stream — propagates up through
     * {@link InvoiceSyncClient#sync} to terminate the producer cooperatively.
     */
    private static final class CancelledStreamException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
