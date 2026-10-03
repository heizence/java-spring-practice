package com.example.bookstore.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The general part of the API's OpenAPI document, which springdoc-openapi publishes at {@code /v3/api-docs} and
 * shows at {@code /swagger-ui.html}. springdoc builds the rest from the controllers, their annotations and the
 * request and response records.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI bookstoreOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bookstore API")
                .version("1")
                .description("The catalog of the online bookstore. Errors are RFC 9457 problem details with a "
                        + "machine-readable code."));
    }
}
