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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An error logger class that records the number of errors and provides error print methods.
 */
public class Errors {
  public static final String ERR_INVALID_FLAG = "invalid flag";
  public static final String ERR_FILE_NOT_FOUND = "file not found";
  public static final String ERR_INVALID_SOURCE_FILE = "invalid source file";
  public static final String ERR_INVALID_SOURCE_VERSION = "invalid source version";
  public static final String ERR_UNSUPPORTED_ENCODING = "unsupported encoding";
  public static final String ERR_CANNOT_CREATE_DIRECTORY = "cannot create directory";
  public static final String ERR_CANNOT_GENERATE_OUTPUT =
      "cannot generate output, please see Velocity runtime log";

  private int errorCount = 0;
  private List<String> errorMessages = new ArrayList<>();
  private PrintStream errorStream;

  public Errors() {
    this.errorStream = System.err;
  }

  public Errors(PrintStream errorStream) {
    this.errorStream = errorStream;
  }

  public int errorCount() {
    return errorCount;
  }

  public PrintStream getErrorStream() {
    return errorStream;
  }

  public void reset() {
    this.errorCount = 0;
    this.errorMessages.clear();
  }

  public void error(String message) {
    errorCount++;
    errorMessages.add(message);
  }

  public void error(String message, String detail) {
    errorCount++;
    errorMessages.add(message + ": " + detail);
  }

  /**
   * Prints all error messages and a summary.
   */
  public void report() {
    for (String message : errorMessages) {
      errorStream.println(message);
    }
    errorStream.printf("%d error(s).%n", errorCount);
  }

  /**
   * If there were errors, prints a summary and exits.
   */
  public void maybeReportAndExit() {
    if (errorCount > 0) {
      report();
      System.exit(errorCount);
    }
  }
}
