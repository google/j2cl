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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.junit.integration.IntegrationTestBase.TestMode;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
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
          "at sun.reflect.",
          "at java.lang.reflect.",
          "at org.junit.",
          "at com.google.testing.",
          "at java.base/");

  private static final ImmutableList<String> JS_TEST_INFRA_FRAMES =
      ImmutableList.of(
          "new Promise (<anonymous>)",
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

  void matches(List<String> expectedStacktrace) {
    ImmutableList<String> actualStacktrace =
        extractStackTrace(consoleLogs, "Exception: __the_message__!");

    Deque<String> expectedLinesQueue = new ArrayDeque<>(expectedStacktrace);
    Deque<String> actualLinesQueue = new ArrayDeque<>(actualStacktrace);

    while (!expectedLinesQueue.isEmpty()) {
      String expectedLine = expectedLinesQueue.pop();

      if (isOptionalLine(expectedLine)) {
        handleOptionalLine(
            expectedLinesQueue, actualLinesQueue, expectedStacktrace, actualStacktrace);
        continue;
      }

      // just compare the two lines
      if (actualLinesQueue.isEmpty()) {
        fail(expectedStacktrace, actualStacktrace);
      }

      String actualLine = actualLinesQueue.pop();
      if (!actualLine.equals(expectedLine)) {
        fail(expectedStacktrace, actualStacktrace);
      }
    }

    if (!actualLinesQueue.isEmpty()) {
      fail(expectedStacktrace, actualStacktrace);
    }
  }

  private static void handleOptionalLine(
      Deque<String> expectedStack,
      Deque<String> actualStack,
      List<String> expectedStacktrace,
      List<String> actualStacktrace) {

    if (expectedStack.isEmpty()) {
      if (actualStack.isEmpty()) {
        // no more lines we are done
        return;
      }

      // pop one line of the actual
      actualStack.pop();
      if (actualStack.isEmpty()) {
        // no more lines we are done
        return;
      }
      // still lines on actual but no more on the expected
      fail(expectedStacktrace, actualStacktrace);
    }

    // we might need to skip a line in the actual lines

    // Lets see if there are more optional lines
    int optionalCount = countOptionals(expectedStack) + 1;

    // Start skipping lines until we find the next expected line or run out of optional lines
    for (int i = 0; i < optionalCount; i++) {
      if (actualStack.isEmpty()) {
        if (expectedStack.isEmpty()) {
          // we are good
          break;
        }
        fail(expectedStacktrace, actualStacktrace);
      }

      if (expectedStack.isEmpty()) {
        actualStack.pop();
        continue;
      }

      String actualLine = actualStack.peek();
      String nextRealLine = expectedStack.peek();
      if (actualLine.equals(nextRealLine)) {
        // we are good
        break;
      } else {
        actualStack.pop();
        continue;
      }
    }
  }

  private static void fail(
      Collection<String> expectedStacktrace, Collection<String> actualStackTrace) {
    StringBuilder builder = new StringBuilder();

    builder.append("Stacktraces do not match\n");
    builder.append("Expected stacktrace:\n");
    Joiner.on("\n").appendTo(builder, expectedStacktrace).append("\n\n");

    builder.append("Actual stacktrace:\n");
    Joiner.on("\n").appendTo(builder, actualStackTrace).append("\n\n");

    throw new AssertionError(builder.toString());
  }

  private static int countOptionals(Deque<String> expectedLines) {
    if (expectedLines.isEmpty()) {
      return 0;
    }

    int optionalCount = 0;

    while (!expectedLines.isEmpty()) {
      if (isOptionalLine(expectedLines.peek())) {
        optionalCount++;
        expectedLines.pop();
        continue;
      }
      break;
    }
    return optionalCount;
  }

  private ImmutableList<String> extractStackTrace(List<String> logLines, String startLine) {
    // if running with j2cl there are quite a few extra stack traces in our very verbose log
    // (they are not part of a normal j2cl_test log)
    // Make sure we skip those here
    int logIndex = 0;
    if (testMode.isWeb()) {
      for (; logIndex < logLines.size(); logIndex++) {
        if (logLines
            .get(logIndex)
            .startsWith("com.google.testing.javascript.runner.core.JavaScriptFailure:")) {
          break;
        }
      }
    }

    boolean foundStart = false;
    int frameStart = -1;
    ImmutableList.Builder<String> stacktraceBuilder = ImmutableList.builder();

    // find the error start
    for (int i = logIndex; i < logLines.size(); i++) {
      String line = logLines.get(i).trim();

      if (!foundStart) {
        if (line.contains(startLine)) {
          foundStart = true;
          // First line found is the exception message. Nothing to clean up here.
          stacktraceBuilder.add(line);
        }
        continue;
      }

      if ("FAILURES!!!".equals(line)) {
        // The immediate line after the stack is FAILURES!!!. We can stop reading.
        break;
      }

      boolean skipLine = false;

      if (testMode.isJ2cl()) {
        if (frameStart < 0 && line.startsWith("at ")) {
          frameStart = i;
        }

        if (i < frameStart + EXTRA_J2CL_FRAME_COUNT) {
          // J2CL introduces an extra frame in the stack due to the way it instantiates the
          // exception. Skip that frame. We don't use an optional frame because we want to be sure
          // that no more than EXTRA_J2CL_FRAME_COUNT frames are inserted by j2cl.
          skipLine = EXTRA_J2CL_FRAMES.stream().anyMatch(line::matches);
        } else {
          skipLine = JS_TEST_INFRA_FRAMES.stream().anyMatch(line::contains);
        }
      } else {
        skipLine = JAVA_TEST_INFRA_FRAMES.stream().anyMatch(line::contains);
      }

      if (!skipLine) {
        stacktraceBuilder.add(line.replaceAll("blaze-out/.*/bin", "<blaze-out>"));
      }
    }

    return stacktraceBuilder.build();
  }

  private static boolean isOptionalLine(String line) {
    return "__OPTIONAL__".equals(line);
  }
}
