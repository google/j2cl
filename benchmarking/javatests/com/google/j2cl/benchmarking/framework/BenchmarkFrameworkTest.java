/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarking.framework;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** IntegrationTest for the Benchmark framework running in JavaScript. */
@RunWith(JUnit4.class)
public class BenchmarkFrameworkTest {
  @Test
  public void test() {
    MockClock clock = new MockClock(50_000_000); // Advance 50ms each time
    MockBenchmark benchmark = new MockBenchmark(clock);
    // 30 iterations with 10 warmup iterations of 1s each.
    BenchmarkResult benchmarkResult = BenchmarkExecutor.execute(benchmark, clock, 30, 10, 1000);
    // 30 iterations of 1 sec
    assertEquals(30000, clock.now() / 1_000_000);
    assertEquals(0.02, benchmarkResult.getAverageThroughput(), 0.001);
    assertTrue(benchmark.isTearDownOneTimeCalled());
  }
}
