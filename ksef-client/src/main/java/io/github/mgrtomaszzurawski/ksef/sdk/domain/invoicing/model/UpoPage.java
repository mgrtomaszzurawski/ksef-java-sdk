/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
 */
public record UpoPage(String referenceNumber, URI downloadUrl, OffsetDateTime downloadUrlExpirationDate) {

}
