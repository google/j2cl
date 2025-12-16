/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.getBoolean;

import com.google.common.base.Throwables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** An error logger class that records the number of errors and provides error print methods. */
public class Problems {
  private static final boolean REPORT_DEBUG =
      getBoolean("com.google.j2cl.common.Problems.reportDebug");

  /** Represents compiler fatal errors. */
  public enum FatalError {
    FILE_NOT_FOUND("File '%s' not found.", 1),
    UNKNOWN_INPUT_TYPE("Cannot recognize input type for file '%s'.", 1),
    OUTPUT_LOCATION("Output location '%s' must be a directory or .zip file.", 1),
    CANNOT_EXTRACT_ZIP("Cannot extract zip '%s': %s.", 2),
    CANNOT_CREATE_ZIP("Cannot create zip '%s': %s.", 2),
    CANNOT_CLOSE_ZIP("Cannot close zip: %s.", 1),
    CANNOT_CREATE_TEMP_DIR("Cannot create temporary directory: %s.", 1),
    CANNOT_OPEN_FILE("Cannot open file: %s.", 1),
    CANNOT_WRITE_FILE("Cannot write file: %s.", 1),
    CANNOT_COPY_FILE("Cannot copy file: %s.", 1),
    PACKAGE_INFO_PARSE("Resource '%s' was found but it failed to parse.", 1),
    CLASS_PATH_URL("Class path entry '%s' is not a valid url.", 1),
    INCOMPATIBLE_ANNOTATION_FOUND_IN_COMPILE(
        "Unexpected @%s annotation found. "
            + "Please run this library through the incompatible annotated code stripper tool.",
        1),
    INVALID_JAVA_FRONTEND("%s is not a valid Java frontend.", 1);

    // used for customized message.
    private final String message;
    // number of arguments the message takes.
    private final int numberOfArguments;

    FatalError(String message, int numberOfArguments) {
      this.message = message;
      this.numberOfArguments = numberOfArguments;
    }

    public String getMessage() {
      return message;
    }

    private int getNumberOfArguments() {
      return numberOfArguments;
    }
  }

  /** Represents the severity of the problem */
  public enum Severity {
    ERROR("Error"),
    WARNING("Warning"),
    INFO("Info"),
    DEBUG("Debug");

    Severity(String messagePrefix) {
      this.messagePrefix = messagePrefix;
    }

    private final String messagePrefix;

    public String getMessagePrefix() {
      return messagePrefix;
    }
  }

  private final SetMultimap<Severity, String> problemsBySeverity =
      Multimaps.synchronizedSetMultimap(LinkedHashMultimap.create());

  public void fatal(FatalError fatalError, Object... args) {
    checkArgument(fatalError.getNumberOfArguments() == args.length);
    log(Severity.ERROR, "Error: " + String.format(fatalError.getMessage(), args));
    abort();
  }

  public void fatal(int lineNumber, String filePath, FatalError fatalError, Object... args) {
    checkArgument(fatalError.getNumberOfArguments() == args.length);
    log(Severity.ERROR, lineNumber, filePath, String.format(fatalError.getMessage(), args));
    abort();
  }

  @FormatMethod
  public void error(
      SourcePosition sourcePosition, @FormatString String detailMessage, Object... args) {
    log(Severity.ERROR, sourcePosition, detailMessage, args);
  }

  @FormatMethod
  public void error(
      int lineNumber, String filePath, @FormatString String detailMessage, Object... args) {
    log(Severity.ERROR, lineNumber, filePath, detailMessage, args);
  }

  @FormatMethod
  public void error(String detailMessage, Object... args) {
    log(Severity.ERROR, "Error: " + String.format(detailMessage, args));
  }

  @FormatMethod
  public void warning(SourcePosition sourcePosition, String detailMessage, Object... args) {
    log(Severity.WARNING, sourcePosition, detailMessage, args);
  }

  @FormatMethod
  public void warning(String detailMessage, Object... args) {
    log(Severity.WARNING, String.format(detailMessage, args));
  }

  @FormatMethod
  public void info(SourcePosition sourcePosition, String detailMessage, Object... args) {
    log(Severity.INFO, sourcePosition, detailMessage, args);
  }

  @FormatMethod
  public void info(String detailMessage, Object... args) {
    log(Severity.INFO, String.format(detailMessage, args));
  }

