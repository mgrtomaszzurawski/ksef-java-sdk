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
 */
public final class PeppolMappers {

    private PeppolMappers() { }

    public static PeppolProvider toPeppolProvider(PeppolProviderRaw raw) {
            return new PeppolProvider(raw.getId(), raw.getName(), raw.getDateCreated());

    }

    public static PeppolProvidersResult toPeppolProvidersResult(QueryPeppolProvidersResponseRaw raw) {
            List<PeppolProvider> mapped = raw.getPeppolProviders().stream().map(PeppolMappers::toPeppolProvider).toList();
            return new PeppolProvidersResult(mapped, raw.getHasMore());

    }

}
