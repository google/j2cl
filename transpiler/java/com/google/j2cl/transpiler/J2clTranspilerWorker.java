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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.j2cl.transpiler.J2clTranspiler.Result;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
public class J2clTranspilerWorker {
  public static void main(String[] args) throws IOException {
    if (!shouldRunAsWorker(args)) {
      // Blaze decides at runtime through a flag if we should be running as a worker
      // or just be performing a regular compile
      // This falls back to the regular compiler.
      J2clTranspiler.main(args);
      return;
    }

    try {
      new J2clTranspilerWorker().run();
    } catch (Exception e) {
      Files.asCharSink(new File("/usr/local/google/home/tdeegan/trace.txt"), StandardCharsets.UTF_8)
          .write(e.toString());
    }
  }

  @VisibleForTesting
  void run() {
    try {
      dispatchWorkRequestsForever();
    } catch (IOException e) {
      // IOException will only occur if System.in has been closed
      // In that case we silently exit our process
    }
  }

  private WorkRequest getWorkRequest() throws IOException {
    return WorkRequest.parseDelimitedFrom(getInput());
  }

  @VisibleForTesting
  InputStream getInput() {
    return System.in;
  }

  private void dispatchWorkRequestsForever() throws IOException {
    while (true) {
      WorkRequest workRequest = getWorkRequest();

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int exitCode = runCompiler(extractArgs(workRequest), new PrintStream(outputStream));

      WorkResponse.newBuilder()
          .setExitCode(exitCode)
          .setOutput(outputStream.toString())
          .build()
          .writeDelimitedTo(System.out);
    }
  }

  @VisibleForTesting
  J2clTranspiler createTranspiler() {
    return new J2clTranspiler();
  }

  @VisibleForTesting
  void exit(int code) {
    System.exit(code);
  }

  private int runCompiler(final String[] args, PrintStream outputStream) {
    // Compiler has no static state, but rather uses thread local variables.
    // Because of this we invoke the compiler on a different thread each time.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Result> futureResult =
        executorService.submit(
            () -> {
              J2clTranspiler transpiler = createTranspiler();
              return transpiler.transpile(args);
            });

    try {
      Result result = futureResult.get();
      result.getProblems().report(outputStream, outputStream);
      return result.getExitCode();
    } catch (RuntimeException | ExecutionException e) {
      // Compiler had a bug, report this and quit the process.
      e.printStackTrace(System.err);
      exit(-2);
      return -3;
    } catch (InterruptedException e) {
      // Compilation has been interrupted, set the interrupt flag in the current thread.
      System.err.println("Transpilation interrupted.");
      Thread.currentThread().interrupt();
    }
    return 0;
  }

  private String[] extractArgs(WorkRequest workRequest) {
    return workRequest.getArgumentsList().toArray(new String[0]);
  }

  private static boolean shouldRunAsWorker(String[] args) {
    return Arrays.asList(args).contains("--persistent_worker");
  }
}
