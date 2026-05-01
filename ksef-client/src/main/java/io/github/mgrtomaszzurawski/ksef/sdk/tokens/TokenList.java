/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens;
import io.github.mgrtomaszzurawski.ksef.sdk.tokens.TokenListItem;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;

import java.util.List;

/**
 * List of KSeF API tokens.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param tokens token items
 */
public record TokenList(String continuationToken, List<TokenListItem> tokens) {

    public static TokenList from(QueryTokensResponseRaw raw) {
        List<TokenListItem> mapped = raw.getTokens() != null
                ? raw.getTokens().stream().map(TokenListItem::from).toList()
                : List.of();
        return new TokenList(raw.getContinuationToken(), mapped);
    }
}
