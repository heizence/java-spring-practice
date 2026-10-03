package com.example.bookstore.book;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores and serves the cover images of books. */
@Service
@RequiredArgsConstructor
public class CoverService {

    private final BookRepository bookRepository;
    private final CoverStorage coverStorage;
    private final CoverProperties coverProperties;

    /**
     * Checks an uploaded cover and stores it as the book's cover, replacing the one it had.
     *
     * <h2>Rules, checked in this order</h2>
     * <ol>
     *   <li>The book exists, or {@link ErrorCode#BOOK_NOT_FOUND}: {@code Book 9999 was not found.}</li>
     *   <li>The file isn't empty, or {@link ErrorCode#INVALID_INPUT}: {@code The cover file is empty.}</li>
     *   <li>It has at most {@link CoverProperties#maxSize()} bytes, or {@link ErrorCode#COVER_TOO_LARGE}:
     *       {@code The cover has 1048577 bytes, but at most 1048576 are allowed.}</li>
     *   <li>Its content is a JPEG or PNG image ({@link ImageFormat#detect}), or
     *       {@link ErrorCode#UNSUPPORTED_MEDIA_TYPE}: {@code A cover must be a JPEG or PNG image.} What the client
     *       declares as the file's content type doesn't count: the bytes decide.</li>
     * </ol>
     * The stored cover's media type is the detected format's: a PNG sent as {@code application/octet-stream} is
     * stored as {@code image/png}.
     *
     * @throws BusinessException as listed above; nothing is stored then
     */
    public void store(long bookId, MultipartFile file) {
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(bookId));
        }
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "The cover file is empty.");
        }
        long maxBytes = coverProperties.maxSize().toBytes();
        // Check the size before reading the bytes: getSize() is known without loading the file into memory.
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.COVER_TOO_LARGE,
                    "The cover has %d bytes, but at most %d are allowed.".formatted(file.getSize(), maxBytes));
        }
        byte[] content = bytesOf(file);
        ImageFormat format = ImageFormat.detect(content).orElseThrow(() -> new BusinessException(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE, "A cover must be a JPEG or PNG image."));
        coverStorage.save(bookId, new Cover(content, format.mediaType()));
    }

    /**
     * The cover of a book.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if the book doesn't exist;
     *                           {@link ErrorCode#COVER_NOT_FOUND} if it has no cover
     */
    public Cover find(long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(bookId));
        }
        return coverStorage.findById(bookId).orElseThrow(() -> new BusinessException(ErrorCode.COVER_NOT_FOUND,
                "Book %d has no cover image.".formatted(bookId)));
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            // The upload broke off while the server read it; nothing the client can fix by changing the request.
            throw new UncheckedIOException("Could not read the uploaded cover", e);
        }
    }
}
