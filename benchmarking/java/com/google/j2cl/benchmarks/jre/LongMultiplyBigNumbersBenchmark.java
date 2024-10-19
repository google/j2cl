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
 * Benchmark to monitor performance for multiplying longs in GWT.
 *
 * <p>This benchmarks will incur values that need more than 54 bits to be represented and thus will
 * not fit in a simple JavaScript double.
 */
public class LongMultiplyBigNumbersBenchmark extends AbstractBenchmark {

  private static final long BITS_54 = 1L << 54;

  private long[] array;
  private long[] result;

  @Override
  public Object run() {
    for (int i = 0; i < array.length - 1; i++) {
      result[i] = array[i] * array[i + 1];
    }
    if (result[0] != 54043195528445954L) {
      throw new AssertionError();
    }

    if (result[result.length - 1] != -882705526963618216L) {
      throw new AssertionError();
    }
    return result;
  }

  @Override
  public void setupOneTime() {
    array = new long[1000];
    result = new long[999];
    for (int i = 0; i < array.length; i++) {
      array[i] = i + 1 + BITS_54;
    }
    result[0] = 1L << 55; // to avoid reallocations in v8 since number type might change
  }
}
