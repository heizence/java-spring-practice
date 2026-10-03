package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Mutant: scheduling is always on; bookstore.scheduling.enabled=false changes nothing. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ScheduledJobsConfiguration {
}
