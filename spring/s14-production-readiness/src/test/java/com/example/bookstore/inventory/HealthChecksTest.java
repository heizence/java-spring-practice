package com.example.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The health endpoint and its groups, through {@code /actuator}, and the inventory indicator itself. The seed data
 * has 60 books, and books 59 and 60 are out of stock.
 */
@IntegrationTest
@DisplayName("S14-02 · Health checks and metrics")
class HealthChecksTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private InventoryHealthIndicator inventoryHealthIndicator;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("the inventory indicator is UP and counts the books in stock and out of stock")
        void inventoryIsUp() {
            Health health = inventoryHealthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("booksInStock", 58L, "booksOutOfStock", 2L));
        }

        @Test
        @DisplayName("the readiness group checks the readiness state and the inventory")
        void readinessGroup() {
            assertThat(mvc.get().uri("/actuator/health/readiness"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "status": "UP",
                              "components": {
                                "inventory": { "status": "UP" },
                                "readinessState": { "status": "UP" }
                              }
                            }""");
        }

        @Test
        @DisplayName("the liveness group checks only the liveness state: no dependencies")
        void livenessGroup() {
            assertThat(mvc.get().uri("/actuator/health/liveness"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "status": "UP",
                              "components": {
                                "livenessState": { "status": "UP" }
                              }
                            }""");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("health shows each check's status, but none of their details")
        void healthHidesDetails() {
            assertThat(mvc.get().uri("/actuator/health"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.components.inventory.status",
                            status -> assertThat(status).asString().isEqualTo("UP"))
                    .doesNotHavePath("$.components.inventory.details")
                    .doesNotHavePath("$.components.db.details")
                    .doesNotHavePath("$.components.diskSpace.details");
        }

        @ParameterizedTest(name = "/actuator/{0}")
        @ValueSource(strings = {"health", "info", "metrics"})
        @DisplayName("exposes health, info and metrics on the web")
        void exposesWhatOperationsNeed(String endpoint) {
            assertThat(mvc.get().uri("/actuator/" + endpoint)).hasStatusOk();
        }

        @ParameterizedTest(name = "/actuator/{0}")
        @ValueSource(strings = {"env", "beans", "configprops", "loggers", "heapdump", "threaddump", "mappings",
                "conditions", "shutdown"})
        @DisplayName("exposes no other endpoint: they reveal internals or change the application")
        void exposesNothingElse(String endpoint) {
            assertThat(mvc.get().uri("/actuator/" + endpoint)).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("the inventory indicator is DOWN, with the error, when the stock can't be read")
        void inventoryIsDownWhenTheDatabaseFails() {
            InventoryRepository failing = mock(InventoryRepository.class);
            when(failing.countByQuantityGreaterThan(0))
                    .thenThrow(new DataAccessResourceFailureException("Communications link failure"));
            when(failing.countByQuantity(0))
                    .thenThrow(new DataAccessResourceFailureException("Communications link failure"));

            Health health = new InventoryHealthIndicator(failing).health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("error");
        }
    }
}
