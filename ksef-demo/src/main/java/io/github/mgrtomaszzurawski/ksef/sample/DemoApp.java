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
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sample.report.RunReport;
import io.github.mgrtomaszzurawski.ksef.sample.runner.AuthRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.BatchSessionRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.CertificateRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.DemoRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.InvoiceRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.LimitsRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.PeppolRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.PermissionRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.QrCodeRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.RateLimitRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.SecurityRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.SessionRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.TestDataRunner;
import io.github.mgrtomaszzurawski.ksef.sample.runner.TokenRunner;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the KSeF demo application.
 * Exercises all SDK clients against the live KSeF demo server.
 */
public final class DemoApp {

    private static final Logger LOG = LoggerFactory.getLogger(DemoApp.class);
    private static final Path CREDENTIALS_FILE = Path.of("ksef-credentials.properties");
    private static final Path STATE_FILE = Path.of("demo-state.json");
    private static final String ERR_MISSING_TOKEN = "ksef.token is required in credentials file";
    private static final String ERR_MISSING_NIP = "ksef.nip is required in credentials file";
    private static final String ERR_MISSING_ENV = "ksef.environment is required in credentials file";
    private static final String ERR_INVALID_MODE = "Invalid demo.mode: ";
    private static final int EXIT_FAILURE = 1;

    private DemoApp() { }

    public static void main(String[] args) {
        DemoMode mode = resolveMode(args);
        LOG.info("KSeF Demo App starting in {} mode", mode);

        AppProperties properties = AppProperties.load(CREDENTIALS_FILE);
        validateProperties(properties);

        DemoState state;
        try {
            state = DemoState.load(STATE_FILE);
        } catch (IOException exception) {
            LOG.error("Failed to load state file: {}", exception.getMessage());
            System.exit(EXIT_FAILURE);
            return;
        }

        KsefCredentials credentials = buildCredentials(properties);

        try (KsefClient client = KsefClient.builder(KsefEnvironment.custom(properties.environment()))
                .credentials(credentials)
                .retryPolicy(RetryPolicy.builder().build())
                .build()) {

            DemoContext context = new DemoContext(client, mode, state,
                    properties.ksefToken(), properties.nipIdentifier(),
                    credentials.identifier().type(), properties.environment());

            List<DemoRunner> runners = buildRunners(mode);
            DemoSession session = new DemoSession(context);
            RunReport report = session.execute(runners);

            state.setLastRunTimestamp(Instant.now().toString());
            state.setLastRunMode(mode.name());
            try {
                state.save(STATE_FILE);
            } catch (IOException exception) {
                LOG.error("Failed to save state file: {}", exception.getMessage());
            }

            report.print();
            System.exit(report.exitCode());
        }
    }

    private static DemoMode resolveMode(String[] args) {
        String modeStr = System.getProperty("demo.mode");
        if (modeStr == null && args.length > 0) {
            modeStr = args[0];
        }
        if (modeStr == null) {
            return DemoMode.READ_ONLY;
        }
        try {
            return DemoMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException exception) {
            LOG.error(ERR_INVALID_MODE + modeStr);
            System.exit(EXIT_FAILURE);
            return DemoMode.READ_ONLY;
        }
    }

    private static void validateProperties(AppProperties properties) {
        if (properties.ksefToken() == null || properties.ksefToken().isBlank()) {
            LOG.error(ERR_MISSING_TOKEN);
            System.exit(EXIT_FAILURE);
        }
        if (properties.nipIdentifier() == null || properties.nipIdentifier().isBlank()) {
            LOG.error(ERR_MISSING_NIP);
            System.exit(EXIT_FAILURE);
        }
        if (properties.environment() == null || properties.environment().isBlank()) {
            LOG.error(ERR_MISSING_ENV);
            System.exit(EXIT_FAILURE);
        }
    }

    private static KsefCredentials buildCredentials(AppProperties properties) {
        if (properties.hasCertificate()) {
            return new KsefPkcs12Credentials(
                    Path.of(properties.certFile()),
                    properties.certPassword().toCharArray(),
                    properties.nipIdentifier());
        }
        return new KsefTokenCredentials(properties.ksefToken(), properties.nipIdentifier());
    }

    private static List<DemoRunner> buildRunners(DemoMode mode) {
        List<DemoRunner> runners = new ArrayList<>();

        if (mode != DemoMode.CLEANUP) {
            runners.add(new SecurityRunner());
            runners.add(new QrCodeRunner());
        }

        if (mode == DemoMode.AUTH_SAFE || mode == DemoMode.FULL) {
            runners.add(new AuthRunner());
            runners.add(new LimitsRunner());
            runners.add(new RateLimitRunner());
            runners.add(new TokenRunner());
            runners.add(new PermissionRunner());
            runners.add(new CertificateRunner());
            runners.add(new PeppolRunner());
            runners.add(new TestDataRunner());
            runners.add(new BatchSessionRunner());
            runners.add(new SessionRunner());
            runners.add(new InvoiceRunner());
        }

        return runners;
    }
}
