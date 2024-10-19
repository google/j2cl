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
package com.google.j2cl.benchmarks.octane;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import com.google.j2cl.benchmarks.octane.richards.Richards;
import javax.annotation.Nullable;

/**
 * This is the Java port of the JavaScript implementation of Richards benchmark from the V8 octane
 * benchmark suite.
 *
 * <p>Main focus: property load/store, function/method calls
 *
 * <p>Secondary focus: code optimization, elimination of redundant code
 */
public class RichardsBenchmark extends AbstractBenchmark {
  private Richards richards;

  @Override
  public void setupOneTime() {
    richards = new Richards();
  }

  @Nullable
  @Override
  public Object run() {
    richards.runRichards();
    return null;
  }

  @Override
  public void tearDownOneTime() {
    richards = null;
  }
}
