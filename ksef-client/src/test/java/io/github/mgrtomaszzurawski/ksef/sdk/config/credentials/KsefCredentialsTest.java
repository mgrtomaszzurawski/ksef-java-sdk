/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KsefCredentialsTest {

    private static final String VALID_NIP = "1111111111";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test-token";
    private static final String SHORT_NIP = "123456789";
    private static final String LONG_NIP = "11111111111";
    private static final String ALPHA_NIP = "12345678AB";
    private static final String BLANK_TOKEN = "   ";
    private static final String KEYSTORE_PASSWORD = "test-password";
    private static final String KEYSTORE_PATH = "/tmp/test.p12";

    @Test
    void validateNip_whenValidNip_doesNotThrow() {
        // given
        String nip = VALID_NIP;

        // when / then
        assertDoesNotThrow(() -> KsefCredentials.validateNip(nip));
    }

    @Test
    void validateNip_whenNullNip_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> KsefCredentials.validateNip(null));
    }

    @Test
    void validateNip_whenTooShort_throwsIllegalArgument() {
        // given
        String nip = SHORT_NIP;

        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefCredentials.validateNip(nip));
    }

    @Test
    void validateNip_whenTooLong_throwsIllegalArgument() {
        // given
        String nip = LONG_NIP;

        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefCredentials.validateNip(nip));
    }

    @Test
    void validateNip_whenContainsLetters_throwsIllegalArgument() {
        // given
        String nip = ALPHA_NIP;

        // when / then
        assertThrows(IllegalArgumentException.class, () -> KsefCredentials.validateNip(nip));
    }

    @Test
    void tokenCredentials_whenValidInputs_createsSuccessfully() {
        // given / when
        KsefTokenCredentials credentials = new KsefTokenCredentials(VALID_TOKEN, VALID_NIP);

        // then
        assertEquals(VALID_TOKEN, credentials.ksefToken());
        assertEquals(VALID_NIP, credentials.nip());
    }

    @Test
    void tokenCredentials_whenNullToken_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefTokenCredentials(null, VALID_NIP));
    }

    @Test
    void tokenCredentials_whenBlankToken_throwsIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new KsefTokenCredentials(BLANK_TOKEN, VALID_NIP));
    }

    @Test
    void tokenCredentials_toString_redactsToken() {
        // given
        KsefTokenCredentials credentials = new KsefTokenCredentials(VALID_TOKEN, VALID_NIP);

        // when
        String result = credentials.toString();

        // then
        assertFalse(result.contains(VALID_TOKEN),
                "toString must not expose the token value");
        assertNotNull(result);
    }

    @Test
    void certificateCredentials_whenNullCertificate_throwsNullPointerException() throws Exception {
        // given
        TestCertificates certs = TestCertificates.generateRsa();
        java.security.PrivateKey privateKey = certs.privateKey();

        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefCertificateCredentials(null, privateKey, VALID_NIP));
    }

    @Test
    void certificateCredentials_whenNullKey_throwsNullPointerException() throws Exception {
        // given
        TestCertificates certs = TestCertificates.generateRsa();
        java.security.cert.X509Certificate cert = certs.certificate();

        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefCertificateCredentials(cert, null, VALID_NIP));
    }

    @Test
    void pkcs12Credentials_whenNullPath_throwsNullPointerException() {
        // given
        char[] password = KEYSTORE_PASSWORD.toCharArray();

        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefPkcs12Credentials(null, password, VALID_NIP));
    }

    @Test
    void pkcs12Credentials_whenNullPassword_throwsNullPointerException() {
        // given
        Path path = Path.of(KEYSTORE_PATH);

        // when / then
        assertThrows(NullPointerException.class,
                () -> new KsefPkcs12Credentials(path, null, VALID_NIP));
    }

    @Test
    void pkcs12Credentials_toString_redactsPassword() {
        // given
        KsefPkcs12Credentials credentials = new KsefPkcs12Credentials(
                Path.of(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray(), VALID_NIP);

        // when
        String result = credentials.toString();

        // then
        assertFalse(result.contains(KEYSTORE_PASSWORD),
                "toString must not expose the password");
        assertNotNull(result);
    }

    private static final String PEPPOL_VALUE = "PPL000123";
    private static final String INTERNAL_VALUE = "1111111111-12345";

    @Test
    void tokenCredentials_whenIdentifierConstructor_returnsIdentifier() {
        // given
        KsefIdentifier identifier = KsefIdentifier.peppolId(PEPPOL_VALUE);

        // when
        KsefTokenCredentials credentials = new KsefTokenCredentials(VALID_TOKEN, identifier);

        // then
        assertEquals(identifier, credentials.identifier());
        assertEquals(KsefIdentifier.Type.PEPPOL_ID, credentials.identifier().type());
    }

    @Test
    void tokenCredentials_whenLegacyNipConstructor_identifierIsNip() {
        // when
        KsefTokenCredentials credentials = new KsefTokenCredentials(VALID_TOKEN, VALID_NIP);

        // then
        assertEquals(KsefIdentifier.Type.NIP, credentials.identifier().type());
        assertEquals(VALID_NIP, credentials.identifier().value());
        assertEquals(VALID_NIP, credentials.nip());
    }

    @Test
    void nip_whenIdentifierIsPeppol_throwsIllegalState() {
        // given
        KsefTokenCredentials credentials = new KsefTokenCredentials(
                VALID_TOKEN, KsefIdentifier.peppolId(PEPPOL_VALUE));

        // when / then
        assertThrows(IllegalStateException.class, credentials::nip);
    }

    @Test
    void nip_whenIdentifierIsInternalId_throwsIllegalState() {
        // given
        KsefTokenCredentials credentials = new KsefTokenCredentials(
                VALID_TOKEN, KsefIdentifier.internalId(INTERNAL_VALUE));

        // when / then
        assertThrows(IllegalStateException.class, credentials::nip);
    }
}
