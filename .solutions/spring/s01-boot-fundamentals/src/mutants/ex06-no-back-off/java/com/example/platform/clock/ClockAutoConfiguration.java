package com.example.platform.clock;

import java.time.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Mutant: no @ConditionalOnMissingBean, so an application with a Clock of its own ends up with two. */
@AutoConfiguration
@ConditionalOnBooleanProperty(name = "platform.clock.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ClockProperties.class)
public class ClockAutoConfiguration {

    @Bean
    public Clock clock(ClockProperties properties) {
        return Clock.tick(Clock.system(properties.zone()), properties.precision());
    }
}
