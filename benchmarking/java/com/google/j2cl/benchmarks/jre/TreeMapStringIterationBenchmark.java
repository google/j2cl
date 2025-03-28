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
import java.util.Map.Entry;
import java.util.TreeMap;

/** Benchmark for iteration on a {@link TreeMap} with String keys. */
public class TreeMapStringIterationBenchmark extends AbstractBenchmark {
  private static final int ENTRY_COUNT = 1000;
  private Object[] array;
  private TreeMap<String, String> map;

  @Override
  public Object run() {
    int count = 0;
    for (Entry<String, String> entry : map.entrySet()) {
      array[count++] = entry.getKey();
      array[count++] = entry.getValue();
    }

    if (count != ENTRY_COUNT * 2) {
      throw new AssertionError();
    }

    return array;
  }

  @Override
  public void setupOneTime() {
    array = new Object[ENTRY_COUNT * 2];
    map = new TreeMap<>();

    for (int i = 0; i < ENTRY_COUNT; i++) {
      String key = "thisissomekey" + i;
      String value = "thisissomevalue" + i;
      map.put(key, value);
    }
  }
}
