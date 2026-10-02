package com.example.platform.clock;

import java.time.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Mutant: no condition on platform.clock.enabled, so the clock can't be switched off. */
@AutoConfiguration
@EnableConfigurationProperties(ClockProperties.class)
public class ClockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock(ClockProperties properties) {
        return Clock.tick(Clock.system(properties.zone()), properties.precision());
    }
}
