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
package com.google.j2cl.errors;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.common.J2clUtils;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** An error logger class that records the number of errors and provides error print methods. */
public class Problems {
  /** Represents compiler problem categories. */
  public enum Messages {
    ERR_INVALID_FLAG("Invalid flag"),
    ERR_FLAG_FILE("Cannot load flag file"),
    ERR_FILE_NOT_FOUND("File not found"),
    ERR_INVALID_SOURCE_FILE("Invalid source file"),
    ERR_INVALID_SOURCE_VERSION("Invalid source version"),
    ERR_UNSUPPORTED_ENCODING("Unsupported encoding"),
    ERR_CANNOT_GENERATE_OUTPUT("Cannot generate output, please see Velocity runtime log"),
    ERR_CANNOT_FIND_UNIT("Cannot find CompilationUnit for type "),
    ERR_OUTPUT_LOCATION("-output location must be a directory or .zip file"),
    ERR_CANNOT_EXTRACT_ZIP("Cannot extract zip"),
    ERR_CANNOT_OPEN_ZIP("Cannot open zip"),
    ERR_CANNOT_CLOSE_ZIP("Cannot close zip"),
    ERR_NATIVE_JAVA_SOURCE_NO_MATCH("Cannot find matching native file"),
    ERR_NATIVE_UNUSED_NATIVE_SOURCE("Native JavaScript file not used"),
    ERR_CANNOT_CREATE_TEMP_DIR("Cannot create temporary directory"),
    ERR_CANNOT_OPEN_FILE("Cannot open file"),
    ERR_JSINTEROP_RESTRICTIONS_ERROR("JsInterop error"),
    ERR_AMBIGUOUS_NATIVE_FILE_MATCH("Native JavaScript file matched multiple srcs"),
    ERR_PACKAGE_INFO_PARSE("Resource was found but it failed to parse"),
    ERR_CLASS_PATH_URL("Class path entry is not a valid url"),
    ERR_ERROR("Error"),
    ;

    // used for customized message.
    private final String message;

    Messages(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  /** Represents the severity of the problem */
  public enum Severity {
    ERROR,
    WARNING,
    INFO
  }

  private boolean abortRequested = false;
  private final Multimap<Severity, String> problemsBySeverity = LinkedHashMultimap.create();

  public int problemCount() {
    return problemsBySeverity.size();
  }

  public void error(Messages messages) {
    abortWhenPossible();
    problemsBySeverity.put(Severity.ERROR, messages.getMessage());
  }

  public void error(Messages messages, String detailMessage, Object... args) {
    abortWhenPossible();
    problemsBySeverity.put(
        Severity.ERROR, messages.getMessage() + ": " + J2clUtils.format(detailMessage, args));
  }

  public void warning(String detailMessage, Object... args) {
    problemsBySeverity.put(Severity.WARNING, J2clUtils.format(detailMessage, args));
  }

  public void info(String detailMessage, Object... args) {
    problemsBySeverity.put(Severity.INFO, J2clUtils.format(detailMessage, args));
  }

  public void abortWhenPossible() {
    abortRequested = true;
  }
  /** Prints all error messages and a summary. */
  public void report(PrintStream outputStream, PrintStream errorStream) {
    for (Map.Entry<Severity, String> severityMessagePair : problemsBySeverity.entries()) {
      if (severityMessagePair.getKey() == Severity.INFO) {
        outputStream.println(severityMessagePair.getValue());
      } else {
        errorStream.println(severityMessagePair.getValue());
      }
    }
    if (hasErrors() || hasWarnings()) {
      J2clUtils.printf(
          errorStream,
          "%d error(s), %d warning(s).\n",
          problemsBySeverity.get(Severity.ERROR).size(),
          problemsBySeverity.get(Severity.WARNING).size());
    }
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

  /** If there were errors abort. */
  public void abortIfRequested() {
    if (abortRequested) {
      throw new Exit(problemsBySeverity.get(Severity.ERROR).size());
    }
  }

  public List<String> getErrors() {
    return getProblems(Collections.singleton(Severity.ERROR));
  }

  public List<String> getWarnings() {
    return getProblems(Collections.singleton(Severity.WARNING));
  }

  public List<String> getInfoMessages() {
    return getProblems(Collections.singleton(Severity.INFO));
  }

  public List<String> getProblems(Collection<Severity> severities) {
    return problemsBySeverity
        .entries()
        .stream()
        .filter(e -> severities.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }


  /**
   * J2clExit is thrown to signal that a System.exit should be performed at a higher level.
   *
   * <p>Note: It should never be caught except on the top level.
   */
  public static class Exit extends java.lang.Error {
    private final int exitCode;

    public Exit(int exitCode) {
      this.exitCode = exitCode;
    }

    public int getExitCode() {
      return exitCode;
    }
  }
}
