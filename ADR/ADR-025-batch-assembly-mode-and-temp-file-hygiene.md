# ADR-025: `BatchAssemblyMode` sealed interface and batch temp-file hygiene

Date: 2026-05-06
Status: Accepted

## Context

KSeF batch upload requires the SDK to:

1. ZIP every invoice XML into a single archive,
2. SHA-256 the full ZIP (whole-batch integrity hash),
3. split the archive into chunks ≤ 100 MiB per part (spec maximum),
4. AES-256-CBC encrypt each chunk independently,
5. SHA-256 each ciphertext (per-part integrity hash),
6. PUT each ciphertext to a presigned URL.

A 50-part 5 GiB batch is permitted by the spec. The 0.x SDK had three
problems:

1. **Two-pass disk usage.** Encryption wrote the full ZIP to disk,
   then re-read it to encrypt each chunk. Peak disk: ~2× batch size.
2. **No in-memory mode.** Read-only-filesystem environments
   (Lambda `/tmp`, sandboxed containers) could not run batch upload
   even for small batches.
3. **Silent reliance on `java.io.tmpdir`.** No way to redirect the
   temp files to a faster volume or an encrypted mount.
4. **No crash recovery.** A SIGKILL or OOM before
   `BatchPackage.cleanup()` left ciphertext fragments on `/tmp`
   indefinitely (`deleteOnExit` does not fire on hard exits).

## Decision

Introduce a sealed interface `BatchAssemblyMode` with three variants
exposed via static factories:

```java
public sealed interface BatchAssemblyMode {
    static BatchAssemblyMode onDisk();              // default, java.io.tmpdir
    static BatchAssemblyMode onDisk(Path tempDir);  // caller-chosen dir
    static BatchAssemblyMode inMemory(long maxBytes); // heap, fail-fast cap

    record OnDisk(Path tempDirectory) implements BatchAssemblyMode { }
    record InMemory(long maxBytes)    implements BatchAssemblyMode { }
}
```

Caller picks the mode via `BatchSessionOptions.assemblyMode(...)` and
the rest of the pipeline branches on the sealed subtype.

Single-pass pipeline. `BatchPackageBuilder.ChunkSink` is a custom
`OutputStream` plumbed into the ZIP writer. It accumulates into a
chunk buffer, encrypts and emits a `BatchPart` when the buffer fills,
then resets. ZIP bytes are never round-tripped through disk. Peak
disk drops from ~2× to 1× batch size; for InMemory mode peak disk
is 0.

Two `BatchPart` subtypes (also sealed, in non-exported
`internal.runtime.batch` per ADR-005):

- `OnDiskPart(int ordinalNumber, long sizeBytes, byte[] hash, Path path)`
  — uploaded via `BodyPublishers.ofFile(path)`,
- `InMemoryPart(int ordinalNumber, byte[] hash, byte[] bytes)` —
  uploaded via `BodyPublishers.ofByteArray(bytes)`.

`KsefBatchSession.uploadParts` pattern-matches on the subtype.

Crash recovery via `BatchTempCleanup.purgeOrphans`:

- runs from `KsefClient` constructor on a daemon thread (so a slow
  `Files.list` on a large `/tmp` does not block construction),
- scans `java.io.tmpdir` for files matching the SDK's exact prefix
  (`ksef-batch-part-...`),
- deletes anything older than `DEFAULT_ORPHAN_AGE = 24h` (longer
  than any plausible single-batch upload window).

Caller-supplied custom temp directories are NOT scanned automatically
— callers using `BatchAssemblyMode.onDisk(Path)` must invoke
`purgeOrphans(customDir, ttl)` themselves at startup. Documented on
`BatchTempCleanup`.

`InMemoryPart` deliberately skips defensive `byte[]` clones in its
compact constructor and `bytes()` accessor:

- the record is internal (non-exported package),
- only constructed by `ChunkSink` from a just-encrypted buffer with
  no other live references,
- `BodyPublishers.ofByteArray` performs its own internal copy.

Skipping the clones reduces peak heap from 3× (compact-ctor clone +
accessor clone + JDK internal copy) to 1× per part. For a 100 MiB
part: ~200 MiB saved.

## Alternatives considered

1. **Single `BatchAssemblyMode` enum** with `(maxBytes)` parameter
   stored separately. Rejected: enums cannot carry per-variant
   parameters cleanly. The sealed interface + records gives
   exhaustive `switch` and per-variant fields.
2. **Always on-disk; expose a `Path` parameter; no in-memory mode.**
   Rejected: read-only-filesystem environments are real (Lambda,
   sandboxed CI runners) and even small batches deserve to work
   there.
3. **`BatchTempCleanup` synchronous in constructor.** Initial
   implementation. Round-2 review flagged that
   `Files.list(/tmp)` on a host with millions of `/tmp` entries
   blocks construction for seconds. Switched to daemon thread.
4. **Defensive `byte[]` clones everywhere on `InMemoryPart`.**
   The "secure by default" answer. Rejected because the type is
   non-exported, only one internal constructor path exists, and the
   3× peak-heap cost was real on large batches. Hash component (32 B)
   stays defensive — small, security-relevant.

## Consequences

- **Public API surface gains `BatchAssemblyMode` and `BatchSessionOptions`.**
  Future-compatible: adding a new variant to the sealed interface is
  source-compatible for callers using the static factories.
- **`InMemoryPart.bytes()` is a no-clone accessor.** Same-package
  callers must not mutate the returned buffer. The class is internal
  and there is exactly one consumer (`KsefBatchSession.uploadParts`)
  which passes it directly to `BodyPublishers.ofByteArray`.
- **Custom temp-dir cleanup is opt-in.** Callers who choose
  `onDisk(Path)` for crash-recovery reasons must pair it with their
  own `purgeOrphans` call at startup. Documented on
  `BatchTempCleanup` Javadoc; the SDK does not track caller-supplied
  directories across JVM restarts.
- **`InMemory(maxBytes)` fails fast.** Construction throws
  `IllegalStateException` once emitted ciphertext exceeds the cap.
  Callers picking too-low caps for large batches see a clear error
  rather than OOM.
- **`ChunkSink.chunkBuffer` is lazy-allocated** on the first write.
  Tiny ZIPs (a single small invoice) no longer pay the 100 MiB
  default-part-size allocation cost.

## Where it lives

- `domain/invoicing/batch/BatchAssemblyMode.java` — public sealed
  interface, factories, records.
- `internal/runtime/batch/BatchPart.java` — internal sealed interface
  with `OnDiskPart` / `InMemoryPart`.
- `internal/runtime/batch/BatchPackageBuilder.java` — `ChunkSink`
  pipeline, `BatchPackage` aggregate, `cleanupAll`.
- `internal/runtime/batch/BatchTempCleanup.java` — `purgeOrphans`
  with `DEFAULT_ORPHAN_AGE = 24h`.
- `domain/invoicing/KsefBatchSession.java` — pattern-matches subtype
  in `uploadSinglePart`.
