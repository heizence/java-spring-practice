package com.example.bookstore.book;

import java.util.Objects;

/**
 * The cover image of a book.
 *
 * <p>Arrays are mutable, so the record copies its bytes on the way in and on the way out: nobody can change a stored
 * cover through an array they hold.
 *
 * @param content   the image file's bytes
 * @param mediaType the image's media type, such as {@code image/png}
 */
public record Cover(byte[] content, String mediaType) {

    public Cover {
        content = Objects.requireNonNull(content, "content").clone();
        Objects.requireNonNull(mediaType, "mediaType");
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
