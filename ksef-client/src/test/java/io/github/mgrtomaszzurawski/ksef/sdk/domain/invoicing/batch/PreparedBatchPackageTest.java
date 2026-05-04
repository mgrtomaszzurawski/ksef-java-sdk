/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreparedBatchPackageTest {

    private static final int AES_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;
    private static final long PART_SIZE = 1024L;
    private static final byte FILL_KEY_BYTE = (byte) 0xAA;
    private static final byte FILL_IV_BYTE = (byte) 0xBB;
    private static final byte FILL_PART_BYTE = (byte) 0xCC;

    @Test
    void constructor_whenValid_createsPackage() {
        // given
        byte[] aesKey = filledBytes(AES_KEY_LENGTH, FILL_KEY_BYTE);
        byte[] iv = filledBytes(IV_LENGTH, FILL_IV_BYTE);
        BatchFileSpec spec = makeSpec(1);
        List<byte[]> partBytes = List.of(filledBytes(8, FILL_PART_BYTE));

        // when
        PreparedBatchPackage pkg = new PreparedBatchPackage(spec, aesKey, iv, partBytes);

        // then
        assertEquals(spec, pkg.spec());
        assertArrayEquals(aesKey, pkg.aesKey());
        assertArrayEquals(iv, pkg.initVector());
        assertEquals(1, pkg.partBytes().size());
    }

    @Test
    void constructor_whenAesKeyWrongLength_throwsIllegalArgument() {
        byte[] tooShort = new byte[AES_KEY_LENGTH - 1];
        BatchFileSpec spec = makeSpec(1);
        byte[] iv = filledBytes(IV_LENGTH, FILL_IV_BYTE);
        List<byte[]> parts = List.of(new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> new PreparedBatchPackage(spec, tooShort, iv, parts));
    }

    @Test
    void constructor_whenIvWrongLength_throwsIllegalArgument() {
        byte[] tooShort = new byte[IV_LENGTH - 1];
        BatchFileSpec spec = makeSpec(1);
        byte[] aesKey = filledBytes(AES_KEY_LENGTH, FILL_KEY_BYTE);
        List<byte[]> parts = List.of(new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> new PreparedBatchPackage(spec, aesKey, tooShort, parts));
    }

    @Test
    void constructor_whenPartCountMismatch_throwsIllegalArgument() {
        BatchFileSpec spec = makeSpec(2);
        byte[] aesKey = filledBytes(AES_KEY_LENGTH, FILL_KEY_BYTE);
        byte[] iv = filledBytes(IV_LENGTH, FILL_IV_BYTE);
        List<byte[]> onePart = List.of(new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> new PreparedBatchPackage(spec, aesKey, iv, onePart));
    }

    @Test
    void aesKey_returnsDefensiveCopy() {
        byte[] aesKey = filledBytes(AES_KEY_LENGTH, FILL_KEY_BYTE);
        PreparedBatchPackage pkg = new PreparedBatchPackage(
                makeSpec(1), aesKey, filledBytes(IV_LENGTH, FILL_IV_BYTE),
                List.of(new byte[]{1}));

        byte[] returned = pkg.aesKey();
        assertNotSame(aesKey, returned);
        returned[0] = (byte) 0x00;
        assertEquals(FILL_KEY_BYTE, pkg.aesKey()[0]);
    }

    @Test
    void initVector_returnsDefensiveCopy() {
        byte[] iv = filledBytes(IV_LENGTH, FILL_IV_BYTE);
        PreparedBatchPackage pkg = new PreparedBatchPackage(
                makeSpec(1), filledBytes(AES_KEY_LENGTH, FILL_KEY_BYTE), iv,
                List.of(new byte[]{1}));

        byte[] returned = pkg.initVector();
        assertNotSame(iv, returned);
        returned[0] = (byte) 0x00;
        assertEquals(FILL_IV_BYTE, pkg.initVector()[0]);
    }

    private static byte[] filledBytes(int length, byte value) {
        byte[] arr = new byte[length];
        java.util.Arrays.fill(arr, value);
        return arr;
    }

    /** SHA-256 hash length — required by BatchFileSpec strict validation (Codex M1). */
    private static final int SHA_256_BYTES = 32;

    private static BatchFileSpec makeSpec(int partCount) {
        BatchFileSpec.Part[] parts = new BatchFileSpec.Part[partCount];
        for (int idx = 0; idx < partCount; idx++) {
            byte[] partHash = new byte[SHA_256_BYTES];
            partHash[0] = (byte) idx;
            parts[idx] = new BatchFileSpec.Part(idx + 1, PART_SIZE, partHash);
        }
        return new BatchFileSpec(PART_SIZE * partCount, new byte[SHA_256_BYTES], List.of(parts));
    }
}
