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

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for session operations using the KsefSession API.
 * Opens an online session, sends ONE test invoice, closes, and retrieves UPO.
 * FULL mode only.
 *
 * <p>Demonstrates the simplified API — the SDK handles encryption, hashing,
 * 415 retry on close, and completion polling internally.</p>
 */
public final class SessionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRunner.class);
    private static final String NAME = "session";
    private static final String OP_OPEN_AND_SEND = "openSessionAndSend";
    private static final String OP_UPO = "getUpo";
    private static final String SKIP_NOT_FULL = "FULL mode only";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_AND_SEND, SKIP_NOT_FULL));
            return results;
        }

        long start = System.currentTimeMillis();
        KsefSession session = context.client().openSession(FormCode.FA2);
        try {
            String sessionRef = session.referenceNumber();
            LOG.info("[{}] opened session ref={}", NAME, sessionRef);
            context.setSessionReferenceNumber(sessionRef);
            context.state().setSessionReferenceNumber(sessionRef);

            byte[] invoiceXml = TestInvoiceXml.generate(context.nipIdentifier());
            SendInvoiceResult sendResult = session.send(invoiceXml);
            String invoiceRef = sendResult.referenceNumber();
            LOG.info("[{}] sent invoice ref={}", NAME, invoiceRef);
            context.setInvoiceReferenceNumber(invoiceRef);

            // Close session — SDK handles 415 retry + completion polling
            session.close();

            results.add(RunResult.ok(NAME, OP_OPEN_AND_SEND, elapsed(start),
                    "session=" + sessionRef + ", invoice=" + invoiceRef));

            // Retrieve UPO (available after close)
            long upoStart = System.currentTimeMillis();
            try {
                byte[] upo = session.upo(invoiceRef);
                LOG.info("[{}] UPO retrieved, size={} bytes", NAME, upo.length);
                results.add(RunResult.ok(NAME, OP_UPO, elapsed(upoStart),
                        upo.length + " bytes"));
            } catch (Exception exception) {
                results.add(RunResult.fail(NAME, OP_UPO, elapsed(upoStart),
                        errorMessage(exception)));
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_AND_SEND, elapsed(start),
                    errorMessage(exception)));
            try {
                session.close();
            } catch (Exception closeEx) {
                LOG.warn("[{}] session close after error failed: {}", NAME, closeEx.getMessage());
            }
        }

        return results;
    }
}
