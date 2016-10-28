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

import com.google.j2cl.transpiler.J2clTranspiler.Result;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs J2clTranspiler in its own thread.
 *
 * <p>J2clTranspiler stores state in thread local variables. This driver runs the transpiler in its
 * own thread for isolation.
 */
public class J2clTranspilerDriver {

  public static Result transpile(String[] args) {
    // Run the transpiler in its own tread
    try {
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      Future<Result> futureResult = executorService.submit(() -> doTranspile(args));
      return futureResult.get();
    } catch (InterruptedException | ExecutionException e) {
      return J2clTranspiler.Result.fromException(-3, e);
    }
  }

  private static Result doTranspile(String[] args) {
    try {
      return new J2clTranspiler().transpile(args);
    } catch (RuntimeException e) {
      return J2clTranspiler.Result.fromException(-3, e);
    }
  }
}
