/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.common;

import java.io.PrintStream;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2cl with J2cl.
 */
public class J2clUtils {

  public static final String FILEPATH_SEPARATOR = "/";
  public static final char FILEPATH_SEPARATOR_CHAR = '/';

  /**
   * J2cl's implementation of String.format(format, args). Returns only the format string.
   */
  public static String format(String format, Object... args) {
    // TODO(epmjohnston): This is only a temporary placeholder; should be properly implemented.
    return format;
  }

  /**
   * J2cl's implementation of PrintStream.printf(format, args). (Note that the method signature
   * differs from PrintStream.printf). Prints only the format string to the given PrintStream.
   */
  public static PrintStream printf(PrintStream stream, String format, Object... args) {
    // TODO(epmjohnston): This is only a temporary placeholder; should be properly implemented.
    stream.print(format);
    return stream;
  }

  /** Placeholder for escapeJavaString. Returns string as-is. */
  public static String escapeJavaString(String string) {
    // TODO(epmjohnston): This is only a temporary placeholder; should be properly implemented.
    return string;
  }
  
  /** Placeholder for decapitalize. Returns string as-is. */
  public static String decapitalize(String substring) {
    // TODO(epmjohnston): This is only a temporary placeholder; should be properly implemented.
    return substring;
  }
}
