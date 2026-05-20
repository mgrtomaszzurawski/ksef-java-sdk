# ADR-023: `Stream<T>` paginators replace materialized `queryAll*` / `listAll`

Date: 2026-05-06
Status: Accepted
Last verified: 2026-05-07

## Context

KSeF query and list endpoints are paginated. The 0.x SDK exposed every
paginated endpoint twice:

- a low-level method returning a single page (`queryInvoicesByMetadata(builder, pageOffset)`),
- a high-level convenience returning the full result set as a `List<T>`.

The convenience methods walked pages internally and returned `List<T>`.
Two problems emerged from production-shaped use:

1. **Silent truncation.** A defensive cap of `DEFAULT_QUERY_RESULT_LIMIT = 10_000`
   was applied to every full-result helper. Callers querying a large
   range silently got only the first 10k records with no error and no
   indication that more existed. Documented as "fix by Javadoc" in an
   earlier round; the documentation note did not survive contact with
   real consumers.
2. **Forced full materialization.** The convenience always built the
   complete `List<T>` in heap before returning. Callers that only
   needed the first N matched (`for invoice in invoices` then `break`)
   paid the cost of fetching every page anyway.

A follow-up audit proposed adding `maxResults` overloads on the
materialized helpers. That addresses #1 partially (caller can opt into
a non-default cap) but leaves the zero-arg variant silently truncating,
requires the sentinel `Integer.MAX_VALUE` to mean "unbounded" (a code
smell), and does nothing for #2.

## Decision

Replace every `queryAll*` / `listAll` with a `streamXxx*` variant
returning a lazy `Stream<T>` backed by an internal `PagedSpliterator`.
The spliterator pulls one page at a time on demand, holds at most one
page in heap, and stops fetching as soon as the caller's terminal
operation is satisfied.

Two flavours, one pattern:

- `PagedSpliterator.stream(IntFunction<Page<T>> fetchPage)` — offset-based,
  walks `pageOffset = 0, 1, 2, ...` until the response reports
  `hasMore == false`. Used by query endpoints with a numeric
  `pageOffset`.
- `PagedSpliterator.cursorStream(Function<String, CursorPage<T>> fetchPage)` —
  cursor-based, passes `null` on the first call and forwards
  `nextCursor` until it goes `null`/empty. Used by the `/tokens`
  endpoint with its `x-continuation-token` header.

Callers bound memory and work explicitly:

```java
List<InvoiceMetadata> first100 = client.invoices()
        .streamInvoicesByMetadata(query)
        .limit(100)
        .toList();
```

A defensive bound (`MAX_CONSECUTIVE_EMPTY_PAGES = 100_000`, JVM-tunable)
aborts the walk with `IllegalStateException` if a misbehaving server
returns `Page(items=[], hasMore=true)` indefinitely — replacing the
implicit safety the old 10k cap provided.

## Alternatives considered

1. **Add `maxResults` overloads to materialized helpers** (the original
   audit proposal). Rejected: doesn't fix silent truncation on the
   zero-arg variant, encodes "unbounded" as a sentinel, still
   materializes everything.
2. **Keep materialized helpers + raise the cap to `Integer.MAX_VALUE`**.
   Rejected: trades silent truncation for silent OOM on large ranges.
3. **Provide async/reactive paginators** (`Publisher<T>`, `Flowable<T>`).
   Rejected for 1.0: introduces a runtime dependency choice (Reactor
   vs. RxJava vs. Mutiny) the SDK does not need to make. `Stream<T>`
   is JDK-native and `parallelStream()` covers the common
   "batch-process N pages concurrently" case.

The chosen pattern matches AWS SDK V2 paginators and GCP `Page`
iterators — the established Java SDK idiom.

## Consequences

- **Breaking change vs. 0.x SDKs:** all `queryAll*` / `listAll`
  methods removed. Acceptable because pre-1.0 nothing has shipped to
  Maven Central; SemVer pre-1.0 permits it.
- **Caller responsibility:** consumers must bound memory and lifetime
  themselves. `.limit(N)` for hard caps, `.takeWhile(...)` for
  early-termination predicates, `.toList()` only when the result set
  is known-small.
- **Defensive bound surfaces server bugs:** if KSeF ever ships a
  pagination contract drift (empty page with `hasMore=true`),
  consumers see a clear `IllegalStateException` with diagnostic
  message instead of a hung thread.
- **Stream consumption is single-shot.** Reusing the same `Stream<T>`
  across two terminal operations throws `IllegalStateException` per
  the JDK contract — call `streamXxx*()` again to walk twice.
- **No `Stream::close`** required: spliterators do not hold
  resources between page fetches; closing the wrapped HTTP response
  happens inside each fetcher call.

## Where it lives

- `internal/runtime/pagination/PagedSpliterator.java` — package-private
  walker, two `Stream<T>` factories, two record types
  (`Page<T>` / `CursorPage<T>`).
- Domain client interfaces (`InvoiceClient`, `PermissionClient`,
  `TokenClient`, ...) expose `streamXxx*` methods. The internal
  `Impl` classes wire each method to a spliterator over the appropriate
  raw response shape.
