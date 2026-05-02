/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
        List<AuthenticationListItem> mappedItems = raw.getItems() != null
                ? raw.getItems().stream().map(AuthenticationListItem::from).toList()
                : List.of();
        return new AuthenticationList(raw.getContinuationToken(), mappedItems);
    }
}
