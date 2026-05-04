# ADR-021: Public API tiers — workflow first, endpoints second

Date: 2026-05-03
Status: Accepted

## Context

The SDK exposes two kinds of public API today:

- **Workflow APIs** — `KsefSession`, `KsefBatchSession`,
  `PreparedInvoiceExport`, `KsefVerificationLinks`. Hide protocol details:
  encryption, hashing, polling, decryption, URL canonicalization. Consumer
  calls `session.send(xml)` and the SDK does the rest.
- **Endpoint APIs** — `InvoiceClient.queryMetadata(...)`,
  `PermissionClient.grantPerson(...)`, `TokenClient.list(...)`. Mirror the
  REST endpoints. Consumer assembles a request via builder, gets a typed
  response.

Both shapes have value. Workflow APIs are easier for typical use cases;
endpoint APIs are necessary when consumers want fine-grained control.

There has been recurring discussion about whether to expose a third tier
("advanced" / "raw" / "escape hatch") that lets consumers reach
generated `*Raw` types or transport internals when the SDK lags behind
KSeF API changes.

## Decision

The 1.0.0 SDK has two tiers. There is no third.

- **Tier 1 — Workflow API.** This is what README quick-start uses. It is
  what SDK examples demonstrate. It is the answer to "how do I [send /
  download / verify / authenticate / sync]?". Examples:
  - `try (KsefSession s = client.openSession(FormCode.FA3)) { s.send(xml); s.closeAndAwait(); }`
  - `client.batch().prepare(FormCode.FA3).addInvoice(...).uploadAndClose()`
  - `client.invoiceSync().sync(plan, checkpointStore, sink)`
  - `client.qr().invoiceVerificationUrl(nip, issueDate, sha256)`
- **Tier 2 — Domain endpoint API.** Endpoint-shaped methods on domain
  clients. Used when the workflow API doesn't fit (custom polling cadence,
  partial flows, building blocks for a custom orchestrator). Examples:
  - `client.invoices().queryMetadata(filter)`
  - `client.permissions().grantPerson(builder)`
  - `client.tokens().getStatus(refNumber)`

There is **no** Tier 3 in 1.0.0. Specifically:

- No public access to generated `*Raw` types.
- No public `HttpRuntime` / `HttpSupport` / transport internals.
- No `client.advanced()` / `client.raw()` accessor.
- No `KsefClientInternals` / `activateSessionForTests` (removed per ADR-020).

If a consumer needs behavior the SDK doesn't expose, the answer is "open
an issue, we add it as Tier 1 or Tier 2." Not "use the escape hatch."

## Consequences

- README and examples present Tier 1 first. Tier 2 documented as "for when
  workflows don't fit." Tier 3 is not mentioned.
- New public types must justify their existence as either Tier 1
  (workflow) or Tier 2 (endpoint domain). Test seams, internal helpers,
  and generated raw types do not appear in either.
- The "escape hatch" is implicit: new SDK releases can add Tier 1 / Tier 2
  capabilities. Consumers who need access before a release happens fork
  or patch the SDK locally, but the published API doesn't ship a hatch
  for them.
- Public API surface tests (Step 8 of implementation plan) enforce this
  by asserting no `sdk.internal.*` and no `client.model.*Raw` types in
  exported method signatures.

## Rejected alternatives

1. **Add Tier 3 advanced API.** Rejected because there are no consumers
   today and we don't know what advanced needs would look like.
   Speculative escape hatches age into permanent maintenance burden.
2. **Single tier (everything is workflow).** Rejected because endpoint
   APIs are useful for cases where the workflow doesn't fit (testing,
   audit, partial flows). Removing them forces consumers to compose
   protocol details themselves.
3. **Reverse the tier names so endpoint is Tier 1.** Rejected because
   endpoint-first puts protocol mechanics in front of consumers and
   pushes them toward the official-SDK style we explicitly chose not to
   copy.

## Spec citations

The spec doesn't define API tiers; this is a presentation choice for the
SDK. Tier 1 examples cite specific spec sections in their javadoc.

## Related

- ADR-004 — domain-specific clients (basis for Tier 2)
- ADR-005 — SDK overlay on generated code (no `*Raw` in public)
- ADR-008 — API redesign with session abstractions (Tier 1 workflows)
- ADR-012 — package structure (`sdk.domain.*` is Tier 1+2; `sdk.internal.*`
  is hidden)
- ADR-016 — `KsefClient` single entry point
- ADR-020 — testkit philosophy (no public test seam in main artifact)
