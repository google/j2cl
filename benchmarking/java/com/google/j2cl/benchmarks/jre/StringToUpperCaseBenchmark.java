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

/** Benchmark for {@link String#toUpperCase}. */
public class StringToUpperCaseBenchmark extends AbstractBenchmark {

  private int length;
  private String[] from;
  private String[] to;

  @Override
  public Object run() {
    for (int i = 0; i < length; i++) {
      to[i] = from[i].toUpperCase();
    }
    return to;
  }

  @Override
  public void setupOneTime() {
    length = 100;
    from = new String[length];
    to = new String[length];

    for (int i = 0; i < length; i++) {
      from[i] = "thisissomekey" + i;
    }
  }
}
