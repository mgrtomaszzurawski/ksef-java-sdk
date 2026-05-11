/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample;
/**
 * Run mode for the KSeF demo application.
 *
 * <p>Controls which operations each runner executes. Set via
 * {@code demo.mode} system property or in credentials file.</p>
 */
public enum DemoMode {

    /** Unauthenticated operations only: SecurityClient, QrCodeService, TestDataClient (create+remove pairs). */
    READ_ONLY,

    /** Authenticate + all read queries + reversible CRUD (token, permission, certificate generate+revoke). */
    AUTH_SAFE,

    /** Everything: auth + open session + send ONE invoice + close + poll UPO + export. Run once per NIP. */
    FULL,

    /** Read state file, revoke orphaned tokens/permissions/certificates, reset testdata limits. */
    CLEANUP
}
