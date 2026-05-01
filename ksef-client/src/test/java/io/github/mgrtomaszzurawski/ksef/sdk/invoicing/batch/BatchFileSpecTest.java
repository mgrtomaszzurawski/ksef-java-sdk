/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.batch;

import java.util.List;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchFileSpecTest {

    private static final long FILE_SIZE = 1024L;
    private static final long PART_SIZE = 512L;
    private static final int PART_ONE = 1;
    private static final int PART_TWO = 2;
    private static final byte FIRST_HASH_BYTE = (byte) 0xAB;
    private static final byte MUTATED_BYTE = (byte) 0xFF;
    private static final int HASH_LENGTH = 32;

    private static byte[] sampleHash() {
        byte[] hash = new byte[HASH_LENGTH];
        hash[0] = FIRST_HASH_BYTE;
        return hash;
    }

    private static BatchFileSpec.Part samplePart(int ordinal) {
        return new BatchFileSpec.Part(ordinal, PART_SIZE, sampleHash());
    }

    // --- BatchFileSpec construction ---

    @Test
    void create_whenValidInputs_createsSpec() {
        // given
        byte[] hash = sampleHash();
        List<BatchFileSpec.Part> parts = List.of(samplePart(PART_ONE));

        // when
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, hash, parts);

        // then
        assertEquals(FILE_SIZE, spec.fileSize());
        assertArrayEquals(hash, spec.fileHash());
        assertEquals(1, spec.parts().size());
    }

    @Test
    void create_whenNullFileHash_throwsNullPointerException() {
        // given
        List<BatchFileSpec.Part> parts = List.of(samplePart(PART_ONE));

        // when / then
        assertThrows(NullPointerException.class,
                () -> new BatchFileSpec(FILE_SIZE, null, parts));
    }

    @Test
    void create_whenNullParts_throwsNullPointerException() {
        // given
        byte[] hash = sampleHash();

        // when / then
        assertThrows(NullPointerException.class,
                () -> new BatchFileSpec(FILE_SIZE, hash, null));
    }

    @Test
    void create_whenEmptyParts_throwsIllegalArgumentException() {
        // given
        byte[] hash = sampleHash();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new BatchFileSpec(FILE_SIZE, hash, List.of()));
    }

    @Test
    void create_whenMultipleParts_acceptsAll() {
        // given
        byte[] hash = sampleHash();
        List<BatchFileSpec.Part> parts = List.of(samplePart(PART_ONE), samplePart(PART_TWO));

        // when
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, hash, parts);

        // then
        assertEquals(2, spec.parts().size());
        assertEquals(PART_ONE, spec.parts().get(0).ordinalNumber());
        assertEquals(PART_TWO, spec.parts().get(1).ordinalNumber());
    }

    // --- BatchFileSpec defensive copy of fileHash ---

    @Test
    void fileHash_whenInputMutatedAfterConstruction_returnsOriginalHash() {
        // given
        byte[] hash = sampleHash();
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, hash, List.of(samplePart(PART_ONE)));

        // when — mutate the original input
        hash[0] = MUTATED_BYTE;

        // then — spec retains the original value
        assertEquals(FIRST_HASH_BYTE, spec.fileHash()[0]);
    }

    @Test
    void fileHash_whenReturnedArrayMutated_doesNotAffectSpec() {
        // given
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, sampleHash(), List.of(samplePart(PART_ONE)));
        byte[] returned = spec.fileHash();

        // when — mutate the returned array
        returned[0] = MUTATED_BYTE;

        // then — next call returns the original value
        assertEquals(FIRST_HASH_BYTE, spec.fileHash()[0]);
    }

    @Test
    void fileHash_returnsNewArrayOnEachCall() {
        // given
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, sampleHash(), List.of(samplePart(PART_ONE)));

        // when
        byte[] first = spec.fileHash();
        byte[] second = spec.fileHash();

        // then — different array instances (defensive copy)
        assertNotSame(first, second);
    }

    // --- BatchFileSpec parts immutability ---

    @Test
    void parts_whenSourceListMutated_doesNotAffectSpec() {
        // given
        java.util.ArrayList<BatchFileSpec.Part> mutableSource = new java.util.ArrayList<>();
        mutableSource.add(samplePart(PART_ONE));
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, sampleHash(), mutableSource);

        // when — mutate source after construction
        mutableSource.add(samplePart(PART_TWO));

        // then — spec retains only the original size
        assertEquals(1, spec.parts().size());
    }

    @Test
    void parts_whenAttemptedToModify_throwsUnsupportedOperationException() {
        // given
        BatchFileSpec spec = new BatchFileSpec(FILE_SIZE, sampleHash(), List.of(samplePart(PART_ONE)));

        // when / then — returned list is immutable
        assertThrows(UnsupportedOperationException.class,
                () -> spec.parts().add(samplePart(PART_TWO)));
    }

    // --- BatchFileSpec.Part construction ---

    @Test
    void partCreate_whenValidInputs_createsPart() {
        // given
        byte[] hash = sampleHash();

        // when
        BatchFileSpec.Part part = new BatchFileSpec.Part(PART_ONE, PART_SIZE, hash);

        // then
        assertEquals(PART_ONE, part.ordinalNumber());
        assertEquals(PART_SIZE, part.fileSize());
        assertArrayEquals(hash, part.fileHash());
    }

    @Test
    void partCreate_whenNullFileHash_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> new BatchFileSpec.Part(PART_ONE, PART_SIZE, null));
    }

    // --- BatchFileSpec.Part defensive copy of fileHash ---

    @Test
    void partFileHash_whenInputMutatedAfterConstruction_returnsOriginalHash() {
        // given
        byte[] hash = sampleHash();
        BatchFileSpec.Part part = new BatchFileSpec.Part(PART_ONE, PART_SIZE, hash);

        // when — mutate the original input
        hash[0] = MUTATED_BYTE;

        // then — part retains the original value
        assertEquals(FIRST_HASH_BYTE, part.fileHash()[0]);
    }

    @Test
    void partFileHash_whenReturnedArrayMutated_doesNotAffectPart() {
        // given
        BatchFileSpec.Part part = new BatchFileSpec.Part(PART_ONE, PART_SIZE, sampleHash());
        byte[] returned = part.fileHash();

        // when — mutate the returned array
        returned[0] = MUTATED_BYTE;

        // then — next call returns the original value
        assertEquals(FIRST_HASH_BYTE, part.fileHash()[0]);
    }

    @Test
    void partFileHash_returnsNewArrayOnEachCall() {
        // given
        BatchFileSpec.Part part = new BatchFileSpec.Part(PART_ONE, PART_SIZE, sampleHash());

        // when
        byte[] first = part.fileHash();
        byte[] second = part.fileHash();

        // then
        assertNotSame(first, second);
    }
}
