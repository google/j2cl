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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.j2cl.junit.integration.IntegrationTestBase.TestMode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StacktraceAsserterTest {

  private static class LogLineBuilder {
    private final ImmutableList.Builder<String> builder = new Builder<>();

    public static LogLineBuilder builder() {
      return new LogLineBuilder();
    }

    public LogLineBuilder addLine(String line) {
      builder.add(line);
      return this;
    }

    public LogLineBuilder addTrace(String line) {
      builder.add("\tat " + line);
      return this;
    }

    public ImmutableList<String> build() {
      return builder.build();
    }
  }

  @Test
  public void testSimpleStacktrace() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of("java.lang.RuntimeException: __the_message__!", "at A", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testSimpleStacktrace_notWorking() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of("java.lang.RuntimeException: __the_message__!", "at A", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("D")
                .build());

    Assert.assertThrows(AssertionError.class, () -> stacktraceAsserter.matches(expectedTrace));
  }

  @Test
  public void testSimpleOptionalFrame_notPresent() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!", "at A", "__OPTIONAL__", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testSimpleOptionalFrame_present() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!", "at A", "__OPTIONAL__", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("covered_by_optional")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testSimpleOptionalFrame_twoFramesNotWorking() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!", "at A", "__OPTIONAL__", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("covered_by_optional")
                .addTrace("not_covered_by_optional")
                .addTrace("B")
                .addTrace("C")
                .build());

    Assert.assertThrows(AssertionError.class, () -> stacktraceAsserter.matches(expectedTrace));
  }

  @Test
  public void testMultipleOptionalFrames_threeFramesTwoUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testMultipleOptionalFrames_threeFramesThreeUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("covered_by_optional2")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testMultipleOptionalFrames_threeFramesFourUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("covered_by_optional2")
                .addTrace("not_covered_by_optional")
                .addTrace("B")
                .addTrace("C")
                .build());

    Assert.assertThrows(AssertionError.class, () -> stacktraceAsserter.matches(expectedTrace));
  }

  @Test
  public void testOptionalAtStart_noneUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtStart_oneUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("covered_by_optional")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtStart_twoUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtStart_threeUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "__OPTIONAL__",
            "__OPTIONAL__",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("not_covered_by_optional")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    Assert.assertThrows(AssertionError.class, () -> stacktraceAsserter.matches(expectedTrace));
  }

  @Test
  public void testOptionalAtEnd_noneUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "at B",
            "at C",
            "__OPTIONAL__",
            "__OPTIONAL__");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtEnd_oneUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "at B",
            "at C",
            "__OPTIONAL__",
            "__OPTIONAL__");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addTrace("covered_by_optional")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtEnd_twoUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "at B",
            "at C",
            "__OPTIONAL__",
            "__OPTIONAL__");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testOptionalAtEnd_threeUsed() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "at B",
            "at C",
            "__OPTIONAL__",
            "__OPTIONAL__");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addTrace("covered_by_optional")
                .addTrace("covered_by_optional1")
                .addTrace("not_covered_by_optional")
                .build());

    Assert.assertThrows(AssertionError.class, () -> stacktraceAsserter.matches(expectedTrace));
  }

  @Test
  public void testSkipFrameJ2CLMode_firstFrameOnlySkipped() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "and submessage",
            "at java.lang.RuntimeException.notskipped",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.J2CL_UNCOMPILED,
            LogLineBuilder.builder()
                .addLine("com.google.testing.javascript.runner.core.JavaScriptFailure:")
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addLine("and submessage")
                .addTrace("java.lang.RuntimeException.skipped")
                .addTrace("java.lang.RuntimeException.notskipped")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testSkipFrameJ2CLMode_jsTestInfraFramesSkipped() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of("java.lang.RuntimeException: __the_message__!", "at A", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.J2CL_UNCOMPILED,
            LogLineBuilder.builder()
                .addLine("com.google.testing.javascript.runner.core.JavaScriptFailure:")
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addTrace("javascript/closure/testing/testcase.js")
                .addTrace("javascript/closure/testing/testrunner.js")
                .addTrace("javascript/closure/promise/promise.js")
                .addTrace("javascript/closure/testing/jsunit.js")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testSkipFrameJavaMode_javaTestInfraFramesSkipped() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at java.lang.RuntimeException.notskipped",
            "at A",
            "at B",
            "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("com.google.testing.javascript.runner.core.JavaScriptFailure:")
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("java.lang.RuntimeException.notskipped")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addTrace("sun.reflect.skipped")
                .addTrace("java.lang.reflect.skipped")
                .addTrace("org.junit.skipped")
                .addTrace("com.google.testing.skipped")
                .addTrace("java.base/java.skipped")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testStacktraceExtraction() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of("java.lang.RuntimeException: __the_message__!", "at A", "at B", "at C");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("other log line 1")
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addLine("FAILURES!!!")
                .addLine("other log line 2")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }

  @Test
  public void testStacktraceWithCausedByChain() {
    ImmutableList<String> expectedTrace =
        ImmutableList.of(
            "java.lang.RuntimeException: __the_message__!",
            "at A",
            "at B",
            "__OPTIONAL__",
            "Caused by: java.lang.RuntimeException: cause 1",
            "at D",
            "at E",
            "__OPTIONAL__",
            "Caused by: java.lang.RuntimeException: cause 2",
            "at G",
            "at H",
            "... 3 mores");

    StacktraceAsserter stacktraceAsserter =
        new StacktraceAsserter(
            TestMode.JAVA,
            LogLineBuilder.builder()
                .addLine("java.lang.RuntimeException: __the_message__!")
                .addTrace("A")
                .addTrace("B")
                .addTrace("C")
                .addLine("Caused by: java.lang.RuntimeException: cause 1")
                .addTrace("D")
                .addTrace("E")
                .addTrace("F")
                .addLine("Caused by: java.lang.RuntimeException: cause 2")
                .addTrace("G")
                .addTrace("H")
                .addLine("... 3 mores")
                .build());

    stacktraceAsserter.matches(expectedTrace);
  }
}
