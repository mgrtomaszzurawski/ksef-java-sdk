/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * R2-11a verification: {@code Permissions.grant(PermissionGrantRequest)}
 * routes each sealed subtype to the matching wire endpoint. The
 * server response shape is identical (per ADR-032 sync-default),
 * so the proof of correct dispatch is the URL each request hits.
 */
@WireMockTest
class PermissionsSealedDispatchTest {

    private static final String TEST_PESEL = "12345678901";
    private static final String TEST_NIP_TARGET = "1234567890";
    private static final String OPERATION_DESCRIPTION = "Sealed dispatch test";
    private static final String FIRST_NAME = "Test";
    private static final String LAST_NAME = "Subject";
    private static final String ENTITY_FULL_NAME = "Test Sp. z o.o.";
    private static final String OPERATION_REFERENCE = "20251019-PM-1A2B3C4D5E-FF0011-AA";
    private static final String PATH_GRANT_PERSONS = "/v2/permissions/persons/grants";
    private static final String PATH_GRANT_ENTITIES = "/v2/permissions/entities/grants";
    private static final String PATH_OPERATIONS_PREFIX = "/v2/permissions/operations/";

    private static final String OPERATION_REF_RESPONSE = """
            {"referenceNumber":"%s"}
            """.formatted(OPERATION_REFERENCE);

    private static final String OPERATION_STATUS_TERMINAL_RESPONSE = """
            {"status":{"code":200,"description":"OK"}}
            """;

    @Test
    void grant_personRequest_postsToPersonsEndpoint(WireMockRuntimeInfo wmInfo) {
        stubAcceptedAt(PATH_GRANT_PERSONS);
        stubOperationStatusTerminal();

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            PermissionGrantRequest request = PersonPermissionGrantBuilder.forPesel(TEST_PESEL)
                    .description(OPERATION_DESCRIPTION)
                    .personDetails(FIRST_NAME, LAST_NAME)
                    .invoiceRead()
                    .build();

            PermissionOperationStatus status = client.permissions().grant(request);
            assertEquals(OPERATION_REFERENCE, status.referenceNumber());
            verify(postRequestedFor(urlEqualTo(PATH_GRANT_PERSONS)));
        }
    }

    @Test
    void grant_entityRequest_postsToEntitiesEndpoint(WireMockRuntimeInfo wmInfo) {
        stubAcceptedAt(PATH_GRANT_ENTITIES);
        stubOperationStatusTerminal();

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            PermissionGrantRequest request = EntityPermissionGrantBuilder.forNip(TEST_NIP_TARGET)
                    .description(OPERATION_DESCRIPTION)
                    .entityDetails(ENTITY_FULL_NAME)
                    .invoiceRead()
                    .build();

            PermissionOperationStatus status = client.permissions().grant(request);
            assertEquals(OPERATION_REFERENCE, status.referenceNumber());
            verify(postRequestedFor(urlEqualTo(PATH_GRANT_ENTITIES)));
        }
    }

    @Test
    void grant_nullRequest_throwsNullPointer(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            var permissions = client.permissions();
            assertThrows(NullPointerException.class, () -> permissions.grant(null));
        }
    }

    private static void stubAcceptedAt(String path) {
        stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_ACCEPTED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_REF_RESPONSE)));
    }

    private static void stubOperationStatusTerminal() {
        stubFor(get(urlMatching(PATH_OPERATIONS_PREFIX + OPERATION_REFERENCE))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPERATION_STATUS_TERMINAL_RESPONSE)));
    }
}
