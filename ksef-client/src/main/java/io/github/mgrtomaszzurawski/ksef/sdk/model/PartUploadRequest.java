/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.PartUploadRequestRaw;

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

    public static PartUploadRequest from(PartUploadRequestRaw raw) {
        return new PartUploadRequest(
                raw.getOrdinalNumber() != null ? raw.getOrdinalNumber() : 0,
                raw.getMethod(),
                raw.getUrl(),
                raw.getHeaders() != null ? Map.copyOf(raw.getHeaders()) : Map.of());
    }
}
