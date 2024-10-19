/*
 * Copyright 2022 Google Inc.
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

/** String + Double performance test. */
public class StringConcatDoubleBenchmark extends AbstractBenchmark {

  private static final int LENGTH = 1000;

  private static final double DOUBLE_REF = Math.PI;

  private String[] array;

  @Override
  public Object run() {
    for (int i = 0; i < LENGTH; i = i + 2) {
      // Mix both short and long decimals to cover both scenarios.
      array[i] = "" + (double) i;
      array[i + 1] = "" + (DOUBLE_REF + i);
    }
    return array;
  }

  @Override
  public void setupOneTime() {
    array = new String[LENGTH];
  }
}
