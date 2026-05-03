/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * Result of opening a batch session.
 *
 * @param referenceNumber session reference number
 * @param partUploadRequests upload instructions for each batch part
 */
public record BatchSession(String referenceNumber, List<PartUploadRequest> partUploadRequests) {

}
