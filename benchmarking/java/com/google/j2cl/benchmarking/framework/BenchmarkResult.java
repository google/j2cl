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


/** A benchmark result contains all information for an executed benchmark. */
public class BenchmarkResult {
  static BenchmarkResult from(double[] throughputs) {
    BenchmarkResult result = new BenchmarkResult();
    result.throughputs = throughputs;

    for (double iterationThroughput : throughputs) {
      result.averageThroughput += iterationThroughput;
    }
    result.averageThroughput /= throughputs.length;

    return result;
  }

  /** Average number of benchmark run per millisecond. */
  private double averageThroughput;

  /** Throughputs measured from all iterations. */
  private double[] throughputs;

  public double getAverageThroughput() {
    return averageThroughput;
  }

  public double[] getThroughputs() {
    return throughputs;
  }
}
