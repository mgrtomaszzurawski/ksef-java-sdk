/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
