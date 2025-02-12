/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.benchmarks.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;

/** Benchmark for the compiler itself. */
public class JavaCompileBenchmark extends AbstractBenchmark {

  @Override
  public Object run() {
    try {
      return compile();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TranspileResult compile() throws Exception {
    return newTesterWithDefaults()
        .addCompilationUnit("org.jspecify.annotations.Nullable", "public @interface Nullable {}")
        .addSourcePathArg("samples/box2d/src/main/java/libbox2d_library-j2cl-src.jar")
        .assertTranspileSucceeds();
  }
}
