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
import java.util.Map.Entry;

/** Benchmark for iteration operation of a {@link HashMap}. */
public class HashMapObjectIterationBenchmark extends AbstractBenchmark {

  private int length;

  private HashMap<Object, Object> map;

  @Override
  public Object run() {
    Object[] array = new Object[length * 2];

    int count = 0;
    for (Entry<Object, Object> entry : map.entrySet()) {
      array[count++] = entry.getKey();
      array[count++] = entry.getValue();
    }

    if (array[array.length - 1] == null) {
      throw new AssertionError();
    }

    return array;
  }

  @Override
  public void setupOneTime() {
    length = 1000;

    map = new HashMap<>();

    for (int i = 0; i < length; i++) {
      map.put(new Key(), new Value());
    }
  }
}
