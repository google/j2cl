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
package com.google.j2cl.common.bazel;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

  protected final Problems problems = new Problems();

  protected abstract void run();

  /**
   * Process the request described by the arguments. Note that you must output errors and warnings
   * via {@link Problems} to avoid interrupting the worker protocol which occurs over stdout.
   */
  private int processRequest(List<String> args, PrintWriter pw) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      problems.error("%s", e.getMessage());
      return problems.reportAndGetExitCode(pw);
    }

    try {
      run();
    } catch (Problems.Exit e) {
      // Program aborted due to errors recorded in problems.
    }
    return problems.reportAndGetExitCode(pw);
  }

  public static final void start(String[] args, Supplier<BazelWorker> workerSupplier)
      throws Exception {
    if (args.length == 1 && args[0].equals("--persistent_worker")) {
      runPersistentWorker(workerSupplier);
    } else {
      runStandaloneWorker(workerSupplier, expandFlagFile(args));
    }
  }

  @SuppressWarnings("SystemExitOutsideMain")
  private static void runStandaloneWorker(Supplier<BazelWorker> workerSupplier, List<String> args) {
    // This is a single invocation of builder that exits after it processed the request.
    int exitCode =
        workerSupplier
            .get()
            .processRequest(args, new PrintWriter(System.err, /* autoFlush= */ true));
    System.exit(exitCode);
  }

  private static void runPersistentWorker(Supplier<BazelWorker> workerSupplier) throws IOException {
    WorkRequestHandler workerHandler =
        new WorkRequestHandler.WorkRequestHandlerBuilder(
                new WorkRequestHandler.WorkRequestCallback(
                    (request, pw) ->
                        workerSupplier.get().processRequest(request.getArgumentsList(), pw)),
                System.err,
                new ProtoWorkerMessageProcessor(System.in, System.out))
            .build();
    workerHandler.processRequests();
  }

  /**
   * Loads a potential flag file and returns the flags. Flag files are only allowed as the last
   * parameter and need to start with an '@'.
   */
  private static List<String> expandFlagFile(String[] args) throws IOException {
    List<String> combinedArgs = Lists.newArrayList(args);
    String lastArg = Iterables.getLast(combinedArgs, null);
    if (lastArg != null && lastArg.startsWith("@")) {
      combinedArgs.remove(combinedArgs.size() - 1); // Remove flag file
      combinedArgs.addAll(getArgsFromFlagFile(lastArg.substring(1)));
    }
    return combinedArgs;
  }

  private static List<String> getArgsFromFlagFile(String flagFileName) throws IOException {
    String flagFileContent = Files.asCharSource(new File(flagFileName), UTF_8).read();
    return Splitter.on('\n').omitEmptyStrings().splitToList(flagFileContent);
  }
}
