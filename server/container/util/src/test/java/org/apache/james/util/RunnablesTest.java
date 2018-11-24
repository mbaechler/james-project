package org.apache.james.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Stopwatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RunnablesTest {

    @Test
    void shouldActuallyRunThings() {
        AtomicBoolean sideEffect = new AtomicBoolean(true);
        Runnables.runParallel(() -> sideEffect.set(false));
        assertThat(sideEffect).isFalse();
    }

    @Test
    void shouldActuallyRunInParallel() {
        Runnable runnable = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        };
        Stopwatch stopwatch = Stopwatch.createStarted();
        int parallel = 20;
        Runnables.runParallel(Flux.range(0, 20).map(i -> runnable), parallel);
        assertThat(stopwatch.elapsed()).isLessThan(Duration.ofSeconds(2));
    }
}