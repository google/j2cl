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
import java.util.TreeMap;

/** Benchmark for {@link TreeMap} contains with String values. */
public class TreeMapStringContainsValueBenchmark extends AbstractBenchmark {
  private String[] valuesInMap;
  private String[] valuesNotInMap;
  private int length;

  private TreeMap<String, String> map;

  @Override
  public Object run() {
    for (int i = 0; i < length; i++) {
      if (!map.containsValue(valuesInMap[i])) {
        throw new AssertionError();
      }
      if (map.containsValue(valuesNotInMap[i])) {
        throw new AssertionError();
      }
    }

    return map;
  }

  @Override
  public void setupOneTime() {
    length = 1000;
    valuesInMap = new String[length];
    valuesNotInMap = new String[length];

    map = new TreeMap<>();

    for (int i = 0; i < length; i++) {
      String key = "thisissomekey" + i;
      String value = "thisissomevalue" + i;

      valuesInMap[i] = value;
      valuesNotInMap[i] = key;
      map.put(key, value);
    }
  }
}
