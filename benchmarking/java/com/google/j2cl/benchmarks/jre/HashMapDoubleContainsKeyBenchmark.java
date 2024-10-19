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

/** Benchmark for {@link HashMap} get performance for Double keys. */
public class HashMapDoubleContainsKeyBenchmark extends AbstractBenchmark {
  private Double[] keysInMap;
  private Double[] keysNotInMap;
  private int length;
  private HashMap<Double, String> map;

  @Override
  public Object run() {
    for (int i = 0; i < length; i++) {
      if (!map.containsKey(keysInMap[i])) {
        throw new AssertionError();
      }
      if (map.containsKey(keysNotInMap[i])) {
        throw new AssertionError();
      }
    }

    return map;
  }

  @Override
  public void setupOneTime() {
    length = 1000;
    keysInMap = new Double[length];
    keysNotInMap = new Double[length];

    map = new HashMap<>();

    for (int i = 0; i < length; i++) {
      Double key = Double.valueOf(i);
      Double keyNotIn = Double.valueOf(i + length);
      String value = "thisissomevalue" + i;

      // Note that we are using the same identity which would take shortcut for keys that exists in
      // the map but we would be also getting the slow path with the keys that are not on the map so
      // we will be benchmarking both scenarios.
      keysInMap[i] = key;
      keysNotInMap[i] = keyNotIn;
      map.put(key, value);
    }
  }
}
