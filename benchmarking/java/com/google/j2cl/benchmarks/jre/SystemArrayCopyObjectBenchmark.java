/*
 * Copyright 2021 Google Inc.
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

/** Benchmark for Object array copy performance. */
public class SystemArrayCopyObjectBenchmark extends AbstractBenchmark {
  private Item[] array;
  private static final int ARRAY_LENGTH = 10000;

  @Override
  public Object run() {
    Item[] copy = new Item[ARRAY_LENGTH];
    System.arraycopy(array, 0, copy, 0, ARRAY_LENGTH);

    if (copy[ARRAY_LENGTH - 1].state != array[ARRAY_LENGTH - 1].state) {
      throw new AssertionError();
    }
    return copy;
  }

  @Override
  public void setupOneTime() {
    array = new Item[ARRAY_LENGTH];
    for (int i = 0; i < array.length; i++) {
      array[i] = new Item(i);
    }
  }

  private static class Item {
    private final int state;

    Item(int state) {
      this.state = state;
    }
  }
}
