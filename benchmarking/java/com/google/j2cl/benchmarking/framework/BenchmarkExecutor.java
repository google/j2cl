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

import java.util.logging.Logger;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** The BenchmarkExecutor executes benchmarks and measures their performance. */
public final class BenchmarkExecutor {
  static {
    CollectionUtilizer.dependOnAllCollections();
  }

  public static BenchmarkResult execute(AbstractBenchmark benchmark) {
    // Run the benchmark with 5 warm-up rounds of 1 second each and then with 5 measurement rounds
    // of 1 second each.
    return execute(benchmark, Clock.DEFAULT, 10, 5, 1000);
  }

  public static void prepareForRunOnce(AbstractBenchmark benchmark) {
    // Warm up the benchmark with 5 rounds.
    execute(benchmark, Clock.DEFAULT, 6, 5, 1000);

    // Prepare the benchmark for another run. Note that it was teared down after the execute.
    benchmark.setupOneTime();
    benchmark.setup();
  }

  /**
   * Runs a benchmark with a certain amount of rounds defined by <code>timePerIterations</code>.
   * Each round has a duration of <code>timePerIterations</code> milliseconds. The measurements will
   * be taken once a number of warmup rounds defined by <code>warmupIterations</code> is executed.
   */
  static BenchmarkResult execute(
      AbstractBenchmark benchmark,
      Clock clock,
      int totalIterations,
      int warmupIterations,
      long timePerIterationInMs) {
    if (totalIterations < warmupIterations) {
      throw new IllegalArgumentException();
    }

    int measurementIterations = totalIterations - warmupIterations;

    log("# Running %s", benchmark.getClass().getSimpleName());
    log("# Warmup: %s iterations, %s ms each", warmupIterations, timePerIterationInMs);
    log("# Measurement: %s iterations, %s ms each", measurementIterations, timePerIterationInMs);

    benchmark.setupOneTime();
    long timePerIterationInNanos = timePerIterationInMs * 1_000_000;
    double[] throughputs = new double[measurementIterations];
    for (int iteration = 0; iteration < totalIterations; iteration++) {
      boolean isWarmup = iteration < warmupIterations;
      long iterationTimeInNanos = 0;
      int runs = 0;
      Platform.forceGc();

      while (iterationTimeInNanos < timePerIterationInNanos) {
        benchmark.setup();
        long before = clock.now();
        Object result = benchmark.run();
        long executionTime = clock.now() - before;
        benchmark.tearDown();

        iterationTimeInNanos += executionTime;
        runs++;
        // We use the results to avoid V8 or the JVM to figure out whole benchmark doesn't have
        // side effects, consider it as death code and not even execute it.
        useResult(result);
      }

      double iterationThroughput = runs / (iterationTimeInNanos / 1_000_000d);

      if (!isWarmup) {
        throughputs[iteration - warmupIterations] = iterationThroughput;
      }
      log(
          "%sIteration %s: %s ops/ms",
          (isWarmup ? "# Warmup " : ""),
          (isWarmup ? iteration : iteration - warmupIterations),
          iterationThroughput);
    }

    benchmark.tearDownOneTime();

    BenchmarkResult benchmarkResult = BenchmarkResult.from(throughputs);
    log("Throughput: %s ops/ms", benchmarkResult.getAverageThroughput());
    return benchmarkResult;
  }

  private static void log(String s, Object... args) {
    for (Object arg : args) {
      s = s.replaceFirst("%s", arg.toString());
    }
    Logger.getGlobal().info(s);
  }

  private static void useResult(Object o) {
    boolean isJvm = System.getProperty("java.version", null) != null;
    if (isJvm) {
      // JVM doesn't need user result to disable optimizations.
      return;
    }
    Global.__benchmarking_result = externalize(o);
  }

  @Wasm("extern.externalize")
  private static External externalize(Object result) {
    return (External) result;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface External {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "goog.global")
  private static class Global {
    public static External __benchmarking_result;
  }

  private BenchmarkExecutor() {}
}
