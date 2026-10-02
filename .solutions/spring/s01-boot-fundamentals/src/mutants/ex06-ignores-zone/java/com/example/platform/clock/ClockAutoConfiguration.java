package com.example.platform.clock;

import java.time.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Mutant: always uses UTC, whatever platform.clock.zone says. */
@AutoConfiguration
@ConditionalOnBooleanProperty(name = "platform.clock.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ClockProperties.class)
public class ClockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock(ClockProperties properties) {
        return Clock.tick(Clock.systemUTC(), properties.precision());
    }
}
