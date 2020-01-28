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
import com.google.j2cl.common.Problems;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * A base class for running processes as blaze workers. Used for both the transpiler
 * and @GwtIncompatible stripper tool.
 *
 * <p>Partially adapted from {@code com.google.devtools.build.buildjar.BazelJavaBuilder}.
 */
public abstract class BazelWorker {

  protected abstract Problems run();

  /**
   * Process the request described by the arguments. Note that you must output errors and warnings
   * via {@link Problems} to avoid interrupting the worker protocol which occurs over stdout.
   */
  private Problems processRequest(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);
    Problems problems = new Problems();

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      problems.error("Error: %s", e.getMessage());
      return problems;
    }

    try {
      return run();
    } catch (Problems.Exit e) {
      return e.getProblems();
    }
  }

  public static final void start(String[] args, Supplier<BazelWorker> workerSupplier)
      throws Exception {
    if (args.length == 1 && args[0].equals("--persistent_worker")) {
      runPersistentWorker(workerSupplier);
    } else {
      runStandaloneWorker(workerSupplier, expandFlagFile(args));
    }
  }

  private static void runStandaloneWorker(Supplier<BazelWorker> workerSupplier, String[] args)
      throws IOException {
    // This is a single invocation of builder that exits after it processed the request.
    int exitCode = workerSupplier.get().processRequest(args).reportAndGetExitCode(System.err);
    System.exit(exitCode);
  }

  private static void runPersistentWorker(Supplier<BazelWorker> workerSupplier) throws IOException {
    while (true) {
      WorkRequest request = WorkRequest.parseDelimitedFrom(System.in);

      if (request == null) {
        break;
      }

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      String[] args = request.getArgumentsList().toArray(new String[0]);
      int exitCode = workerSupplier.get().processRequest(args).reportAndGetExitCode(pw);
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
    String flagFileContent =
        Files.asCharSource(new File(flagFileName), StandardCharsets.UTF_8).read();
    return Splitter.on('\n').omitEmptyStrings().split(flagFileContent);
  }
}
