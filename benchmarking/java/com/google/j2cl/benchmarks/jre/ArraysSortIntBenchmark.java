/*
 * Copyright 2023 Google Inc.
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
import com.google.j2cl.benchmarks.jre.helper.ArrayConstants;
import java.util.Arrays;

/** Benchmark for primitive array sort performance. */
public class ArraysSortIntBenchmark extends AbstractBenchmark {
  private int[] array;

  @Override
  public Object run() {
    Arrays.sort(array);

    if (array[0] != 455) {
      throw new AssertionError();
    }
    return array;
  }

  @Override
  public void setup() {
    array = Arrays.copyOf(ArrayConstants.unsorted, ArrayConstants.unsorted.length);
  }
}
