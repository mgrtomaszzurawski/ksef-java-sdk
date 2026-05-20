/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.policy;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Codex 2026-05-05 #7 / F6 — public AuthorizationPolicy validation
 * mirrors the OpenAPI {@code AllowedIps} schema regex constraints.
 */
class AuthorizationPolicyTest {

    @Test
    void onlyAddress_factoryProducesSingleAddressPolicy() {
        // when
        AuthorizationPolicy policy = AuthorizationPolicy.onlyAddress("192.168.1.10");

        // then
        assertEquals(List.of("192.168.1.10"), policy.ip4Addresses());
        assertEquals(List.of(), policy.ip4Ranges());
        assertEquals(List.of(), policy.ip4Masks());
    }

    @Test
    void onlyCidr_factoryProducesSingleMaskPolicy() {
        // when
        AuthorizationPolicy policy = AuthorizationPolicy.onlyCidr("10.0.0.0/24");

        // then
        assertEquals(List.of("10.0.0.0/24"), policy.ip4Masks());
    }

    @Test
    void canonicalConstructor_acceptsAllThreeKinds() {
        // when
        AuthorizationPolicy policy = new AuthorizationPolicy(
                List.of("192.168.0.10", "10.0.0.5"),
                List.of("172.16.0.1-172.16.0.255"),
                List.of("192.168.0.0/24", "10.0.0.0/8"));

        // then
        assertEquals(2, policy.ip4Addresses().size());
        assertEquals(1, policy.ip4Ranges().size());
        assertEquals(2, policy.ip4Masks().size());
    }

    @Test
    void canonicalConstructor_rejectsInvalidAddress() {
        // given
        List<String> invalidAddrs = List.of("not-an-ip");
        List<String> emptyRanges = List.of();
        List<String> emptyMasks = List.of();
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(invalidAddrs, emptyRanges, emptyMasks));
        assertTrue(ex.getMessage().contains("not a valid IPv4 address"));
    }

    @Test
    void canonicalConstructor_rejectsInvalidRange() {
        // given
        List<String> emptyAddrs = List.of();
        List<String> invalidRanges = List.of("10.0.0.1-bad");
        List<String> emptyMasks = List.of();
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(emptyAddrs, invalidRanges, emptyMasks));
    }

    @Test
    void canonicalConstructor_rejectsInvalidCidr() {
        // given
        List<String> emptyAddrs = List.of();
        List<String> emptyRanges = List.of();
        List<String> invalidMasks = List.of("10.0.0.0/33");
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(emptyAddrs, emptyRanges, invalidMasks));
    }

    @Test
    void canonicalConstructor_rejectsTooManyEntries() {
        // given
        List<String> eleven = List.of("1.1.1.1", "1.1.1.2", "1.1.1.3", "1.1.1.4", "1.1.1.5",
                "1.1.1.6", "1.1.1.7", "1.1.1.8", "1.1.1.9", "1.1.1.10", "1.1.1.11");
        List<String> emptyRanges = List.of();
        List<String> emptyMasks = List.of();

        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(eleven, emptyRanges, emptyMasks));
        assertTrue(ex.getMessage().contains("exceeds spec limit"));
    }
}
