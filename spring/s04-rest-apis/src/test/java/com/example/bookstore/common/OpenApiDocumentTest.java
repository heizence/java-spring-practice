package com.example.bookstore.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The OpenAPI document that springdoc-openapi generates at {@code /v3/api-docs}. */
@IntegrationTest
@DisplayName("S04-09 · OpenAPI documentation")
class OpenApiDocumentTest {

    private static final String PROBLEM_DETAIL = "#/components/schemas/ProblemDetail";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("the document names the API: Bookstore API, version 1")
        void info() {
            assertThat(apiDocs())
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.info.title", title -> assertThat(title).isEqualTo("Bookstore API"))
                    .hasPathSatisfying("$.info.version", version -> assertThat(version).isEqualTo("1"));
        }

        @Test
        @DisplayName("the book operations are grouped under the tag Books, each with its summary")
        void tagsAndSummaries() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/books/{id}'].get.tags",
                            tags -> assertThat(tags).asArray().containsExactly("Books"))
                    .hasPathSatisfying("$.paths['/api/books'].post.tags",
                            tags -> assertThat(tags).asArray().containsExactly("Books"))
                    .hasPathSatisfying("$.paths['/api/books/{id}'].get.summary",
                            summary -> assertThat(summary).isEqualTo("Get a book"))
                    .hasPathSatisfying("$.paths['/api/books'].post.summary",
                            summary -> assertThat(summary).isEqualTo("Add a book"))
                    .hasPathSatisfying("$.paths['/api/books/{id}'].delete.summary",
                            summary -> assertThat(summary).isEqualTo("Remove a book"));
        }

        @Test
        @DisplayName("the Swagger UI shows the document at /swagger-ui/index.html")
        void swaggerUi() {
            assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Responses")
    class Responses {

        @Test
        @DisplayName("GET /api/books/{id} documents its 404 as a problem detail")
        void getBookNotFound() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/books/{id}'].get.responses['404']"
                            + ".content['application/problem+json'].schema['$ref']",
                            schema -> assertThat(schema).isEqualTo(PROBLEM_DETAIL));
        }

        @Test
        @DisplayName("POST /api/books documents 201 Created instead of 200, and its 400 and 409 as problem details")
        void createBook() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/books'].post.responses", responses -> assertThat(responses)
                            .asMap().containsOnlyKeys("201", "400", "409"))
                    .hasPathSatisfying("$.paths['/api/books'].post.responses['201']"
                            + ".content['application/json'].schema['$ref']",
                            schema -> assertThat(schema).isEqualTo("#/components/schemas/BookResponse"))
                    .hasPathSatisfying("$.paths['/api/books'].post.responses['400']"
                            + ".content['application/problem+json'].schema['$ref']",
                            schema -> assertThat(schema).isEqualTo(PROBLEM_DETAIL))
                    .hasPathSatisfying("$.paths['/api/books'].post.responses['409']"
                            + ".content['application/problem+json'].schema['$ref']",
                            schema -> assertThat(schema).isEqualTo(PROBLEM_DETAIL));
        }

        @Test
        @DisplayName("DELETE /api/books/{id} documents 204 No Content instead of 200")
        void deleteBook() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/books/{id}'].delete.responses",
                            responses -> assertThat(responses).asMap().containsOnlyKeys("204"));
        }

        @Test
        @DisplayName("successful responses are documented as application/json, not as any media type")
        void jsonResponses() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/books'].get.responses['200'].content",
                            content -> assertThat(content).asMap().containsOnlyKeys("application/json"));
        }
    }

    @Nested
    @DisplayName("Requests")
    class Requests {

        @Test
        @DisplayName("GET /api/members/me documents the X-Member-Id header, not a member parameter")
        void currentMember() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths['/api/members/me'].get.parameters[*].name",
                            names -> assertThat(names).asArray().containsExactly("X-Member-Id"))
                    .hasPathSatisfying("$.paths['/api/members/me'].get.parameters[0]",
                            header -> assertThat(header).asMap().contains(Map.entry("in", "header"),
                                    Map.entry("required", true)));
        }

        @Test
        @DisplayName("the request schemas show the validation rules of the records")
        void validationRules() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.components.schemas.CreateBookRequest.required",
                            required -> assertThat(required).asArray().containsExactlyInAnyOrder(
                                    "isbn", "title", "author", "categoryId", "price", "publishedDate"))
                    .hasPathSatisfying("$.components.schemas.CreateBookRequest.properties.isbn.pattern",
                            pattern -> assertThat(pattern).isEqualTo("\\d{13}"))
                    .hasPathSatisfying("$.components.schemas.CreateBookRequest.properties.title.maxLength",
                            maxLength -> assertThat(maxLength).isEqualTo(200));
        }
    }

    @Nested
    @DisplayName("Private parts")
    class PrivateParts {

        @Test
        @DisplayName("the admin API isn't in the document")
        void noAdminApi() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.paths", paths -> assertThat(paths).asMap().isNotEmpty()
                            .allSatisfy((path, operations) -> assertThat(path).doesNotStartWith("/api/admin")));
        }

        @Test
        @DisplayName("the version of a book is no property of its schema: it travels as the ETag header")
        void noVersionProperty() {
            assertThat(apiDocs())
                    .bodyJson()
                    .hasPathSatisfying("$.components.schemas.BookResponse.properties",
                            properties -> assertThat(properties).asMap().doesNotContainKey("version"));
        }
    }

    private MvcTestResult apiDocs() {
        return mvc.get().uri("/v3/api-docs").exchange();
    }
}
