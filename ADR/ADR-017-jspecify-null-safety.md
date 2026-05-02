# ADR-017: JSpecify-based null safety on the public API surface

Date: 2026-05-02
Status: Accepted

## Context

The SDK exports 14 public packages across `sdk.config`, `sdk.common`,
`sdk.exception`, and the eight `sdk.domain.<feature>/...` buckets. Without
null-safety annotations, consumers cannot tell whether a returned `String`,
`List<X>`, or record component is allowed to be `null` — they get no
compile-time signal from the IDE, no static analysis, and no documentation
of the contract.

JSpecify ([jspecify.dev](https://jspecify.dev/)) is the
JEP-recommended cross-vendor null-safety standard, supported by IntelliJ,
Eclipse, NullAway, and the JDK. Its `@NullMarked` annotation declares an
entire package non-null by default; per-method `@Nullable` overrides
selectively re-introduce nullability.

## Decision

1. Add `org.jspecify:jspecify:1.0.0` as a compile dependency.
2. Apply `@org.jspecify.annotations.NullMarked` to every exported
   `package-info.java` (25 files under
   `sdk.{common,config,exception,domain.*}`). Default: every method
   parameter, return type, and record component is non-null unless
   explicitly annotated `@Nullable`.
3. Selective `@Nullable` overrides:
   - Record components that genuinely admit null (e.g.
     `SessionStatus.upo()` while UPO is not yet available, optional builder
     setters that accept null to clear a previously-set value).
   - Builder fluent setters: most return `this` (always non-null) and accept
     non-null parameters; nullable inputs are an exception per setter.
   - `from(*Raw)` factories: the `*Raw` parameter is non-null (caller is
     internal SDK code that always passes a fresh deserialized object); the
     return is non-null.
4. Do NOT annotate the `internal/*` tree. Internal code uses
   field/method nullability conventions; consumers never see those packages.
5. Do NOT annotate generated `client.model` or `xml.model` packages —
   regeneration would overwrite annotations on every build.

## Consequences

- IDE auto-completion shows nullability on every public type.
- Static analysers (NullAway, Checker Framework) can verify SDK-internal
  code AND consumer code-paths.
- A consumer that compiles against `ksef-client:0.1.0` from a JPMS module
  benefits even without JSpecify on their side; the annotations are
  documentation-only (no runtime cost).
- Future record/builder additions inherit `@NullMarked` automatically —
  no per-class boilerplate.

## Implementation note (0.1.0 scope)

For 0.1.0 the change is package-default `@NullMarked` plus the explicit
overrides on records/builders that already have nullable fields. The full
sweep across every method is incremental — additional `@Nullable` overrides
land in 0.1.x patch releases as gaps surface.
