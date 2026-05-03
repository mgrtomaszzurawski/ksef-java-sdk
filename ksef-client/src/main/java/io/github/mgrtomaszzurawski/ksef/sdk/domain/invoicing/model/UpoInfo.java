/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * UPO (official receipt) information with download pages.
 *
 * @param pages download pages for the UPO
 */
public record UpoInfo(List<UpoPage> pages) {

}
