/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import java.util.List;

/**
 * Status information returned by KSeF for various operations.
 *
 * @param code numeric status code (e.g. 200 = success, 415 = session busy)
 * @param description human-readable status description
 * @param details optional additional detail messages
 */
public record StatusInfo(int code, String description, List<String> details) {

}
