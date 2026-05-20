/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * A page of UPO (official receipt) download information.
 *
 * @param referenceNumber UPO reference number
 * @param downloadUrl URL to download the UPO
 * @param downloadUrlExpirationDate when the download URL expires
 *
 * @since 0.1.0
 */
public record UpoPage(String referenceNumber, URI downloadUrl, OffsetDateTime downloadUrlExpirationDate) {

}
