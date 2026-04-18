/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
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
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;

import java.util.List;

/**
 * SecurityClient operations (public key fetch) are now handled automatically
 * by KsefClient during authenticate() and openSession(). This runner is a no-op.
 */
public final class SecurityRunner implements DemoRunner {

    private static final String NAME = "security";
    private static final String OP_GET_CERTS = "getPublicKeyCertificates";
    private static final String SKIP_REASON = "handled automatically by SDK during authenticate()";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        return List.of(RunResult.skip(NAME, OP_GET_CERTS, SKIP_REASON));
    }
}
