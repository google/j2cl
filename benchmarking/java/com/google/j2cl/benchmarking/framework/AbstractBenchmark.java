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
package com.google.j2cl.benchmarking.framework;

/**
 * Base class for all benchmarks.
 *
 * <p>If you want to write a benchmark you will need to extend from this class.
 */
public abstract class AbstractBenchmark {
 
  /**
   * Run is called repeatedly by the benchmark framework.
   * <p>
   * In general a benchmark should return an object that has been calculated in
   * the benchmark, so that it will be impossible for GWT / JavaScript engines to
   * precalculate results of a benchmark. The returned object will be stored by the
   * benchmark system.
   * @return the object that should be stored by the benchmark system.
   */
  public abstract Object run();

  /**
   * Called every time before {@link #run()} is called.
   */
  public void setup() {}

  /**
   * Called one time before the benchmark is executed.
   */
  public void setupOneTime() {}

  /**
   * Called every time after {@link #run()} has been called.
   */
  public void tearDown() {}

  /**
   * Called one time after the benchmark is executed.
   */
  public void tearDownOneTime() {}
}
