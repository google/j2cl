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
package com.google.j2cl.transpiler.bazel;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.j2cl.bazel.BaseWorker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BaseWorkerTest {

  private String[] nonWorkerArgs = null;
  private String[] workerArgs = null;

  @Before
  public void setUp() {
    nonWorkerArgs = null;
    workerArgs = null;
  }

  private class WorkerForTest extends BaseWorker {
    @Override
    protected int runAsWorker(String[] args, PrintStream outputStream) {
      workerArgs = args;
      for (String arg : args) {
        outputStream.println(arg);
      }
      return 0;
    }

    @Override
    protected void runStandard(String[] args) {
      nonWorkerArgs = args;
    }
  }

  @Test
  public void runWorkerStandard() {
    String[] sampleArgs = {"--some", "--sample", "--args"};

    BaseWorker worker = new WorkerForTest();
    worker.start(sampleArgs);

    assertThat(nonWorkerArgs).isEqualTo(sampleArgs);
    assertThat(workerArgs).isNull();
  }

  @Test
  public void runWorkerTwiceThenCloseConnection() throws IOException {
    WorkRequest firstRequest =
        WorkRequest.newBuilder().addArguments("-d").addArguments("First.java").build();
    WorkRequest secondRequest =
        WorkRequest.newBuilder().addArguments("-d").addArguments("Second.java").build();

    // Add the first 2 work requests to the stream.
    final List<InputStream> streams = new ArrayList<>();
    ByteArrayOutputStream firstStream = new ByteArrayOutputStream();
    firstRequest.writeDelimitedTo(firstStream);
    streams.add(new ByteArrayInputStream(firstStream.toByteArray()));

    ByteArrayOutputStream secondStream = new ByteArrayOutputStream();
    secondRequest.writeDelimitedTo(secondStream);
    streams.add(new ByteArrayInputStream(secondStream.toByteArray()));

    // On the third request, simulate closing the connections with blaze by throwing an IOException.
    final InputStream throwingStream = mock(InputStream.class);
    when(throwingStream.read()).thenThrow(new IOException());
    streams.add(throwingStream);

    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
    OutputStream outputStream = new PrintStream(newOut, true);
    BaseWorker worker =
        new WorkerForTest() {
          int currentCompile = 0;

          @Override
          protected InputStream getInput() {
            return streams.get(currentCompile++);
          }

          @Override
          protected OutputStream getOutput() {
            return outputStream;
          }
        };

    String[] args = {"--persistent_worker"};
    worker.start(args);

    ByteArrayInputStream outAsInputStream = new ByteArrayInputStream(newOut.toByteArray());

    WorkResponse firstResponse = WorkResponse.parseDelimitedFrom(outAsInputStream);
    assertThat(firstResponse.getOutput()).isEqualTo("-d\nFirst.java\n");
    assertThat(firstResponse.getExitCode()).isEqualTo(0);

    WorkResponse secondResponse = WorkResponse.parseDelimitedFrom(outAsInputStream);
    assertThat(secondResponse.getOutput()).isEqualTo("-d\nSecond.java\n");
    assertThat(secondResponse.getExitCode()).isEqualTo(0);

    String[] lastExpectedArgs = {"-d", "Second.java"};
    assertThat(workerArgs).isEqualTo(lastExpectedArgs);
  }
}
