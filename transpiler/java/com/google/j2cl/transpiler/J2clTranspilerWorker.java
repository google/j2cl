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
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.j2cl.errors.Errors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs J2clTranspiler as a blaze worker.
 *
 * <p>Blaze workers allow compilers to run in a JVM that is not being terminated after every compile
 * and thus gain significant speedups. This class implements the blaze worker protocol and drives
 * J2clTranspiler as a worker.
 */
public class J2clTranspilerWorker {
  public static void main(String[] args) {
    if (!shouldRunAsWorker(args)) {
      // Blaze decides at runtime through a flag if we should be running as a worker
      // or just be performing a regular compile
      // This falls back to the regular compiler.
      J2clTranspiler.main(args);
      return;
    }

    new J2clTranspilerWorker().run();
  }

  private ByteArrayOutputStream stdoutStream;
  private ByteArrayOutputStream stdErrStream;
  private PrintStream originalStdout;
  private PrintStream originalStdErr;

  @VisibleForTesting
  void run() {
    trapOutputs();

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

  private void trapOutputs() {
    originalStdout = System.out;
    originalStdErr = System.err;
    stdoutStream = new ByteArrayOutputStream();
    stdErrStream = new ByteArrayOutputStream();
    System.setErr(new PrintStream(stdErrStream, true));
    System.setOut(new PrintStream(stdoutStream, true));
  }

  private void dispatchWorkRequestsForever() throws IOException {
    while (true) {
      WorkRequest workRequest = getWorkRequest();

      String[] args = extractArgs(workRequest);

      boolean success = runCompiler(args);

      outputResult(success);
    }
  }

  private void outputResult(boolean success) throws IOException {
    int exitCode = success ? 0 : 1;
    String output = success ? asString(stdoutStream) : asString(stdErrStream);

    WorkResponse.newBuilder()
        .setExitCode(exitCode)
        .setOutput(output)
        .build()
        .writeDelimitedTo(originalStdout);

    stdoutStream.reset();
    stdErrStream.reset();
  }

  @VisibleForTesting
  J2clTranspiler createTranspiler(String[] args) {
    return new J2clTranspiler(args);
  }

  @VisibleForTesting
  void exit(int code) {
    System.exit(code);
  }

  private boolean runCompiler(final String[] args) {
    final AtomicBoolean successful = new AtomicBoolean(false);

    // Compiler has no static state, but rather uses thread local variables.
    // Because of this we invoke the compiler on a different thread each time.
    Runnable runnable =
        () -> {
          J2clTranspiler transpiler = createTranspiler(args);
          try {
            transpiler.run();
            successful.set(true);
          } catch (Errors.Exit e) {
            // Compiler is signaling a compile failure for the given code.
            successful.set(false);
          } catch (RuntimeException e) {
            // Compiler had a bug, report this and quit the process.
            e.printStackTrace(originalStdErr);
            exit(-2);
          }
        };

    Thread thread = new Thread(runnable);
    thread.start();

    try {
      thread.join();
    } catch (InterruptedException e) {
      // we are not triggering interupts
    }

    return successful.get();
  }

  private String[] extractArgs(WorkRequest workRequest) {
    return workRequest.getArgumentsList().toArray(new String[0]);
  }

  private static boolean shouldRunAsWorker(String[] args) {
    return Arrays.asList(args).contains("--persistent_worker");
  }

  private static String asString(ByteArrayOutputStream s) {
    return new String(s.toByteArray(), StandardCharsets.UTF_8);
  }
}
