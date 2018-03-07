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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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

    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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

    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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

    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addOptionalFrame()
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addOptionalFrame()
            .addOptionalFrame()
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .addOptionalFrame()
            .addOptionalFrame()
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .addOptionalFrame()
            .addOptionalFrame()
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .addOptionalFrame()
            .addOptionalFrame()
            .build();

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
    Stacktrace expectedTrace =
        Stacktrace.newStacktraceBuilder()
            .message("java.lang.RuntimeException: __the_message__!")
            .addFrame("at A")
            .addFrame("at B")
            .addFrame("at C")
            .addOptionalFrame()
            .addOptionalFrame()
            .build();

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
}
