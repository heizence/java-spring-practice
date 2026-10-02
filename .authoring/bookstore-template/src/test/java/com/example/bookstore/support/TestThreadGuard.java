package com.example.bookstore.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Fails a test whose methods run on another thread than the one that started its transaction. Registered by
 * {@link IntegrationTest}.
 *
 * <p>Spring binds a test's transaction to the thread that starts it, and MockMvc runs requests on the calling
 * thread. testkit's {@code junit-platform.properties} runs every test method on a thread of its own (thread mode
 * {@code SEPARATE_THREAD}), so the module's {@code build.gradle.kts} switches back to {@code SAME_THREAD}. If that
 * setting is missing, {@code @Transactional} tests would silently commit their changes; this guard turns the mistake
 * into a clear failure instead.
 */
public class TestThreadGuard implements BeforeEachCallback, InvocationInterceptor {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestThreadGuard.class);

    /** Runs on the thread where Spring starts the test's transaction. */
    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(Thread.class, Thread.currentThread());
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext,
                                          ExtensionContext extensionContext) throws Throwable {
        checkThread(extensionContext);
        invocation.proceed();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        checkThread(extensionContext);
        invocation.proceed();
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext,
                                         ExtensionContext extensionContext) throws Throwable {
        checkThread(extensionContext);
        invocation.proceed();
    }

    private static void checkThread(ExtensionContext context) {
        Thread testThread = context.getStore(NAMESPACE).get(Thread.class, Thread.class);
        if (testThread != null && testThread != Thread.currentThread()) {
            throw new IllegalStateException("This method runs on thread '%s' instead of '%s', outside the test's "
                    .formatted(Thread.currentThread().getName(), testThread.getName())
                    + "transaction. Add systemProperty(\"junit.jupiter.execution.timeout.thread.mode.default\", "
                    + "\"SAME_THREAD\") to tasks.withType<Test> in the module's build.gradle.kts.");
        }
    }
}
