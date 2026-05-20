/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnavailableException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class HttpSupportKsefUnavailableTest {

    private static final String TEST_PATH = "/test/resource";
    private static final String STUB_PATH = "/v2/test/resource";
    private static final String OPERATION_NAME = "testGet";
    private static final String UNAVAILABLE_BODY = "{\"error\":\"unavailable\"}";
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;

    @Test
    void get_whenServerReturns503_throwsKsefUnavailableException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(STUB_PATH))
                .willReturn(aResponse()
                        .withStatus(HTTP_SERVICE_UNAVAILABLE)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(UNAVAILABLE_BODY)));

        HttpSupport http = createHttpSupport(wmInfo);

        // when / then
        KsefUnavailableException exception = assertThrows(KsefUnavailableException.class,
                () -> http.get(TEST_PATH, AuthenticationChallengeResponseRaw.class, OPERATION_NAME));
        assertEquals(HTTP_SERVICE_UNAVAILABLE, exception.statusCode());
    }

    private static HttpSupport createHttpSupport(WireMockRuntimeInfo wmInfo) {
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        return new HttpSupport(runtime);
    }
}
