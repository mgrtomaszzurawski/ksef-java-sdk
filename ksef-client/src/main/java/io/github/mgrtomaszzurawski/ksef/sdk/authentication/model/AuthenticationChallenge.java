/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.authentication.model;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import java.time.OffsetDateTime;

/**
 * Challenge issued by KSeF for authentication.
 *
 * @param challenge the challenge string to sign or encrypt
 * @param timestamp server timestamp of the challenge
 * @param timestampMs server timestamp in milliseconds (epoch)
 * @param clientIp the client IP as seen by the server
 */
public record AuthenticationChallenge(
        String challenge,
        OffsetDateTime timestamp,
        long timestampMs,
        String clientIp) {

    public static AuthenticationChallenge from(AuthenticationChallengeResponseRaw raw) {
        return new AuthenticationChallenge(
                raw.getChallenge(),
                raw.getTimestamp(),
                raw.getTimestampMs() != null ? raw.getTimestampMs() : 0L,
                raw.getClientIp());
    }
}
