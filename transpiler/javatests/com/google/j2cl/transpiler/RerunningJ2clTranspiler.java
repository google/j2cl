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

import com.google.j2cl.errors.Errors;

/**
 * A transpiler entry point that compiles code twice.
 *
 * <p>This is used in testing to run the transpiler twice and possibly uncover static state within
 * the transpiler.
 */
public class RerunningJ2clTranspiler {

  private static class CompilerRunner implements Runnable {

    private boolean success;
    private final String[] args;

    public CompilerRunner(String[] args) {
      this.args = args;
    }

    public boolean compile() throws InterruptedException {
      Thread thread = new Thread(this);
      thread.start();
      thread.join();
      return success;
    }

    @Override
    public void run() {
      J2clTranspiler transpiler = new J2clTranspiler(args);

      try {
        transpiler.run();
        success = true;
      } catch (Errors.Exit e) {
        // Compiler signaled that the compiler was not successful
        success = false;
      } catch (RuntimeException r) {
        // Compiler correctness preconditions were violated. Log a stacktrace and quit the test
        r.printStackTrace();
        System.exit(-1);
      }
    }
  }

  public static void main(final String[] args) {
    boolean firstCompileSuccess = compile(new CompilerRunner(args));
    boolean secondCompileSuccess = compile(new CompilerRunner(args));

    if (firstCompileSuccess != secondCompileSuccess) {
      System.err.println("Compiles do not agree on success");
      System.exit(-1);
    }
  }

  private static boolean compile(CompilerRunner compilerRunner) {
    try {
      return compilerRunner.compile();
    } catch (InterruptedException e) {
      System.exit(-1);
      return false;
    }
  }
}
