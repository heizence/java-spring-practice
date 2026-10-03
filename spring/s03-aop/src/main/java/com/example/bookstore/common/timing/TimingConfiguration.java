package com.example.bookstore.common.timing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the {@link Ticker} that {@link TimingAspect} measures durations with. Tests replace it. */
@Configuration(proxyBeanMethods = false)
public class TimingConfiguration {

    @Bean
    public Ticker ticker() {
        return Ticker.system();
    }
}