  @FormatMethod
  public void debug(SourcePosition sourcePosition, String detailMessage, Object... args) {
    log(Severity.DEBUG, sourcePosition, detailMessage, args);
  }

  @FormatMethod
  public void log(
      Severity severity, SourcePosition sourcePosition, String detailMessage, Object... args) {
    checkArgument(sourcePosition != null);
    if (sourcePosition == SourcePosition.NONE) {
      log(severity, String.format(detailMessage, args));
    } else {
      log(
          severity,
          // SourcePosition lines are 0 based.
          sourcePosition.getStartFilePosition().getLine() + 1,
          sourcePosition.getFilePath(),
          detailMessage,
          args);
    }
  }

  @FormatMethod
  private void log(
      Severity severity,
      int lineNumber,
      String filePath,
      @FormatString String detailMessage,
      Object... args) {
    String message = args.length == 0 ? detailMessage : String.format(detailMessage, args);
    log(severity, lineNumber, filePath, message);
  }

  private void log(Severity severity, int lineNumber, String filePath, String message) {
    log(
        severity,
        String.format(
            "%s:%s:%s: %s",
            severity.getMessagePrefix(),
            filePath.substring(filePath.lastIndexOf('/') + 1),
            lineNumber,
            message));
  }

  private void log(Severity severity, String message) {
    problemsBySeverity.put(severity, message);
  }

  /** Prints all problems to provided output and returns the exit code. */
  public int reportAndGetExitCode(PrintStream output) {
    return reportAndGetExitCode(new PrintWriter(output, true));
  }

  /** Prints all problems to provided output and returns the exit code. */
  public int reportAndGetExitCode(PrintWriter output) {
    for (Map.Entry<Severity, String> severityMessagePair : problemsBySeverity.entries()) {
      if (REPORT_DEBUG || severityMessagePair.getKey() != Severity.DEBUG) {
        output.println(severityMessagePair.getValue());
      }
    }
    if (hasErrors() || hasWarnings()) {
      output.printf(
          "%d error(s), %d warning(s).\n",
          problemsBySeverity.get(Severity.ERROR).size(),
          problemsBySeverity.get(Severity.WARNING).size());
    }

    return hasErrors() ? 1 : 0;
  }

  public boolean hasWarnings() {
    return problemsBySeverity.containsKey(Severity.WARNING);
  }

  public boolean hasErrors() {
    return problemsBySeverity.containsKey(Severity.ERROR);
  }

  public boolean hasProblems() {
    return !problemsBySeverity.isEmpty();
  }

  public void abortIfHasErrors() {
    if (isCancelled() || hasErrors()) {
      abort();
    }
  }

  /**
   * Alternative to abortIfHasErrors for cases where we want to keep accumulating errors but still
   * abort when cancelled.
   */
  public void abortIfCancelled() {
    if (isCancelled()) {
      abort();
    }
  }

  private volatile boolean cancelled = false;

  public boolean isCancelled() {
    return cancelled;
  }

  public void requestCancellation() {
    cancelled = true;
  }

  private void abort() {
    throw new Exit();
  }

  public List<String> getErrors() {
    return getMessages(Severity.ERROR);
  }

  public List<String> getWarnings() {
    return getMessages(Severity.WARNING);
  }

  public List<String> getInfoMessages() {
    return getMessages(Severity.INFO);
  }

  public List<String> getMessages() {
    return getMessages(EnumSet.allOf(Severity.class));
  }

  private List<String> getMessages(Severity severity) {
    return getMessages(Collections.singleton(severity));
  }

  private List<String> getMessages(Collection<Severity> severities) {
    return problemsBySeverity
        .entries()
        .stream()
        .filter(e -> severities.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  /**
   * Exit is thrown to signal that a System.exit should be performed at a higher level.
   *
   * <p>Note: It should never be caught except on the top level.
   */
  public static class Exit extends Error {
    /**
     * Returns true if Problems.Exit is the root cause of the given exception.
     *
     * <p>The exceptions might be wrapped in another exception to provide more context. However if
     * the root cause is a Problems.Exit then either the Program aborted due to errors that were
     * already recorded in problems or it was due to cancellation. Either way we should not crash
     * the worker if the root cause is a Problems.Exit.
     */
    public static boolean isRootCause(Throwable e) {
      return Throwables.getRootCause(e) instanceof Exit;
    }
  }
}
