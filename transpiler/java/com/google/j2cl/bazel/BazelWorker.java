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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    int processRequest(String[] args, PrintWriter outputStream);
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
      int exitCode = handler.processRequest(expandFlagFile(args), err);
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

  /**
   * Loads a potential flag file and returns the flags. Flag files are only allowed as the last
   * parameter and need to start with an '@'.
   */
  private static String[] expandFlagFile(String[] args) throws IOException {
    if (args.length == 0) {
      return args;
    }

    String lastArg = args[args.length - 1];
    if (lastArg == null || !lastArg.startsWith("@") || lastArg.length() == 1) {
      return args;
    }

    List<String> combinedArgs = new ArrayList<>();
    Collections.addAll(combinedArgs, args);
    combinedArgs.remove(combinedArgs.size() - 1); // Remove flag file
    Iterables.addAll(combinedArgs, getArgsFromFlagFile(lastArg.substring(1)));
    return combinedArgs.toArray(new String[0]);
  }

  private static Iterable<String> getArgsFromFlagFile(String flagFileName) throws IOException {
    String flagFileContent = Files.toString(new File(flagFileName), StandardCharsets.UTF_8);
    return Splitter.on('\n').omitEmptyStrings().split(flagFileContent);
  }
}
