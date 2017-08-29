/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import com.google.j2cl.bazel.BaseWorker;
import com.google.j2cl.transpiler.J2clTranspiler.Result;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs J2clTranspiler as a blaze worker.
 *
 * <p>Blaze workers allow compilers to run in a JVM that is not being terminated after every compile
 * and thus gain significant speedups. This class implements the blaze worker protocol and drives
 * J2clTranspiler as a worker.
 */
public class J2clTranspilerWorker extends BaseWorker {
  public static void main(String[] args) {
    new J2clTranspilerWorker().start(args);
  }

  @Override
  public int runAsWorker(String[] args, PrintStream outputStream) {
    // Compiler has no static state, but rather uses thread local variables.
    // Because of this, we invoke the compiler on a different thread each time.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Result> futureResult =
        executorService.submit(
            () -> {
              J2clTranspiler transpiler = new J2clTranspiler();
              return transpiler.transpile(args);
            });

    try {
      Result result = futureResult.get();
      result.getProblems().report(outputStream);
      return result.getExitCode();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Internal compiler error: ", e);
    }
  }

  @Override
  public void runStandard(String[] args) {
    J2clTranspiler.main(args);
  }
}
