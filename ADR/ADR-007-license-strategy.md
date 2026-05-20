# ADR-007: License strategy — AGPL-3.0-only retained at 1.0.0

**Date:** 2026-04-03 (initial); revised 2026-05-03
**Status:** Accepted (revised)
**Last verified:** 2026-05-07

## Context

Need to choose a license for the SDK. Options considered:

- MIT: maximum adoption, no protection.
- Apache 2.0: standard for Java SDKs, patent protection.
- AGPL-3.0-only: strong copyleft, prevents closed-source use without a commercial license.
- Dual license (AGPL + commercial): protection during development, monetisation option.

Competition (official CIRFMF SDK, alapierre fork) uses MIT.

## Decision history

### Original decision (2026-04-03) — DEPRECATED

> AGPL-3.0-only during pre-release development (< 1.0.0). Switch to Apache 2.0
> on 1.0.0 release.

This decision was based on the assumption that adoption-barrier removal at
1.0.0 outweighed free-rider protection.

### Revised decision (2026-05-03) — current

**Stay on AGPL-3.0-only indefinitely. No automatic switch to Apache 2.0 at
1.0.0 or any later version.** A future move to a different license is a
separate architectural decision documented in its own ADR; nothing in this
ADR mandates it.

## Rationale for the revised decision

The original "switch to Apache at 1.0" rationale rested on the trade-off
between adoption reach and protection. Reconsidering at the 1.0 boundary:

- The project is solo-maintained. The author put substantial effort into
  the SDK, including a full audit of the KSeF specification, an
  implementation of the documented incremental sync algorithm, public
  cryptographic facade, KOD II QR signing service, and a release-boundary
  test harness. The free-rider concern is real and not hypothetical.
- Apache 2.0 does not protect against commercial closed-source integration
  by Polish ERP vendors who would build paid KSeF connectors on top of
  this SDK and contribute nothing back. AGPL-3.0-only prevents that
  without an explicit commercial agreement.
- Apache 2.0 and AGPL-3.0-only have **functionally equivalent** warranty
  disclaimers (Apache §7-8, AGPL §15-16). The license string does not
  affect the user's exposure to "this didn't work as expected"
  complaints; what matters there is README messaging, not license text.
- Two of the three concerns the author had at the 1.0 boundary
  ("battle-tested-team illusion" and liability fear) are not addressed
  by either license. The one concern that licensing materially affects
  (free-riding) is addressed by AGPL, not Apache.

The "single author can relicense at any time" point still holds: a
future relicense (commercial dual-license, Apache, BSL, etc.) remains
possible if and when the author chooses to make that decision
deliberately. The point of this revision is to remove the implicit
auto-switch that was scheduled to happen at 1.0.

## Consequences

- `LICENSE.txt` stays as the AGPL-3.0-only text.
- SPDX headers across all source files: `AGPL-3.0-only`.
- `pom.xml` `<licenses>` block: GNU Affero General Public License v3.0.
- README badge: AGPL v3.
- 1.0.0 ships under AGPL-3.0-only.
- A README disclaimer ("solo-maintained, tested against KSeF demo
  environment, no warranty") addresses the team-illusion / liability
  concerns independently of the license choice.

## Compatibility note

Earlier `0.1.x` snapshots also shipped under AGPL-3.0-only. Pre-1.0
adopters (none known) face no licensing change at the 1.0.0 boundary
under this revised decision.

## Related

- ADR-006 — separate SDK and sample-app modules (sample app is GPL-compatible
  for live testing; SDK license decision is independent).
- ADR-021 — public API tiers; the license decision and the API surface
  decision are independent.
