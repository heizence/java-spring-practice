package com.example.bookstore.book;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The cover image of a book: {@code /api/books/{id}/cover}. */
@RestController
@RequestMapping("/api/books/{id}/cover")
@RequiredArgsConstructor
public class BookCoverController {

    private final CoverService coverService;

    /**
     * S04-10: {@code PUT /api/books/{id}/cover}: uploads a book's cover.
     *
     * <ul>
     *   <li>Accepts only {@code multipart/form-data} requests; any other content type gets
     *       {@code 415 Unsupported Media Type}.</li>
     *   <li>The part named {@code file} holds the image; without it, {@code 400 Bad Request}.</li>
     *   <li>{@link CoverService#store} checks and stores it, then the answer is {@code 204 No Content}.</li>
     * </ul>
     */
    public ResponseEntity<Void> uploadCover(long id, MultipartFile file) {
        // TODO: S04-10
        throw new UnsupportedOperationException("TODO: S04-10");
    }

    /** The cover image itself, with its media type as {@code Content-Type}. */
    @GetMapping
    public ResponseEntity<byte[]> getCover(@PathVariable long id) {
        Cover cover = coverService.find(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(cover.mediaType())).body(cover.content());
    }
}
