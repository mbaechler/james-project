package org.apache.james.backends.cassandra;

import com.google.common.primitives.Ints;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.WaitStrategy;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CassandraWaitStrategy implements WaitStrategy {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
    private Duration timeout = DEFAULT_TIMEOUT;

    public CassandraWaitStrategy() {
        this(DEFAULT_TIMEOUT);
    }

    public CassandraWaitStrategy(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public void waitUntilReady(@SuppressWarnings("rawtypes") GenericContainer container) {
        Unreliables.retryUntilTrue(Ints.checkedCast(timeout.getSeconds()), TimeUnit.SECONDS, () -> {
                try {
                    Container.ExecResult result = container.execInContainer("cqlsh", "-e", "show host");
                    return result.getStdout().contains("Connected to Test Cluster");
                } catch (IOException|InterruptedException e) {
                    return false;
                }
            }
        );
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        return new CassandraWaitStrategy(startupTimeout);
    }
}
