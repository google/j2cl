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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import com.google.j2cl.errors.Errors;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link J2clTranspilerWorker}. */
@RunWith(JUnit4.class)
public class J2clTranspilerWorkerTest {

  enum CompilerResponse {
    COMPILE_SUCCEEDED,
    COMPILE_FAILED,
    COMPILER_CRASHES
  }

  private static class StubCompiler extends J2clTranspiler {
    Long threadId;
    final String[] args;
    private CompilerResponse compilerResponse;

    StubCompiler(String[] args, CompilerResponse compilerResponse) {
      super(args);
      this.args = args;
      this.compilerResponse = compilerResponse;
    }

    @Override
    void run() {
      // Make sure we do not do an actual compile.
      Assert.assertNull(threadId);
      threadId = Thread.currentThread().getId();

      switch (compilerResponse) {
        case COMPILE_SUCCEEDED:
          System.out.println("compiler_success");
          break;
        case COMPILE_FAILED:
          System.err.println("compiler_failed");
          throw new Errors.Exit(-3);
        case COMPILER_CRASHES:
          throw new RuntimeException();
      }
    }
  }

  private class WorkerForTest extends J2clTranspilerWorker {

    private List<InputStream> inputStreams;
    private int currentStream = 0;
    private int currentResponse = 0;
    private CompilerResponse[] compilerResponses;
    private Integer exitCode;

    public WorkerForTest(List<InputStream> inputStreams, CompilerResponse... compilerResponses) {
      this.inputStreams = inputStreams;
      this.compilerResponses = compilerResponses;
    }

    List<StubCompiler> compilers = new ArrayList<>();

    @Override
    J2clTranspiler createTranspiler(String[] args) {
      StubCompiler stubCompiler = new StubCompiler(args, compilerResponses[currentResponse++]);
      compilers.add(stubCompiler);
      return stubCompiler;
    }

    @Override
    InputStream getInput() {
      return inputStreams.get(currentStream++);
    }

    @Override
    void exit(int code) {
      Assert.assertNull(exitCode);
      exitCode = code;
    }
  }

  @Test
  public void testWorker() throws IOException {

    WorkRequest firstRequest =
        WorkRequest.newBuilder().addArguments("-d").addArguments("First.java").build();
    WorkRequest secondRequest =
        WorkRequest.newBuilder().addArguments("-d").addArguments("Second.java").build();

    List<InputStream> inputStreams = createInputStreams(firstRequest, secondRequest);

    InputStream throwingInputStream = mock(InputStream.class);
    when(throwingInputStream.read()).thenThrow(new IOException());
    inputStreams.add(throwingInputStream);

    WorkerForTest worker =
        new WorkerForTest(
            inputStreams, CompilerResponse.COMPILE_SUCCEEDED, CompilerResponse.COMPILE_FAILED);

    // hijack System.out, workers need to output protos to stdout
    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(newOut, true));

    worker.run();

    assertThat(worker.compilers).hasSize(2);

    // Make sure every compile is invoked on a different thread
    long currentThreadId = Thread.currentThread().getId();
    assertThat(worker.compilers.get(0).threadId).isNotEqualTo(currentThreadId);
    assertThat(worker.compilers.get(1).threadId).isNotEqualTo(currentThreadId);
    assertThat(worker.compilers.get(0).threadId).isNotEqualTo(worker.compilers.get(1).threadId);

    // Did arguemnts arrive properly?
    assertThat(worker.compilers.get(0).args).asList().containsExactly("-d", "First.java");
    assertThat(worker.compilers.get(1).args).asList().containsExactly("-d", "Second.java");
    // verify that out contains the two work things

    ByteArrayInputStream outasInputStream = new ByteArrayInputStream(newOut.toByteArray());

    WorkResponse firstResponse = WorkResponse.parseDelimitedFrom(outasInputStream);
    assertThat(firstResponse.getOutput()).isEqualTo("compiler_success\n");
    assertThat(firstResponse.getExitCode()).isEqualTo(0);

    WorkResponse secondResponse = WorkResponse.parseDelimitedFrom(outasInputStream);
    assertThat(secondResponse.getOutput()).isEqualTo("compiler_failed\n");
    assertThat(secondResponse.getExitCode()).isEqualTo(1);
  }

  @Test
  public void testWorkerQuitsIfCompilerFails() throws IOException {
    WorkRequest firstRequest =
        WorkRequest.newBuilder().addArguments("-d").addArguments("First.java").build();
    List<InputStream> inputStreams = createInputStreams(firstRequest);
    InputStream throwingInputStream = mock(InputStream.class);
    when(throwingInputStream.read()).thenThrow(new IOException());
    inputStreams.add(throwingInputStream);

    WorkerForTest worker = new WorkerForTest(inputStreams, CompilerResponse.COMPILER_CRASHES);

    worker.run();

    assertThat(worker.exitCode).isEqualTo(-2);
  }

  private List<InputStream> createInputStreams(WorkRequest... request) throws IOException {
    List<InputStream> list = new ArrayList<>();
    for (WorkRequest workRequest : request) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workRequest.writeDelimitedTo(outputStream);
      list.add(new ByteArrayInputStream(outputStream.toByteArray()));
    }
    return list;
  }
}
