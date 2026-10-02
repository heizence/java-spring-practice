package com.example.bookstore.common;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;

/** Mutant: a @Bean method in a class without @Configuration, so component scanning never finds it. */
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
