package org.apache.james.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

class RunnablesTest {

    @Test
    void shouldActuallyRunThings() {
        AtomicBoolean sideEffect = new AtomicBoolean(false);
        Runnables.runParallel(() -> sideEffect.set(true));
        assertThat(sideEffect).isTrue();
    }

    @Test
    void shouldActuallyRunInParallel() throws InterruptedException {
        int parallel = 20;
        CountDownLatch countDownLatch = new CountDownLatch(parallel);
        Runnable runnable = countDownLatch::countDown;
        Runnables.runParallel(Flux.range(0, 20).map(i -> runnable), parallel);
        assertThat(countDownLatch.await(2, TimeUnit.MINUTES)).isTrue();
    }
}