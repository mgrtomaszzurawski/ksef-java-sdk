/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * JPMS consumer compile gate (TC-ARCH-001) — confirms the SDK's public
 * surface is fully resolvable from a downstream named module without
 * any {@code --add-exports} or {@code --add-reads} hacks.
 *
 * <p>If a future change moves a public type into a non-exported package
 * (the way {@code AuthSession} was missing its export in commit
 * {@code 0d1c264} until Codex caught it as F1), this consumer module
 * fails to compile.
 */
module io.github.mgrtomaszzurawski.ksef.jpms.consumer {
    requires io.github.mgrtomaszzurawski.ksef;
}
