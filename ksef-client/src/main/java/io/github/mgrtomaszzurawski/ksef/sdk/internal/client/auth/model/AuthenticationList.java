/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationListResponseRaw;
import java.util.List;

/**
 * List of authentication sessions.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param items authentication session items
 */
public record AuthenticationList(String continuationToken, List<AuthenticationListItem> items) {

    public static AuthenticationList from(AuthenticationListResponseRaw raw) {
        List<AuthenticationListItem> mappedItems = raw.getItems().stream().map(AuthenticationListItem::from).toList();
        return new AuthenticationList(raw.getContinuationToken(), mappedItems);
    }
}
