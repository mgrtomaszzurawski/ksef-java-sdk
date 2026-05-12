/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import java.net.URI;
import java.util.Map;

/**
 * Upload instructions for a batch session part.
 *
 * @param ordinalNumber part sequence number
 * @param method HTTP method to use for upload
 * @param url upload URL
 * @param headers HTTP headers to include in the upload request
 *
 * @since 1.0.0
 */
public record PartUploadRequest(int ordinalNumber, String method, URI url, Map<String, String> headers) {

}
