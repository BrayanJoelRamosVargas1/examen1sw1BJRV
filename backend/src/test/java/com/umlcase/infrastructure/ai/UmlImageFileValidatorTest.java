package com.umlcase.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UmlImageFileValidatorTest {
    private final UmlImageFileValidator validator = new UmlImageFileValidator();

    @Test
    void acceptsPngJpegAndWebpSignatures() {
        assertDoesNotThrow(() -> validator.validate(png(), "image/png"));
        assertDoesNotThrow(() -> validator.validate(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "image/jpeg"));
        assertDoesNotThrow(() -> validator.validate(new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}, "image/webp"));
    }

    @Test
    void rejectsEmptyOversizedAndUnsupportedFiles() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new byte[0], "image/png"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new byte[UmlImageFileValidator.MAX_IMAGE_BYTES + 1], "image/png"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("<svg/>".getBytes(), "image/svg+xml"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("not-png".getBytes(), "image/png"));
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }
}
