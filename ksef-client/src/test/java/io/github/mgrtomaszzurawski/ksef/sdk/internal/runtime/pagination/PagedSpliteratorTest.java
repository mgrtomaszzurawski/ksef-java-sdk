/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedSpliteratorTest {

    private static final int PAGE_SIZE = 3;
    private static final int TOTAL_PAGES_OFFSET = 4;

    @Test
    void offsetStream_walksAllPagesUntilHasMoreFalse() {
        AtomicInteger callCount = new AtomicInteger();
        Stream<Integer> stream = PagedSpliterator.stream(pageOffset -> {
            callCount.incrementAndGet();
            int from = pageOffset * PAGE_SIZE;
            List<Integer> items = List.of(from, from + 1, from + 2);
            boolean hasMore = pageOffset < TOTAL_PAGES_OFFSET - 1;
            return new PagedSpliterator.Page<>(items, hasMore);
        });

        List<Integer> all = stream.toList();

        assertEquals(TOTAL_PAGES_OFFSET * PAGE_SIZE, all.size());
        assertEquals(0, all.get(0));
        assertEquals(TOTAL_PAGES_OFFSET * PAGE_SIZE - 1, all.get(all.size() - 1));
        assertEquals(TOTAL_PAGES_OFFSET, callCount.get());
    }

    @Test
    void offsetStream_limit_stopsFetchingPagesEarly() {
        AtomicInteger callCount = new AtomicInteger();
        Stream<Integer> stream = PagedSpliterator.stream(pageOffset -> {
            callCount.incrementAndGet();
            int from = pageOffset * PAGE_SIZE;
            return new PagedSpliterator.Page<>(List.of(from, from + 1, from + 2), true);
        });

        List<Integer> first4 = stream.limit(4).toList();

        assertEquals(4, first4.size());
        assertTrue(callCount.get() <= 2,
                "limit(4) should not require more than 2 page fetches with pageSize=3, was: " + callCount.get());
    }

    @Test
    void offsetStream_emptyFirstPage_returnsEmpty() {
        Stream<Integer> stream = PagedSpliterator.stream(pageOffset ->
                new PagedSpliterator.Page<>(List.of(), false));
        assertEquals(0, stream.count());
    }

    @Test
    void offsetStream_emptyMiddlePage_keepsFetching() {
        AtomicInteger callCount = new AtomicInteger();
        Stream<Integer> stream = PagedSpliterator.stream(pageOffset -> {
            callCount.incrementAndGet();
            if (pageOffset == 0) {
                return new PagedSpliterator.Page<>(List.of(1, 2), true);
            }
            if (pageOffset == 1) {
                return new PagedSpliterator.Page<>(List.of(), true);
            }
            return new PagedSpliterator.Page<>(List.of(3, 4), false);
        });

        List<Integer> all = stream.toList();
        assertEquals(List.of(1, 2, 3, 4), all);
        assertEquals(3, callCount.get());
    }

    @Test
    void cursorStream_walksAllPagesUntilNullCursor() {
        Stream<String> stream = PagedSpliterator.cursorStream(cursor -> {
            if (cursor == null) {
                return new PagedSpliterator.CursorPage<>(List.of("a", "b"), "next1");
            }
            if ("next1".equals(cursor)) {
                return new PagedSpliterator.CursorPage<>(List.of("c"), "next2");
            }
            return new PagedSpliterator.CursorPage<>(List.of("d", "e"), null);
        });

        assertEquals(List.of("a", "b", "c", "d", "e"), stream.toList());
    }

    @Test
    void cursorStream_emptyCursor_terminates() {
        Stream<String> stream = PagedSpliterator.cursorStream(cursor ->
                new PagedSpliterator.CursorPage<>(List.of("only"), ""));
        assertEquals(List.of("only"), stream.toList());
    }

    @Test
    void cursorStream_propagatesFetcherException() {
        Stream<String> stream = PagedSpliterator.cursorStream(cursor -> {
            throw new IllegalStateException("network down");
        });
        IllegalStateException ex = assertThrows(IllegalStateException.class, stream::toList);
        assertEquals("network down", ex.getMessage());
    }

    @Test
    void offsetStream_nullFetcher_throwsNpe() {
        assertThrows(NullPointerException.class, () -> PagedSpliterator.stream(null));
    }

    @Test
    void cursorStream_nullFetcher_throwsNpe() {
        assertThrows(NullPointerException.class, () -> PagedSpliterator.cursorStream(null));
    }

    @Test
    void page_nullItems_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> new PagedSpliterator.Page<>(null, false));
    }

    @Test
    void cursorPage_nullItems_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> new PagedSpliterator.CursorPage<>(null, null));
    }

    @Test
    void offsetStream_serverReturnsEmptyHasMoreTrueForever_abortsWithIllegalState() {
        AtomicInteger callCount = new AtomicInteger();
        Stream<Integer> stream = PagedSpliterator.stream(pageOffset -> {
            callCount.incrementAndGet();
            return new PagedSpliterator.Page<>(List.of(), true);
        });

        IllegalStateException ex = assertThrows(IllegalStateException.class, stream::toList);
        assertTrue(ex.getMessage().contains("consecutive empty pages"), ex.getMessage());
        assertTrue(callCount.get() > PagedSpliterator.MAX_CONSECUTIVE_EMPTY_PAGES,
                "should reach defensive bound, was: " + callCount.get());
    }

    @Test
    void cursorStream_serverReturnsEmptyNonNullCursorForever_abortsWithIllegalState() {
        AtomicInteger callCount = new AtomicInteger();
        Stream<String> stream = PagedSpliterator.cursorStream(cursor -> {
            int call = callCount.incrementAndGet();
            return new PagedSpliterator.CursorPage<>(List.of(), "cursor-" + call);
        });

        IllegalStateException ex = assertThrows(IllegalStateException.class, stream::toList);
        assertTrue(ex.getMessage().contains("consecutive empty pages"), ex.getMessage());
        assertTrue(callCount.get() > PagedSpliterator.MAX_CONSECUTIVE_EMPTY_PAGES,
                "should reach defensive bound, was: " + callCount.get());
    }
}
