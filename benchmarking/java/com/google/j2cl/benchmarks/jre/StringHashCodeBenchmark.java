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

/** Benchmark for {@link String#hashCode} which generally measure char iteration performance. */
public class StringHashCodeBenchmark extends AbstractBenchmark {

  private String string;

  @Override
  public Object run() {
    return string.hashCode();
  }

  private StringBuilder builder;

  @Override
  public void setupOneTime() {
    builder = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      builder.append(i).append("Some other string to build a longer one");
    }
  }

  @Override
  public void setup() {
    // With "new String" we ensure a fresh String copy for J2WASM.
    string = new String(builder.toString());
  }
}
