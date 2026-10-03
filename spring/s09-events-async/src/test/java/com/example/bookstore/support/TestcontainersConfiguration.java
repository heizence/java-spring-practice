package com.example.bookstore.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs MySQL 9.7 in a Docker container for tests and connects Spring Boot to it through
 * {@link ServiceConnection @ServiceConnection}: no datasource properties needed.
 *
 * <p><b>One container per test JVM.</b> Every application context that imports this class gets the same container
 * ({@link #MYSQL} is static), so a module's tests start MySQL once, however many contexts they create. Spring closes
 * a context's beans when it closes the context (for example when its context cache evicts it) and would stop the
 * container with it, while other cached contexts still use it. So the container ignores {@code close()}:
 * Testcontainers removes it when the JVM exits.
 *
 * <p><b>Small memory settings,</b> because many test JVMs may run at the same time in one Docker VM. Our data needs
 * a few megabytes, so MySQL gets small buffers and no performance schema: one container uses about 140 MB instead of
 * 460 MB.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final MySQLContainer MYSQL = new SharedMySQLContainer();

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return MYSQL;
    }

    /** A MySQL container that belongs to the test JVM, not to one application context. */
    private static final class SharedMySQLContainer extends MySQLContainer {

        SharedMySQLContainer() {
            super(DockerImageName.parse("mysql:9.7"));
            withDatabaseName("bookstore");
            withCommand(
                    "--innodb-buffer-pool-size=32M",  // default 128M
                    "--innodb-log-buffer-size=4M",    // default 64M
                    "--innodb-redo-log-capacity=8M",  // default 100M
                    "--performance-schema=OFF",       // its statistics cost the most memory; tests don't read them
                    "--skip-log-bin");                // no binary log: nothing replicates from this server
        }

        /**
         * Called by Spring when it closes an application context. Other contexts may still use this container, so
         * keep it running; Testcontainers removes it when the JVM exits.
         */
        @Override
        public void close() {
        }
    }
}
