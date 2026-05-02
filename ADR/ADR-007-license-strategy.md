# ADR-007: AGPL-3.0 until 1.0.0, then Apache 2.0

**Date:** 2026-04-03
**Status:** Accepted
**Last verified:** 2026-05-02

## Context

Need to choose license for the SDK. Options considered:
- MIT: Maximum adoption, no protection
- Apache 2.0: Standard for Java SDKs, patent protection
- AGPL-3.0: Copyleft, prevents closed-source use without commercial license
- Dual license (AGPL + commercial): Protection during development, monetization option

Competition (official SDK, alapierre fork) uses MIT.

## Decision

AGPL-3.0 during pre-release development (< 1.0.0). Switch to Apache 2.0 on 1.0.0 release.

## Rationale

- Pre-1.0.0: API is unstable, not ready for production. AGPL prevents someone taking early code and building a closed product before we finish.
- Post-1.0.0: Apache 2.0 removes adoption barriers. Quality of the SDK should speak for itself at that point.
- Single author (copyright holder) can relicense at any time.

## Consequences

- LICENSE.txt starts as AGPL-3.0
- SPDX headers in source files: `AGPL-3.0-only`
- On 1.0.0: replace LICENSE.txt, update SPDX headers, update pom.xml license section
- Old git tags remain under AGPL but are irrelevant (nobody uses pre-release versions)
