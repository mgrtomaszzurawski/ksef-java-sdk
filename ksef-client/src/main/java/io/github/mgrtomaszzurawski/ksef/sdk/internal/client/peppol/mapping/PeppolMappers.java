/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.PeppolProviderRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPeppolProvidersResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import java.util.List;

/**
 * Internal mappers from generated {@code *Raw} types to public peppol
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 0.1.0
 */
public final class PeppolMappers {

    private PeppolMappers() { }

    public static PeppolProvider toPeppolProvider(PeppolProviderRaw rawValue) {
        return new PeppolProvider(rawValue.getId(), rawValue.getName(), rawValue.getDateCreated());
    }

    public static PeppolProvidersResult toPeppolProvidersResult(QueryPeppolProvidersResponseRaw rawValue) {
        List<PeppolProvider> mapped = rawValue.getPeppolProviders().stream().map(PeppolMappers::toPeppolProvider).toList();
        return new PeppolProvidersResult(mapped, rawValue.getHasMore());
    }

}
