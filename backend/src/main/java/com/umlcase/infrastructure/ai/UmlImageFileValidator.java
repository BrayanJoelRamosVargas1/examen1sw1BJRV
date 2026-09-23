package com.umlcase.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class UmlImageFileValidator {
    public static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    public void validate(byte[] image, String mimeType) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía");
        }
        if (image.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el máximo de 8 MB");
        }
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Formato de imagen no permitido");
        }
        if (!hasValidSignature(image, mimeType.toLowerCase())) {
            throw new IllegalArgumentException("El contenido no coincide con el MIME declarado");
        }
    }

    private boolean hasValidSignature(byte[] image, String mimeType) {
        return switch (mimeType) {
            case "image/png" -> startsWith(image, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/jpeg" -> startsWith(image, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/webp" -> image.length >= 12
                && Arrays.equals(Arrays.copyOfRange(image, 0, 4), new byte[]{0x52, 0x49, 0x46, 0x46})
                && Arrays.equals(Arrays.copyOfRange(image, 8, 12), new byte[]{0x57, 0x45, 0x42, 0x50});
            default -> false;
        };
    }

    private boolean startsWith(byte[] image, byte[] signature) {
        return image.length >= signature.length && Arrays.equals(Arrays.copyOf(image, signature.length), signature);
    }
}
