/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Offline invoice issuance: {@code OfflineInvoice} wrapper carrying the
 * underlying invoice + KOD I/II QR codes + offline-mode classification,
 * the {@code OfflineInvoices} build-only facade, and the {@code OfflineMode}
 * enum (consumer-chosen, KSeF-unavailable, emergency).
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline;
