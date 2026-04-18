/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectTypeRaw;

/**
 * Type of test subject in KSeF.
 */
public enum TestSubjectType {

    ENFORCEMENT_AUTHORITY,
    VAT_GROUP,
    JST;

    public SubjectTypeRaw toRaw() {
        return switch (this) {
            case ENFORCEMENT_AUTHORITY -> SubjectTypeRaw.ENFORCEMENT_AUTHORITY;
            case VAT_GROUP -> SubjectTypeRaw.VAT_GROUP;
            case JST -> SubjectTypeRaw.JST;
        };
    }
}
