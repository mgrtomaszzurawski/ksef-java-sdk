/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy spliterator that walks KSeF list/query pagination and exposes
 * the underlying records as a {@link Stream}.
 *
 * <p>Two pagination idioms in KSeF, both supported:
 * <ul>
 *   <li><strong>Offset-based</strong> — most query endpoints take a
 *       {@code pageOffset} integer; SDK calls {@code 0, 1, 2, ...}
 *       until the response reports {@code hasMore == false}. Use
 *       {@link #stream(IntFunction)}.</li>
 *   <li><strong>Cursor-based</strong> — the {@code /tokens} endpoint
 *       uses an {@code x-continuation-token} header opaque cursor.
 *       SDK passes {@code null} on the first call and forwards the
 *       cursor returned in each {@link CursorPage}'s
 *       {@link CursorPage#nextCursor()} until that cursor goes
 *       {@code null}/empty. Use {@link #cursorStream(Function)}.</li>
 * </ul>
 *
 * <p>No upper bound is imposed. Memory pressure is bounded by what
 * the caller materialises from the stream — the spliterator only
 * holds one page at a time. Equivalent in spirit to AWS SDK V2
 * paginators / GCP {@code Page} iterators.
 *
 * @since 1.0.0
 */
public final class PagedSpliterator {

    private PagedSpliterator() { }

    /**
     * Wrap an offset-based pager as a sequential {@link Stream}.
     *
     * @param fetchPage given the current zero-based {@code pageOffset},
     *     return the matching {@link Page}
     */
    public static <T> Stream<T> stream(IntFunction<Page<T>> fetchPage) {
        Objects.requireNonNull(fetchPage, "fetchPage must not be null");
        return StreamSupport.stream(new OffsetSpliterator<>(fetchPage), false);
    }

    /**
     * Wrap a cursor-based pager as a sequential {@link Stream}. The
     * first invocation receives {@code null}; subsequent invocations
     * receive the {@link CursorPage#nextCursor()} returned by the
     * previous call. Iteration stops when {@code nextCursor() == null}.
     */
    public static <T> Stream<T> cursorStream(Function<String, CursorPage<T>> fetchPage) {
        Objects.requireNonNull(fetchPage, "fetchPage must not be null");
        return StreamSupport.stream(new CursorBasedSpliterator<>(fetchPage), false);
    }

    /**
     * One page from an offset-based pager.
     *
     * @param items records on this page (non-null, possibly empty)
     * @param hasMore {@code true} if at least one more page exists
     */
    public record Page<T>(List<T> items, boolean hasMore) {
        public Page {
            Objects.requireNonNull(items, "items must not be null");
        }
    }

    /**
     * One page from a cursor-based pager.
     *
     * @param items records on this page (non-null, possibly empty)
     * @param nextCursor cursor token to pass on the next call, or
     *     {@code null} when the stream is exhausted
     */
    public record CursorPage<T>(List<T> items, String nextCursor) {
        public CursorPage {
            Objects.requireNonNull(items, "items must not be null");
        }
    }

    private abstract static class BaseSpliterator<T> implements Spliterator<T> {

        private static final int CHARACTERISTICS = NONNULL | ORDERED;

        protected final Deque<T> buffer = new ArrayDeque<>();
        protected boolean exhausted;

        @Override
        public final boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action must not be null");
            if (buffer.isEmpty() && !fetchUntilNonEmptyOrEnd()) {
                return false;
            }
            action.accept(buffer.poll());
            return true;
        }

        protected abstract boolean fetchUntilNonEmptyOrEnd();

        // Sequential by design — page fetching is I/O-bound and ordered;
        // a Spliterator returning null from trySplit means single-thread.
        @Override
        @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract") // canonical non-splittable Spliterator returns null, not abstract
        public final Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public final long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public final int characteristics() {
            return CHARACTERISTICS;
        }
    }

    private static final class OffsetSpliterator<T> extends BaseSpliterator<T> {

        private final IntFunction<Page<T>> fetchPage;
        private int nextPageOffset;

        OffsetSpliterator(IntFunction<Page<T>> fetchPage) {
            this.fetchPage = fetchPage;
        }

        @Override
        protected boolean fetchUntilNonEmptyOrEnd() {
            while (!exhausted) {
                Page<T> page = fetchPage.apply(nextPageOffset);
                nextPageOffset++;
                buffer.addAll(page.items());
                if (!page.hasMore()) {
                    exhausted = true;
                }
                if (!buffer.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class CursorBasedSpliterator<T> extends BaseSpliterator<T> {

        private final Function<String, CursorPage<T>> fetchPage;
        private String nextCursor;

        CursorBasedSpliterator(Function<String, CursorPage<T>> fetchPage) {
            this.fetchPage = fetchPage;
        }

        @Override
        protected boolean fetchUntilNonEmptyOrEnd() {
            while (!exhausted) {
                CursorPage<T> page = fetchPage.apply(nextCursor);
                buffer.addAll(page.items());
                String next = page.nextCursor();
                if (next == null || next.isEmpty()) {
                    exhausted = true;
                } else {
                    nextCursor = next;
                }
                if (!buffer.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
