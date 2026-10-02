package com.example.bookstore.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import com.github.dockerjava.api.model.Container;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Two different application contexts (a full {@link IntegrationTest} and a JPA slice) run against the same MySQL
 * container, this test JVM starts no other one, and closing a context doesn't stop it.
 */
@DisplayName("Template · One MySQL container per test JVM")
class SharedContainerTest {

    private static final Set<ApplicationContext> CONTEXTS = ConcurrentHashMap.newKeySet();

    @Nested
    @IntegrationTest
    @DisplayName("A full application context")
    class FullApplication {

        @Autowired
        private ApplicationContext context;

        @Autowired
        private DataSource dataSource;

        @Test
        @DisplayName("connects to the shared container")
        void connectsToTheSharedContainer() throws SQLException {
            assertConnectsToTheSharedContainer(context, dataSource);
        }
    }

    @Nested
    @DataJpaTest(showSql = false)
    @Import(TestcontainersConfiguration.class)
    @DisplayName("A JPA slice context (@DataJpaTest)")
    class JpaSlice {

        @Autowired
        private ApplicationContext context;

        @Autowired
        private DataSource dataSource;

        @Test
        @DisplayName("connects to the shared container")
        void connectsToTheSharedContainer() throws SQLException {
            assertConnectsToTheSharedContainer(context, dataSource);
        }
    }

    @Test
    @DisplayName("closing an application context leaves the container running for the others")
    void survivesAClosedContext() {
        var context = new AnnotationConfigApplicationContext();
        new TestcontainersLifecycleApplicationContextInitializer().initialize(context); // as Spring Boot tests do
        context.register(TestcontainersConfiguration.class);
        context.refresh();

        context.close();

        assertThat(TestcontainersConfiguration.MYSQL.isRunning()).isTrue();
    }

    @AfterAll
    static void twoContextsShareOneContainer() {
        assumeThat(CONTEXTS).as("both nested test classes ran").hasSize(2);

        List<Container> mySqlContainers = DockerClientFactory.instance().client().listContainersCmd()
                .withLabelFilter(Map.of(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL,
                        DockerClientFactory.SESSION_ID))
                .exec().stream()
                .filter(container -> container.getImage().startsWith("mysql"))
                .toList();

        assertThat(mySqlContainers).as("MySQL containers started by this JVM").hasSize(1);
    }

    private static void assertConnectsToTheSharedContainer(ApplicationContext context, DataSource dataSource)
            throws SQLException {
        CONTEXTS.add(context);

        assertThat(context.getBean(MySQLContainer.class)).isSameAs(TestcontainersConfiguration.MYSQL);
        assertThat(JdbcClient.create(dataSource).sql("SELECT @@server_uuid").query(String.class).single())
                .isEqualTo(serverUuidOfTheSharedContainer());
    }

    /** Asks the container directly, without Spring, which MySQL server it runs. */
    private static String serverUuidOfTheSharedContainer() throws SQLException {
        try (Connection connection = TestcontainersConfiguration.MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT @@server_uuid")) {
            result.next();
            return result.getString(1);
        }
    }
}
