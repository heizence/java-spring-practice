package com.example.bookstore.shipping;

import static com.example.bookstore.support.ContextAssertions.assertStartupFailed;
import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tests how the shipping rules get from the configuration into {@link ShippingProperties}. Each test starts a small
 * application context the way {@code SpringApplication} starts the application, and holds what component scanning
 * finds in the shipping package.
 */
@DisplayName("S02-01 · Type-safe configuration properties")
class ShippingPropertiesTest {

    /** Scans {@code com.example.bookstore.shipping} the way {@code @SpringBootApplication} scans the application. */
    @ComponentScan(basePackageClasses = ShippingConfiguration.class)
    static class ShippingPackage {
    }

    private final ApplicationContextRunner runner = startWith();

    /**
     * A runner that starts like the application on a server: the environment variables (such as
     * {@code BOOKSTORE_SHIPPING_FEE=4000}) are there first, then {@link ConfigDataApplicationContextInitializer} reads
     * {@code application.yml}, as {@code SpringApplication} does.
     */
    private static ApplicationContextRunner startWith(String... environmentVariables) {
        return new ApplicationContextRunner()
                .withInitializer(context -> TestPropertyValues.of(environmentVariables)
                        .applyTo(context.getEnvironment(), TestPropertyValues.Type.SYSTEM_ENVIRONMENT))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(ShippingPackage.class);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("binds the shipping rules from application.yml")
        void bindsApplicationYml() {
            runner.run(context -> assertThat(singleBean(context, ShippingProperties.class)).isEqualTo(
                    new ShippingProperties(20_000, 3_000, Duration.ofHours(14),
                            Set.of(Region.SEOUL, Region.INCHEON, Region.GYEONGGI, Region.BUSAN))));
        }

        @Test
        @DisplayName("ShippingFeeCalculator is a bean that gets the properties injected")
        void calculatorGetsTheProperties() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.feeFor(1_000, Region.SEOUL)).isEqualTo(3_000);
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("an environment variable overrides application.yml: BOOKSTORE_SHIPPING_FEE=4000")
        void environmentVariableOverridesTheFile() {
            startWith("BOOKSTORE_SHIPPING_FEE=4000")
                    .run(context -> assertThat(singleBean(context, ShippingProperties.class).fee()).isEqualTo(4_000));
        }

        @Test
        @DisplayName("an environment variable sets a list as comma-separated values")
        void environmentVariableSetsAList() {
            startWith("BOOKSTORE_SHIPPING_SUPPORTEDREGIONS=busan,jeju")
                    .run(context -> assertThat(singleBean(context, ShippingProperties.class).supportedRegions())
                            .containsExactlyInAnyOrder(Region.BUSAN, Region.JEJU));
        }

        @ParameterizedTest(name = "{0}", quoteTextArguments = false)
        @ValueSource(strings = {"bookstore.shipping.free-threshold", "bookstore.shipping.freeThreshold",
                "bookstore.shipping.free_threshold"})
        @DisplayName("relaxed binding: every spelling of a property name reaches the same component")
        void relaxedBinding(String propertyName) {
            runner.withPropertyValues(propertyName + "=50000")
                    .run(context -> assertThat(singleBean(context, ShippingProperties.class).freeThreshold())
                            .isEqualTo(50_000));
        }

        @Test
        @DisplayName("region names bind in any letter case")
        void regionsInAnyCase() {
            runner.withPropertyValues("bookstore.shipping.supported-regions=Jeju,BUSAN,seoul")
                    .run(context -> assertThat(singleBean(context, ShippingProperties.class).supportedRegions())
                            .containsExactlyInAnyOrder(Region.JEJU, Region.BUSAN, Region.SEOUL));
        }

        @Test
        @DisplayName("a duration takes a unit: 90m is an hour and a half")
        void durationUnits() {
            runner.withPropertyValues("bookstore.shipping.cutoff-time=90m")
                    .run(context -> assertThat(singleBean(context, ShippingProperties.class).cutoffTime())
                            .isEqualTo(Duration.ofMinutes(90)));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "fee {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("a fee of 0 or less stops the application, with an error that names the property")
        void invalidFee(int fee) {
            runner.withPropertyValues("bookstore.shipping.fee=" + fee)
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("bookstore.shipping")
                            .hasMessageContaining("'fee'")
                            .hasMessageContaining("must be greater than 0"));
        }

        @Test
        @DisplayName("a negative free-shipping threshold stops the application")
        void negativeThreshold() {
            runner.withPropertyValues("bookstore.shipping.free-threshold=-1")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("freeThreshold")
                            .hasMessageContaining("must be greater than or equal to 0"));
        }

        @Test
        @DisplayName("a missing cutoff time stops the application")
        void missingCutoffTime() {
            runner.withPropertyValues("bookstore.shipping.cutoff-time=")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("cutoffTime")
                            .hasMessageContaining("must not be null"));
        }

        @Test
        @DisplayName("an empty list of regions stops the application")
        void noRegions() {
            runner.withPropertyValues("bookstore.shipping.supported-regions=")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("supportedRegions")
                            .hasMessageContaining("must not be empty"));
        }

        @Test
        @DisplayName("a region that doesn't exist stops the application, with an error that names the property")
        void unknownRegion() {
            runner.withPropertyValues("bookstore.shipping.supported-regions=seoul,mars")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .hasMessageContaining("bookstore.shipping"));
        }
    }
}
