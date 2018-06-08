/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.bazel;

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.j2cl.frontend.FrontendUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A base class for running processes as blaze workers. Used for both the transpiler
 * and @GwtIncompatible stripper tool.
 *
 * <p>Partially adapted from {@code com.google.devtools.build.buildjar.BazelJavaBuilder}.
 */
public final class BazelWorker {

  /** A handler that could process requests designated by Bazel. */
  public interface WorkHandler {
    /**
     * Process the request described by the arguments. Note that you must output errors and warnings
     * to the provided outputStream to avoid interrupting the worker protocol which occurs over
     * stdout.
     */
    int processRequest(final String[] args, PrintWriter outputStream);
  }

  public static final void start(String[] args, WorkHandler handler) {
    try {
      int exitCode = execute(args, handler);
      System.exit(exitCode);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static final int execute(String[] args, WorkHandler handler) throws IOException {
    if (args.length == 1 && args[0].equals("--persistent_worker")) {
      runPersistentWorker(handler);
      return 0;
    } else {
      // This is a single invocation of builder that exits after it processed the request.
      PrintWriter err = new PrintWriter(System.err);
      int exitCode = handler.processRequest(FrontendUtils.expandFlagFile(args), err);
      err.flush();
      return exitCode;
    }
  }

  private static void runPersistentWorker(WorkHandler handler) throws IOException {
    while (true) {
      WorkRequest request = WorkRequest.parseDelimitedFrom(System.in);

      if (request == null) {
        break;
      }

      try (StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw)) {
        String[] args = request.getArgumentsList().toArray(new String[0]);
        int exitCode = handler.processRequest(args, pw);
        WorkResponse.newBuilder()
            .setOutput(sw.toString())
            .setExitCode(exitCode)
            .build()
            .writeDelimitedTo(System.out);
        System.out.flush();

        // Hint to the system that now would be a good time to run a gc.  After a compile
        // completes lots of objects should be available for collection and it should be cheap to
        // collect them.
        System.gc();
      }
    }
  }
}
