package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code PUT} and {@code GET /api/books/{id}/cover}: a multipart upload, checked and stored in memory. */
@IntegrationTest
@DisplayName("S04-10 · File upload and CORS")
class BookCoverApiTest {

    /** The limit in application.yml: 1MB, which Spring reads as 1,048,576 bytes. */
    private static final int MAX_BYTES = 1024 * 1024;

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("PUT with a PNG answers 204, and GET answers the image with Content-Type image/png")
        void uploadsAPng() {
            byte[] png = png(100);

            assertThat(upload(1, "cover.png", "image/png", png)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(mvc.get().uri("/api/books/1/cover"))
                    .hasStatusOk()
                    .hasContentType(MediaType.IMAGE_PNG)
                    .body().isEqualTo(png);
        }

        @Test
        @DisplayName("a JPEG works too, with Content-Type image/jpeg")
        void uploadsAJpeg() {
            byte[] jpeg = jpeg(100);

            assertThat(upload(1, "cover.jpg", "image/jpeg", jpeg)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(mvc.get().uri("/api/books/1/cover"))
                    .hasContentType(MediaType.IMAGE_JPEG)
                    .body().isEqualTo(jpeg);
        }

        @Test
        @DisplayName("a second upload replaces the first cover")
        void replacesTheCover() {
            upload(1, "old.png", "image/png", png(10));
            byte[] newCover = jpeg(20);

            upload(1, "new.jpg", "image/jpeg", newCover);

            assertThat(mvc.get().uri("/api/books/1/cover")).body().isEqualTo(newCover);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a cover of exactly 1 MB is accepted")
        void largestCover() {
            assertThat(upload(1, "cover.png", "image/png", png(MAX_BYTES))).hasStatus(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("the bytes decide the type: a PNG sent as application/octet-stream is stored as image/png")
        void bytesDecideTheType() {
            upload(1, "cover", "application/octet-stream", png(100));

            assertThat(mvc.get().uri("/api/books/1/cover")).hasContentType(MediaType.IMAGE_PNG);
        }

        @Test
        @DisplayName("each book has its own cover")
        void coverPerBook() {
            upload(1, "cover.png", "image/png", png(10));

            assertThat(mvc.get().uri("/api/books/2/cover"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("COVER_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a cover of 1 MB and 1 byte: 413 with code COVER_TOO_LARGE, and nothing is stored")
        void coverTooLarge() {
            assertThat(upload(1, "cover.png", "image/png", png(MAX_BYTES + 1)))
                    .hasStatus(HttpStatus.CONTENT_TOO_LARGE)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Content Too Large",
                              "status": 413,
                              "detail": "The cover has 1048577 bytes, but at most 1048576 are allowed.",
                              "instance": "/api/books/1/cover",
                              "code": "COVER_TOO_LARGE"
                            }""");
            assertThat(mvc.get().uri("/api/books/1/cover")).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("a GIF that claims to be a PNG: 415 with code UNSUPPORTED_MEDIA_TYPE, and nothing is stored")
        void notJpegOrPng() {
            byte[] gif = "GIF89a, a small animation".getBytes(StandardCharsets.US_ASCII);

            assertThat(upload(1, "cover.png", "image/png", gif))
                    .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unsupported Media Type",
                              "status": 415,
                              "detail": "A cover must be a JPEG or PNG image.",
                              "instance": "/api/books/1/cover",
                              "code": "UNSUPPORTED_MEDIA_TYPE"
                            }""");
            assertThat(mvc.get().uri("/api/books/1/cover")).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("an empty file: 400 with code INVALID_INPUT")
        void emptyFile() {
            assertThat(upload(1, "cover.png", "image/png", new byte[0]))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).isEqualTo("INVALID_INPUT"))
                    .hasPathSatisfying("$.detail", detail -> assertThat(detail).isEqualTo("The cover file is empty."));
        }

        @Test
        @DisplayName("a request without a part named file: 400 with code INVALID_INPUT")
        void noFilePart() {
            var wrongPart = new MockMultipartFile("image", "cover.png", "image/png", png(10));

            assertThat(mvc.put().uri("/api/books/1/cover").multipart().file(wrongPart))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
        }

        @Test
        @DisplayName("a request that isn't multipart/form-data: 415")
        void notMultipart() {
            assertThat(mvc.put().uri("/api/books/1/cover").contentType(MediaType.IMAGE_PNG).content(png(10)))
                    .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("a cover for a book that doesn't exist: 404 with code BOOK_NOT_FOUND")
        void unknownBook() {
            assertThat(upload(9999, "cover.png", "image/png", png(10)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("BOOK_NOT_FOUND");
        }
    }

    private MvcTestResult upload(long bookId, String fileName, String contentType, byte[] content) {
        var file = new MockMultipartFile("file", fileName, contentType, content);
        return mvc.put().uri("/api/books/{id}/cover", bookId).multipart().file(file).exchange();
    }

    /** A file of this size that starts like a PNG image: the 8-byte PNG signature, then zeros. */
    private static byte[] png(int size) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        byte[] content = Arrays.copyOf(signature, size);
        content[size - 1] = 42; // not only zeros, so a wrong copy shows up
        return content;
    }

    /** A file of this size that starts like a JPEG image: FF D8 FF, then zeros. */
    private static byte[] jpeg(int size) {
        byte[] content = Arrays.copyOf(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, size);
        content[size - 1] = 7;
        return content;
    }
}
