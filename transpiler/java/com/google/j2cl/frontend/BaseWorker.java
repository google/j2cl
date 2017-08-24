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
package com.google.j2cl.frontend;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * A base class for running processes as blaze workers. Used for both the transpiler
 * and @GwtIncompaitble stripper tool.
 */
public abstract class BaseWorker {
  /**
   * Note that you must output errors and warnings to the provided outputStream to avoid
   * interrupting the worker protocol which occurs over stdout.
   */
  protected abstract int runAsWorker(final String[] args, PrintStream outputStream);

  protected abstract void runStandard(final String[] args);

  public final void start(String[] args) {
    boolean shouldRunAsWorker = Arrays.asList(args).contains("--persistent_worker");
    if (shouldRunAsWorker) {
      spawnWorker();
      return;
    }
    try {
      runStandard(FrontendUtils.expandFlagFile(args));
    } catch (IOException e) {
      throw new RuntimeException("Unable to load flagfile", e);
    }
  }

  private void spawnWorker() {
    try {
      while (true) {
        WorkRequest workRequest = WorkRequest.parseDelimitedFrom(getInput());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String[] args = workRequest.getArgumentsList().toArray(new String[0]);
        int exitCode = runAsWorker(args, new PrintStream(outputStream));

        WorkResponse.newBuilder()
            .setExitCode(exitCode)
            .setOutput(outputStream.toString())
            .build()
            .writeDelimitedTo(getOutput());
      }
    } catch (IOException e) {
      // IOException will only occur if System.in has been closed
      // In that case we silently exit our process
    }
  }

  @VisibleForTesting
  protected InputStream getInput() {
    return System.in;
  }

  @VisibleForTesting
  protected OutputStream getOutput() {
    return System.out;
  }
}
