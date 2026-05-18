/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.batch;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R1-19 sub: pins {@code submitFromFiles} two-phase preflight semantics.
 *
 * <p>Phase 1A (SAX root-element form-code check) must fail-fast with the
 * offending file path before any encryption / upload begins. Phase 2
 * (streaming XSD validation per file) must do the same on structurally
 * invalid XML. No wire traffic should reach the (unstubbed) batch-open
 * endpoint when either phase rejects — verified by asserting WireMock
 * received zero requests on the batch path.
 */
@WireMockTest
class SubmitBatchFromFilesPreflightTest {

    private static final String FA2_NAMESPACED_FAKTURA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://crd.gov.pl/wzor/2023/06/29/12648/\"/>";
    private static final String UNRECOGNISED_ROOT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<NotAnInvoice/>";
    private static final String FA3_STRUCTURALLY_INVALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://crd.gov.pl/wzor/2025/06/25/13775/\">"
            + "<UnknownElement/></Faktura>";
    private static final String BATCH_OPEN_PATH_REGEX = "/v2/sessions/batch.*";

    private static final BatchOptions FAST_OPTIONS = new BatchOptions(Duration.ofSeconds(30), 1);

    @TempDir
    Path tempDir;

    @Test
    void submitFromFiles_whenFileHasMismatchedFormCode_throwsBeforeAnyWireCall(WireMockRuntimeInfo wmInfo) throws IOException {
        // given — caller declares FA(3) but the file is a FA(2)-namespaced Faktura
        Path mismatched = writeXml("mismatched.xml", FA2_NAMESPACED_FAKTURA);

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {

            // when / then — Phase 1A fails fast with the file path and form-code names
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> client.invoices().batch().submitFromFiles(
                            FormCode.FA3, List.of(mismatched), FAST_OPTIONS));
            assertTrue(thrown.getMessage().contains(mismatched.toString()),
                    () -> "Error message should reference the offending file path: " + thrown.getMessage());
            // Pin both halves of the mismatch — declared (FA2) and expected (FA3).
            assertTrue(thrown.getMessage().contains(FormCode.FA2.toString()),
                    () -> "Error message should reference the declared form code: " + thrown.getMessage());
            assertTrue(thrown.getMessage().contains(FormCode.FA3.toString()),
                    () -> "Error message should reference the expected form code: " + thrown.getMessage());

            // and — the batch-open endpoint was never hit
            assertEquals(0, findAll(postRequestedFor(urlMatching(BATCH_OPEN_PATH_REGEX))).size(),
                    "Phase 1A must fail-fast before any wire traffic to /sessions/batch");
        }
    }

    @Test
    void submitFromFiles_whenFileHasUnrecognisedRoot_throwsBeforeAnyWireCall(WireMockRuntimeInfo wmInfo) throws IOException {
        // given — file's root element is neither Faktura nor UBL Invoice/CreditNote
        Path foreign = writeXml("foreign.xml", UNRECOGNISED_ROOT_XML);

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {

            // when / then — Phase 1A surfaces the unrecognised-root error with the file path
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> client.invoices().batch().submitFromFiles(
                            FormCode.FA3, List.of(foreign), FAST_OPTIONS));
            assertTrue(thrown.getMessage().contains(foreign.toString()),
                    () -> "Error message should reference the file path: " + thrown.getMessage());

            // and — zero wire calls reached the batch path
            assertEquals(0, findAll(postRequestedFor(urlMatching(BATCH_OPEN_PATH_REGEX))).size());
        }
    }

    @Test
    void submitFromFiles_whenFa3FilePassesPhase1AButFailsXsdInPhase2_throwsXmlValidationException(WireMockRuntimeInfo wmInfo) throws IOException {
        // given — file declares FA(3) namespace (Phase 1A passes) but the body
        // is XSD-invalid (no Naglowek/Podmiot1/Podmiot2/Fa) — Phase 2 must reject it
        Path invalid = writeXml("xsd-invalid.xml", FA3_STRUCTURALLY_INVALID_XML);

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {

            // when / then — KsefXmlValidationException carries the file path in its message
            KsefXmlValidator.KsefXmlValidationException thrown = assertThrows(
                    KsefXmlValidator.KsefXmlValidationException.class,
                    () -> client.invoices().batch().submitFromFiles(
                            FormCode.FA3, List.of(invalid), FAST_OPTIONS));
            assertTrue(thrown.getMessage().contains(invalid.toString()),
                    () -> "Error message should reference the offending file path: " + thrown.getMessage());

            // and — Phase 2 also fails before any wire call (no upload triggered)
            assertEquals(0, findAll(postRequestedFor(urlMatching(BATCH_OPEN_PATH_REGEX))).size());
        }
    }

    // Happy-path coverage for submitFromFiles (preflight passes → batch open / part upload /
    // close / status / UPO fetch) is intentionally out of scope here — that's the full
    // SubmitBatchTest scaffolding territory (15+ WireMock stubs). This test class focuses
    // on the preflight regression contract: bad inputs must fail-fast with the file path
    // before any wire traffic reaches /sessions/batch.

    private Path writeXml(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
