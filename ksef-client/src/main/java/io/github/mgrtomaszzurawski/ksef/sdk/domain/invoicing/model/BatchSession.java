/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionResponseRaw;
import java.util.List;

/**
 * Result of opening a batch session.
 *
 * @param referenceNumber session reference number
 * @param partUploadRequests upload instructions for each batch part
 */
public record BatchSession(String referenceNumber, List<PartUploadRequest> partUploadRequests) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static BatchSession from(OpenBatchSessionResponseRaw raw) {
        List<PartUploadRequest> parts = raw.getPartUploadRequests().stream().map(PartUploadRequest::from).toList();
        return new BatchSession(raw.getReferenceNumber(), parts);
    }
}
