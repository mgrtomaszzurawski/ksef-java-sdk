/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Invoice document hierarchy: write-side {@code Invoice} interface +
 * typed FA(2)/FA(3)/PEF/PEF_KOR classes, read-side {@code InvoiceDocument}
 * interface + matching typed wrappers, and the {@code UnrecognizedInvoiceDocument}
 * fallback for custom {@code FormCode} entries without a registered
 * {@code KsefInvoiceTypes} binding.
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;
