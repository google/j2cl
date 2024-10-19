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
import com.google.j2cl.benchmarks.jre.helper.Key;
import com.google.j2cl.benchmarks.jre.helper.Value;
import java.util.HashMap;

/** Benchmark for {@link HashMap} get performance for String keys. */
public class HashMapObjectContainsKeyBenchmark extends AbstractBenchmark {

  private Object[] keysInMap;
  private Object[] keysNotInMap;
  private int length;
  private HashMap<Object, Object> map;

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
    keysInMap = new Object[length];
    keysNotInMap = new Object[length];

    map = new HashMap<>();

    for (int i = 0; i < length; i++) {
      Key key = new Key();
      Value value = new Value();
      keysInMap[i] = key;
      keysNotInMap[i] = value;
      map.put(key, value);
    }
  }
}
