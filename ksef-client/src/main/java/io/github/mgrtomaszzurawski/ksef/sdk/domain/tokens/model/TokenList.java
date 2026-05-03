/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import java.util.List;

/**
 * List of KSeF API tokens.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param tokens token items
 */
public record TokenList(String continuationToken, List<TokenListItem> tokens) {

}
