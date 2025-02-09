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
import java.util.HashMap;

/** Benchmark for Double insertion into {@link HashMap}. */
public class HashMapDoublePutBenchmark extends AbstractBenchmark {
  private int length;
  private Double[] keys;
  private String[] values;

  @Override
  public Object run() {
    HashMap<Double, String> map = new HashMap<>();

    for (int i = 0; i < length; i++) {
      map.put(keys[i], values[i]);
    }

    if (map.size() != length) {
      throw new AssertionError();
    }

    return map;
  }

  @Override
  public void setupOneTime() {
    length = 1000;
    keys = new Double[length];
    values = new String[length];

    for (int i = 0; i < length; i++) {
      keys[i] = Double.valueOf(i);
      values[i] = "thisissomevalue" + i;
    }
  }
}
