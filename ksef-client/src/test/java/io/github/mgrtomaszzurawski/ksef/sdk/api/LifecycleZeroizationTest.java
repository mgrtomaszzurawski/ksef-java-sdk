/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Lifecycle-hygiene gates for {@link OnlineSession#close()} and
 * {@link KsefClient#close()}.
 *
 * <p>Closes Codex round-9 findings F4 (OnlineSession.close did not zeroize
 * the retained AES key + IV) and F6 (KsefClient.close did not clear
 * sessionContext + publicKeyCache).
 *
 * <p>Both checks use reflection because the fields are private and
 * intentionally not exposed on the public surface — this is a security
 * invariant test, not consumer behaviour.
 */
@WireMockTest
class LifecycleZeroizationTest {

    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_SESSION_REF = "20260418-SE-1111111111-AAAAAAAAAA-01";
    private static final String SESSIONS_BASE = "/v2/sessions";
    private static final String ONLINE_BASE = SESSIONS_BASE + "/online";

    @Test
    void ksefSession_close_zeroizesAesKeyAndInitVector(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        stubCloseAndStatusOk();
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        OnlineSession session = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(sessionClient, TEST_SESSION_REF, aesKey.clone(), iv.clone());

        // when
        session.close();

        // then — both arrays must be all zeros after close
        byte[] storedKey = readByteField(session, "aesKey");
        byte[] storedIv = readByteField(session, "initVector");
        for (byte byteValue : storedKey) {
            assertEquals((byte) 0, byteValue,
                    "AES key must be zeroized after OnlineSession.close() (CWE-316)");
        }
        for (byte byteValue : storedIv) {
            assertEquals((byte) 0, byteValue,
                    "IV must be zeroized after OnlineSession.close() (CWE-316)");
        }
    }

    @Test
    void ksefSession_close_isIdempotentEvenAfterZeroization(WireMockRuntimeInfo wmInfo) {
        // given
        stubCloseAndStatusOk();
        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        runtime.sessionContext().activate(TEST_TOKEN, TEST_SESSION_REF, OffsetDateTime.now().plusHours(1));
        SessionClient sessionClient = new SessionClient(runtime);
        OnlineSession session = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newOnlineSession(sessionClient, TEST_SESSION_REF,
                CryptoService.generateAesKey(), CryptoService.generateIv());

        // when
        session.close();

        // then — second close on a zeroized session must not throw.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(session::close);
    }

    @Test
    void ksefClient_close_clearsSessionContextAndPublicKeyCache(WireMockRuntimeInfo wmInfo) throws Exception {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            // given — force lazy auth so sessionContext + publicKeyCache are populated.
            // authenticate() is not exposed on the public surface; the test
            // forces it via reflection.
            invokeAuthenticate(client);
            // sanity precondition — bearer token is held in sessionContext
            Object sessionContextBefore = readObjectField(client, "sessionContext");
            Object tokenBefore = sessionContextBefore.getClass().getMethod("token").invoke(sessionContextBefore);
            assertEquals(KsefAuthFlowFixture.DEFAULT_TEST_TOKEN, tokenBefore);

            // when
            client.close();

            // then — internal state is cleared
            Object sessionContext = readObjectField(client, "sessionContext");
            Object isActive = sessionContext.getClass().getMethod("isActive").invoke(sessionContext);
            assertFalse((Boolean) isActive,
                    "SessionContext must be cleared after KsefClient.close() (Codex F6)");

            java.util.Map<?, ?> publicKeyCache = (java.util.Map<?, ?>) readObjectField(client, "publicKeyCache");
            assertEquals(0, publicKeyCache.size(),
                    "publicKeyCache must be cleared after KsefClient.close() (Codex F6)");
        }
    }

    private static void invokeAuthenticate(KsefClient client) throws Exception {
        java.lang.reflect.Method method = KsefClient.class.getDeclaredMethod("authenticate");
        method.setAccessible(true);
        method.invoke(client);
    }

    private static void stubCloseAndStatusOk() {
        stubFor(post(urlEqualTo(ONLINE_BASE + "/" + TEST_SESSION_REF + "/close"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
        stubFor(get(urlEqualTo(SESSIONS_BASE + "/" + TEST_SESSION_REF))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody("""
                                {"status":{"code":200,"description":"OK"},
                                 "dateCreated":"2026-04-18T12:00:00+02:00"}""")));
    }

    private static byte[] readByteField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (byte[]) field.get(target);
    }

    private static Object readObjectField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
