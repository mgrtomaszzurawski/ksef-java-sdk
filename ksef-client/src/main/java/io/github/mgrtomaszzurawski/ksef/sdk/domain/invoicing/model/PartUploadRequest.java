/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.net.URI;
import java.util.Map;

/**
 * Upload instructions for a batch session part.
 *
 * @param ordinalNumber part sequence number
 * @param method HTTP method to use for upload
 * @param url upload URL
 * @param headers HTTP headers to include in the upload request
 */
public record PartUploadRequest(int ordinalNumber, String method, URI url, Map<String, String> headers) {

}
