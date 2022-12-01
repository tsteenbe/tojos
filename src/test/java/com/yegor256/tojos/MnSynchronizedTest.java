/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2022 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.yegor256.tojos;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link MnSynchronized}.
 *
 * @since 0.3.0
 */
class MnSynchronizedTest {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            Logger.getLogger(MnSynchronizedTest.class.getName());

    /**
     * Number of threads.
     */
    static final int THREADS = 5;

    /**
     * The mono under test.
     */
    private Mono shared;

    /**
     * The executor.
     */
    private ExecutorService executor;

    private CountDownLatch latch;

    @BeforeEach
    final void setUp(@TempDir final Path temp) {
        this.shared = new MnSynchronized(new MnJson(temp.resolve("/bar/baz/a.json")));
        this.executor = Executors.newFixedThreadPool(MnSynchronizedTest.THREADS);
        this.latch = new CountDownLatch(1);
    }

    /**
     * Thread-safety test.
     * In this test, we check the number of changes in MnSynchronized mono.
     * It should be equal to the sum of the arithmetic progression over the number of threads.
     *
     * @throws InterruptedException When fails
     */
    @Test
    final void concurrentScenario() throws InterruptedException {
        final Collection<Map<String, String>> addition = rowsByThreads();
        for (int trds = 1; trds <= MnSynchronizedTest.THREADS; ++trds) {
            this.executor.submit(
                () -> {
                    this.latch.await();
                    final Collection<Map<String, String>> increased = this.shared.read();
                    increased.addAll(addition);
                    this.shared.write(increased);
                    return this.shared.read().size();
                }
            );
        }
        this.latch.countDown();
        assert this.executor.awaitTermination(30, TimeUnit.SECONDS);
        MatcherAssert.assertThat(
            this.shared.read().size(),
            Matchers.equalTo(25)
        );
    }

    /**
     * The rows to write.
     *
     * @return Collection of rows
     */
    private static Collection<Map<String, String>> rowsByThreads() {
        final Map<String, String> row = new HashMap<>(0);
        row.put(Tojos.KEY, String.valueOf(MnSynchronizedTest.THREADS));
        final Collection<Map<String, String>> res = new ArrayList<>(MnSynchronizedTest.THREADS);
        for (int idx = 0; idx < MnSynchronizedTest.THREADS; ++idx) {
            res.add(row);
        }
        return res;
    }
}
