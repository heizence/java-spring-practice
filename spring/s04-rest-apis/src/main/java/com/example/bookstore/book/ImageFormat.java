package com.example.bookstore.book;

import java.util.Arrays;
import java.util.Optional;

/**
 * The image formats that a cover may have, recognized by the first bytes of the file: its <em>magic number</em>.
 *
 * <p>Check the bytes, not what the client says. The {@code Content-Type} of an uploaded part and the file name are
 * chosen by the client, and anyone can send a script named {@code cover.png} with {@code Content-Type: image/png}.
 */
public enum ImageFormat {

    /** Starts with the bytes {@code FF D8 FF}. */
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),

    /** Starts with the bytes {@code 89 50 4E 47 0D 0A 1A 0A}: one byte, the letters PNG, and four control bytes. */
    PNG("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String mediaType;
    private final byte[] magicNumber;

    ImageFormat(String mediaType, byte[] magicNumber) {
        this.mediaType = mediaType;
        this.magicNumber = magicNumber;
    }

    /** The media type of this format, such as {@code image/png}. */
    public String mediaType() {
        return mediaType;
    }

    /** The format whose magic number the content starts with, or empty if it is neither JPEG nor PNG. */
    public static Optional<ImageFormat> detect(byte[] content) {
        return Arrays.stream(values())
                .filter(format -> content.length >= format.magicNumber.length
                        && Arrays.equals(content, 0, format.magicNumber.length,
                        format.magicNumber, 0, format.magicNumber.length))
                .findFirst();
    }
}
