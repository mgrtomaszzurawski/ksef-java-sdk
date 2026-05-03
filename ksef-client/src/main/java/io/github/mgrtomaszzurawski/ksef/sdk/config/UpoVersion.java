/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

/**
 * Selects the UPO XML schema version returned by KSeF for invoice / session
 * receipts.
 *
 * <p>KSeF lets clients opt into a newer UPO schema by setting the
 * {@code X-KSeF-Feature} HTTP header on requests that retrieve UPO. The
 * value mapping is internal:
 *
 * <ul>
 *   <li>{@link #DEFAULT} — server default (no header sent)</li>
 *   <li>{@link #V4_3} — UPO schema {@code upo-v4-3}; corresponds to
 *       header {@code X-KSeF-Feature: upo-v4-3}</li>
 * </ul>
 *
 * <p>Apply via {@link FeaturePolicy} on {@code KsefClient.Builder}.
 *
 * <p>Spec citation: {@code ksef-docs/api-changelog.md} feature-header
 * entries; REQ-MISC test gap (UPO links) in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}.
 */
public enum UpoVersion {

    /** No UPO version override; server default applies. */
    DEFAULT,

    /** UPO schema {@code upo-v4-3}. */
    V4_3
}
