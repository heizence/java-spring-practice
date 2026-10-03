package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Reference tests for S13-01.
 *
 * <p>Three habits make these tests catch bugs:
 * <ul>
 *   <li>Different numbers for different things: order 201 of member 7. A test whose order and member were both 1
 *       couldn't see a {@code Location} header that uses the wrong one.</li>
 *   <li>Check everything a client relies on: the status, the headers, the JSON body, and the {@code code} of an
 *       error, not only its status.</li>
 *   <li>Check what reaches the service: exact arguments for valid requests (records compare by value, so a stub
 *       with the expected request is already a check), and no call at all for invalid ones.</li>
 * </ul>
 */
@WebMvcTest(OrderController.class)
@DisplayName("S13-01 · Write tests: a controller slice")
class OrderControllerTest {

    private static final long MEMBER_ID = 7L;
    private static final Instant ORDERED_AT = Instant.parse("2026-10-15T09:30:00Z");
    private static final OrderResponse ORDER = new OrderResponse(201L, MEMBER_ID, OrderStatus.PENDING, 72_000,
            ORDERED_AT, List.of(new OrderLineResponse(1L, "Effective Java", 2, 36_000, 72_000)));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("POST /api/orders answers 201 Created, with the new order's path in Location and the order as JSON")
        void placesAnOrder() {
            when(orderService.placeOrder(orderOf(2))).thenReturn(ORDER);

            assertThat(postOrder("""
                    { "memberId": 7, "lines": [ { "bookId": 1, "quantity": 2 } ] }"""))
                    .hasStatus(HttpStatus.CREATED)
                    .hasHeader(HttpHeaders.LOCATION, "/api/orders/201")
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "id": 201,
                              "memberId": 7,
                              "status": "PENDING",
                              "totalAmount": 72000,
                              "orderedAt": "2026-10-15T09:30:00Z",
                              "lines": [
                                { "bookId": 1, "title": "Effective Java", "quantity": 2, "unitPrice": 36000,
                                  "amount": 72000 }
                              ]
                            }""");
        }

        @Test
        @DisplayName("GET /api/orders/{id} returns the order as JSON")
        void returnsAnOrder() {
            when(orderService.findOrder(201L)).thenReturn(ORDER);

            assertThat(mvc.get().uri("/api/orders/201"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.id", id -> assertThat(id).asNumber().isEqualTo(201))
                    .hasPathSatisfying("$.lines[0].title", title -> assertThat(title).asString()
                            .isEqualTo("Effective Java"));
        }

        @Test
        @DisplayName("GET /api/members/{memberId}/orders passes the member, the status and the page to the service, "
                + "and returns the page with its metadata")
        void returnsAPageOfOrders() {
            var pageRequest = PageRequest.of(2, 5);
            var summary = new OrderSummaryResponse(201L, OrderStatus.PAID, 72_000, ORDERED_AT);
            when(orderService.findMemberOrders(MEMBER_ID, OrderStatus.PAID, pageRequest))
                    .thenReturn(new PageImpl<>(List.of(summary), pageRequest, 11));

            assertThat(mvc.get().uri("/api/members/7/orders?status=PAID&page=2&size=5"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "content": [
                                { "id": 201, "status": "PAID", "totalAmount": 72000,
                                  "orderedAt": "2026-10-15T09:30:00Z" }
                              ],
                              "page": { "size": 5, "number": 2, "totalElements": 11, "totalPages": 3 }
                            }""");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("without parameters, the list asks for page 0 with 20 orders, in every status")
        void defaultPage() {
            var pageRequest = PageRequest.of(0, 20);
            when(orderService.findMemberOrders(MEMBER_ID, null, pageRequest)).thenReturn(Page.empty(pageRequest));

            assertThat(mvc.get().uri("/api/members/7/orders")).hasStatusOk();

            verify(orderService).findMemberOrders(MEMBER_ID, null, pageRequest);
        }

        @Test
        @DisplayName("accepts the largest page size, 100")
        void largestPageSize() {
            var pageRequest = PageRequest.of(0, 100);
            when(orderService.findMemberOrders(MEMBER_ID, null, pageRequest)).thenReturn(Page.empty(pageRequest));

            assertThat(mvc.get().uri("/api/members/7/orders?size=100")).hasStatusOk();
        }

        @Test
        @DisplayName("accepts a line with 10 copies, the most one line may have")
        void largestQuantity() {
            when(orderService.placeOrder(orderOf(10))).thenReturn(ORDER);

            assertThat(postOrder("""
                    { "memberId": 7, "lines": [ { "bookId": 1, "quantity": 10 } ] }"""))
                    .hasStatus(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a line with 0 copies: 400 INVALID_INPUT naming the field, and the service isn't called")
        void rejectsAnInvalidLine() {
            assertThat(postOrder("""
                    { "memberId": 7, "lines": [ { "bookId": 1, "quantity": 0 } ] }"""))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "code": "INVALID_INPUT",
                              "errors": [ { "field": "lines[0].quantity",
                                            "message": "must be greater than or equal to 1" } ]
                            }""");
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("rejects an order without a member and without lines, listing both, and the service isn't called")
        void rejectsAnIncompleteOrder() {
            assertThat(postOrder("""
                    { "memberId": null, "lines": [] }"""))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "code": "INVALID_INPUT",
                              "errors": [ { "field": "lines", "message": "must not be empty" },
                                          { "field": "memberId", "message": "must not be null" } ]
                            }""");
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("rejects a page size above 100 with 400 INVALID_INPUT, and the service isn't called")
        void rejectsATooLargePage() {
            assertThat(mvc.get().uri("/api/members/7/orders?size=101"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "code": "INVALID_INPUT",
                              "errors": [ { "field": "size", "message": "must be less than or equal to 100" } ]
                            }""");
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("rejects a status that doesn't exist with 400 INVALID_INPUT, and the service isn't called")
        void rejectsAnUnknownStatus() {
            assertThat(mvc.get().uri("/api/members/7/orders?status=LOST"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("answers 404 with code ORDER_NOT_FOUND when the service finds no order")
        void unknownOrder() {
            when(orderService.findOrder(999L))
                    .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order 999 was not found."));

            assertThat(mvc.get().uri("/api/orders/999"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Order 999 was not found.",
                              "instance": "/api/orders/999",
                              "code": "ORDER_NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("answers 409 with code OUT_OF_STOCK when the service reports too few copies")
        void outOfStock() {
            when(orderService.placeOrder(any())).thenThrow(new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "Only 3 copies of book 1 are left, but 5 were requested."));

            assertThat(postOrder("""
                    { "memberId": 7, "lines": [ { "bookId": 1, "quantity": 5 } ] }"""))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().extractingPath("$.code").isEqualTo("OUT_OF_STOCK");
        }
    }

    /** The request that {@code {"memberId": 7, "lines": [{"bookId": 1, "quantity": …}]}} becomes. */
    private static PlaceOrderRequest orderOf(int quantity) {
        return new PlaceOrderRequest(MEMBER_ID, List.of(new PlaceOrderRequest.Line(1L, quantity)));
    }

    private MvcTestResult postOrder(String json) {
        return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
