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
package com.google.j2cl.benchmarks.jre;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;

/** Benchmark integer sum performance for arrays. */
public class ArrayIntSumBenchmark extends AbstractBenchmark {

  private int[] array;
  @Override
  public Object run() {

    int sum = 0;

    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }

    // compare value - disables opts and finds JIT bugs
    if (sum != 49995000) {
      throw new AssertionError();
    }

    return Integer.valueOf(sum);
  }

  @Override
  public void setupOneTime() {
    array = new int[10000];
    for (int i = 0; i < array.length; i++) {
      array[i] = i;
    }
  }
}
