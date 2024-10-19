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

/** Benchmark for {@link TreeMap} get performance for String keys. */
public class TreeMapStringContainsKeyBenchmark extends AbstractBenchmark {
  private String[] keysInMap;
  private String[] keysNotInMap;
  private int length;
  private TreeMap<String, String> map;

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
    keysInMap = new String[length];
    keysNotInMap = new String[length];

    map = new TreeMap<>();

    for (int i = 0; i < length; i++) {
      String key = "thisissomekey" + i;
      String value = "thisissomevalue" + i;

      keysInMap[i] = key;
      keysNotInMap[i] = value;
      map.put(key, value);
    }
  }
}
