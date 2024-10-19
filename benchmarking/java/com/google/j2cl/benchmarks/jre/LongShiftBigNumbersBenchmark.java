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
 * Benchmark to monitor performance for shifting longs in GWT.
 *
 * <p>This benchmarks will incur values that need more than 54 bits to be represented and thus will
 * not fit in a simple JavaScript double.
 */
public class LongShiftBigNumbersBenchmark extends AbstractBenchmark {

  private static final long BITS_55 = 1L << 55;

  private long[] array;
  private long[] result;

  @Override
  public Object run() {
    long product = 1;
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] << 2;
    }
    if (result[0] != 144115188075855872L) {
      throw new AssertionError();
    }

    if (result[result.length - 1] != 144115188075859868L) {
      throw new AssertionError();
    }

    return Long.valueOf(product);
  }

  @Override
  public void setupOneTime() {
    array = new long[1000];
    result = new long[1000];
    for (int i = 0; i < array.length; i++) {
      array[i] = BITS_55 + i;
      result[i] = BITS_55; // to avoid reallocations in v8 since number type might change
    }
  }
}
