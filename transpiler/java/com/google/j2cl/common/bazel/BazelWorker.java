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
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.build.lib.worker.ProtoWorkerMessageProcessor;
import com.google.devtools.build.lib.worker.WorkRequestHandler;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.bazel.profiler.Profiler;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * A base class for running processes as blaze workers. Used for both the transpiler
 * and @GwtIncompatible stripper tool.
 *
 * <p>Partially adapted from {@code com.google.devtools.build.buildjar.BazelJavaBuilder}.
 */
public abstract class BazelWorker {

  protected final Problems problems = new Problems();
  protected Path workdir;

  @Option(name = "-profileOutput", hidden = true)
  Path profileOutput = null;

  protected abstract void run();

  /**
   * Process the request described by the arguments. Note that you must output errors and warnings
   * via {@link Problems} to avoid interrupting the worker protocol which occurs over stdout.
   */
  private int processRequest(List<String> args, PrintWriter pw, String sandboxDir) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      problems.error("%s", e.getMessage());
      return problems.reportAndGetExitCode(pw);
    }
    this.workdir = Path.of(sandboxDir);

    var profiler = Profiler.create(workdir, profileOutput);
    try {
      run();
    } catch (RuntimeException | Error e) {
      // The exceptions might be wrapped in another exception to provide more context. However
      // if the root cause is a Problems.Exit then either the Program aborted due to errors that
      // were already recorded in problems or it was due to cancellation. Either way we should not
      // crash the worker if the root cause is a Problems.Exit.
      if (!(Throwables.getRootCause(e) instanceof Problems.Exit)) {
        throw e;
      }
    } finally {
      profiler.stopProfile();
    }
    return problems.reportAndGetExitCode(pw);
  }

  private void cancel() {
    problems.requestCancellation();
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
            .processRequest(args, new PrintWriter(System.err, /* autoFlush= */ true), ".");
    System.exit(exitCode);
  }

  private static void runPersistentWorker(Supplier<BazelWorker> workerSupplier) throws IOException {
    var activeWorkers = new ConcurrentHashMap<Integer, BazelWorker>();
    WorkRequestHandler workerHandler =
        new WorkRequestHandler.WorkRequestHandlerBuilder(
                new WorkRequestHandler.WorkRequestCallback(
                    (request, pw) -> {
                      BazelWorker worker = workerSupplier.get();
                      activeWorkers.put(request.getRequestId(), worker);
                      try {
                        return worker.processRequest(
                            request.getArgumentsList(), pw, request.getSandboxDir());
                      } finally {
                        activeWorkers.remove(request.getRequestId());
                      }
                    }),
                System.err,
                new ProtoWorkerMessageProcessor(System.in, System.out))
            .setCancelCallback(
                (requestId, thread) -> {
                  BazelWorker worker = activeWorkers.get(requestId);
                  if (worker != null) {
                    worker.cancel();
                  }
                })
            .setIdleTimeBeforeGc(Duration.ofSeconds(4))
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
