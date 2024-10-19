/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.benchmarks.jre;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;

/**
 * Benchmark to monitor performance for adding longs in GWT.
 *
 * <p>This benchmark in particular only uses numbers that can be represented with only 31 bits (see
 * SMI implementation in V8).
 */
public class LongAddSmallNumbersBenchmark extends AbstractBenchmark {

  private long[] array;

  @Override
  public Object run() {
    long sum = 0;
    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }

    if (sum != -500) {
      throw new AssertionError();
    }

    return Long.valueOf(sum);
  }

  @Override
  public void setupOneTime() {
    array = new long[1000];
    for (int i = 0; i < array.length; i++) {
      if (i % 2 == 0) {
        array[i] = i;
      } else {
        array[i] = -i;
      }
    }
  }
}
