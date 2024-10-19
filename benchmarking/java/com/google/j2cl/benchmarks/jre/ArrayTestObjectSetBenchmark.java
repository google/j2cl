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

/** Benchmark array set performance. */
public class ArrayTestObjectSetBenchmark extends AbstractBenchmark {

  private Object[] array;

  @Override
  public Object run() {
    var value = new Object();
    for (int i = 0; i < array.length; i++) {
      array[i] = value;
    }

    return array;
  }

  @Override
  public void setupOneTime() {
    array = new Object[1000];
  }
}
