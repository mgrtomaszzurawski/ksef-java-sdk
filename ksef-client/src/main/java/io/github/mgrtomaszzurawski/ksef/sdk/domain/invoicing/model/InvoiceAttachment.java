/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * A structured attachment to the invoice — the typed view of the
 * {@code Faktura/Zalacznik} node. Null on
 * {@code InvoiceDocument.attachment()} when the invoice carries no
 * attachment. Holds one or more data blocks, each with descriptors,
 * text and tables.
 *
 * @param blocks the attachment data blocks ({@code BlokDanych})
 *
 * @since 0.1.0
 */
public record InvoiceAttachment(List<AttachmentBlock> blocks) {

    public InvoiceAttachment {
        blocks = List.copyOf(blocks);
    }
}
