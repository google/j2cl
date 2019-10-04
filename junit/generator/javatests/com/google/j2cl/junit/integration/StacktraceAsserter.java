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
package com.google.j2cl.junit.integration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.junit.integration.IntegrationTestBase.TestMode;
import com.google.j2cl.junit.integration.Stacktrace.Builder;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/** Helper class for comparing stack traces */
class StacktraceAsserter {

  private static final int EXTRA_J2CL_FRAME_COUNT = 1;

  private static final ImmutableList<String> EXTRA_J2CL_FRAMES =
      ImmutableList.of(
          "at java.lang.Throwable.fillInStackTrace.*",
          "at java.lang.RuntimeException.*",
          "at .*\\$MyJsException.*");

  private static final ImmutableList<String> JAVA_TEST_INFRA_FRAMES =
      ImmutableList.of(
          "at sun.reflect.", "at java.lang.reflect.", "at org.junit.", "at com.google.testing.");

  private static final ImmutableList<String> JS_TEST_INFRA_FRAMES =
      ImmutableList.of(
          "javascript/closure/testing/testcase.js",
          "javascript/closure/testing/testrunner.js",
          "javascript/closure/promise/promise.js",
          "javascript/closure/testing/jsunit.js");

  private final TestMode testMode;
  private final List<String> consoleLogs;

  StacktraceAsserter(TestMode testMode, List<String> consoleLogs) {
    this.testMode = testMode;
    this.consoleLogs = consoleLogs;
  }

  void matches(Stacktrace expectedStacktrace) {
    List<String> stacktrace = extractStackTrace(consoleLogs, "Exception: __the_message__!");
    Stacktrace actualStacktrace = parseStackTrace(stacktrace);

    assertThat(actualStacktrace.message()).isEqualTo(expectedStacktrace.message());

    Deque<String> expectedFrames = new LinkedList<>(expectedStacktrace.frames());
    Deque<String> actualFrames = new LinkedList<>(actualStacktrace.frames());

    while (!expectedFrames.isEmpty()) {
      String expectedFrame = expectedFrames.pop();
      if (Stacktrace.isOptionalFrame(expectedFrame)) {
        handleOptionalFrame(expectedFrames, actualFrames, expectedStacktrace, actualStacktrace);
        continue;
      }

      // just compare the two frames
      if (actualFrames.isEmpty()) {
        fail(expectedStacktrace, actualStacktrace);
      }

      String actualFrame = actualFrames.pop();
      if (!actualFrame.equals(expectedFrame)) {
        fail(expectedStacktrace, actualStacktrace);
      }
    }

    if (!actualFrames.isEmpty()) {
      fail(expectedStacktrace, actualStacktrace);
    }
  }

  private void handleOptionalFrame(
      Deque<String> expectedFrames,
      Deque<String> actualFrames,
      Stacktrace expectedStacktrace,
      Stacktrace actualStacktrace) {
    if (expectedFrames.isEmpty()) {
      if (actualFrames.isEmpty()) {
        // no more frames we are done
        return;
      }

      // pop one frame of the actual
      actualFrames.pop();
      if (actualFrames.isEmpty()) {
        // no more frames we are done
        return;
      }
      // still frames on actual but no more on the expected
      fail(expectedStacktrace, actualStacktrace);
    }

    // we might need to skip a frame in the actual frames

    // Lets see if there are more optional frames
    int optionalCount = countOptionals(expectedFrames) + 1;

    // Start skipping frames until we find the next expected frame or run out of optional frames
    for (int i = 0; i < optionalCount; i++) {
      if (actualFrames.isEmpty()) {
        if (expectedFrames.isEmpty()) {
          // we are good
          break;
        }
        fail(expectedStacktrace, actualStacktrace);
      }

      if (expectedFrames.isEmpty()) {
        actualFrames.pop();
        continue;
      }

      String actualFrame = actualFrames.peek();
      String nextRealFrame = expectedFrames.peek();
      if (actualFrame.equals(nextRealFrame)) {
        // we are good
        break;
      } else {
        actualFrames.pop();
        continue;
      }
    }
  }

  private static void fail(Stacktrace expectedStacktrace, Stacktrace actualStackTrace) {
    StringBuilder builder = new StringBuilder();

    builder.append("Stacktraces do not match\n");
    builder.append("Expected stacktrace:\n");
    builder.append(expectedStacktrace.message()).append("\n");
    expectedStacktrace.frames().forEach((f) -> builder.append(f).append("\n"));
    builder.append("\n");

    builder.append("Actual stacktrace:\n");
    builder.append(actualStackTrace.message()).append("\n");
    actualStackTrace.frames().forEach((f) -> builder.append(f).append("\n"));
    builder.append("\n");

    throw new AssertionError(builder.toString());
  }

  private int countOptionals(Deque<String> expectedFrames) {
    if (expectedFrames.isEmpty()) {
      return 0;
    }

    int optionalCount = 0;

    while (!expectedFrames.isEmpty()) {
      if (Stacktrace.isOptionalFrame(expectedFrames.peek())) {
        optionalCount++;
        expectedFrames.pop();
        continue;
      }
      break;
    }
    return optionalCount;
  }

  private Stacktrace parseStackTrace(List<String> stacktrace) {
    // The first line is different from the rest, it is the message.
    checkArgument(!stacktrace.isEmpty());
    Builder stacktraceBuilder = Stacktrace.newStacktraceBuilder();

    String message = stacktrace.get(0).trim();
    int frameStart;
    for (frameStart = 1; frameStart < stacktrace.size(); frameStart++) {
      String line = stacktrace.get(frameStart).trim();
      if (!line.startsWith("at ")) {
        message += "\n" + line;
      } else {
        break;
      }
    }
    stacktraceBuilder.message(message);

    for (int i = frameStart; i < stacktrace.size(); i++) {
      final String line = stacktrace.get(i).trim();

      boolean skip = false;
      if (testMode.isJ2cl()) {
        if (i < frameStart + EXTRA_J2CL_FRAME_COUNT) {
          skip |= EXTRA_J2CL_FRAMES.stream().anyMatch(s -> line.matches(s));
        }
        skip |= JS_TEST_INFRA_FRAMES.stream().anyMatch(s -> line.contains(s));
      } else {
        skip |= JAVA_TEST_INFRA_FRAMES.stream().anyMatch(s -> line.contains(s));
      }

      if (!skip) {
        stacktraceBuilder.addFrame(line);
      }
    }

    return stacktraceBuilder.build();
  }

  private List<String> extractStackTrace(List<String> logLines, String startLine) {
    // if running with j2cl there are quite a few extra stack traces in our very verbose log
    // (they are not part of a normal j2cl_test log)
    // Make sure we skip those here
    int logIndex = 0;
    if (testMode.isJ2cl()) {
      for (; logIndex < logLines.size(); logIndex++) {
        if (logLines
            .get(logIndex)
            .startsWith("com.google.testing.javascript.runner.core.JavaScriptFailure:")) {
          break;
        }
      }
    }

    boolean foundStart = false;
    List<String> stacklines = new ArrayList<>();
    // find the error start
    for (int i = logIndex; i < logLines.size(); i++) {
      String line = logLines.get(i);

      if (foundStart) {
        // lines after the stack do not have tabs or spaces
        if (!line.startsWith(" ") && !line.startsWith("\t")) {
          break;
        }
        stacklines.add(line.trim());
      }

      if (line.contains(startLine)) {
        foundStart = true;
        stacklines.add(line.trim());
      }
    }

    return stacklines;
  }
}
