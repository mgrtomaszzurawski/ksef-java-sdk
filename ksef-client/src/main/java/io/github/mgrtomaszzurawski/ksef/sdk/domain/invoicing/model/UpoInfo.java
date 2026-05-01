/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.UpoResponseRaw;
import java.util.List;

/**
 * UPO (official receipt) information with download pages.
 *
 * @param pages download pages for the UPO
 */
public record UpoInfo(List<UpoPage> pages) {

    public static UpoInfo from(UpoResponseRaw raw) {
        if (raw == null) {
            return null;
        }
        List<UpoPage> mappedPages = raw.getPages() != null
                ? raw.getPages().stream().map(UpoPage::from).toList()
                : List.of();
        return new UpoInfo(mappedPages);
    }
}
