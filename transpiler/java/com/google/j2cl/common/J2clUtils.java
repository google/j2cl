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

import java.beans.Introspector;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.function.Consumer;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2CL with J2CL.
 */
public class J2clUtils {

  public static final String FILEPATH_SEPARATOR = File.separator;
  public static final char FILEPATH_SEPARATOR_CHAR = File.separatorChar;

  /**
   * J2cl's implementation of String.format(format, args).
   * Returns a formatted string using the specified format string and arguments.
   */
  public static String format(String format, Object... args) {
    return String.format(format, args);
  }

  /**
   * J2cl's implementation of PrintStream.printf(format, args).
   *   (Note that the method signature differs from PrintStream.printf).
   * A convenience method to write a formatted string to this output stream using the specified
   *   format string and arguments.
   */
  public static PrintStream printf(PrintStream stream, String format, Object... args) {
    return stream.printf(format, args);
  }

  /** Escapes a string into a representation suitable for literals. */
  public static String escapeJavaString(String string) {
    // NOTE: StringEscapeUtils.escapeJava does not escape unprintable character 127 (delete).
    return StringEscapeUtils.escapeJava(string).replace("\u007f", "\\u007F");
  }

  /** Escapes a character into a representation suitable for literals. */
  public static String escapeJavaChar(char ch) {
    if (ch == '\'') {
      // Extra escaping needed since the single quotes is the delimiter.
      return "\\'";
    }
    return escapeJavaString(String.valueOf(ch));
  }

  /** Convert a string to normal Java variable name capitalization. */
  public static String decapitalize(String substring) {
    return Introspector.decapitalize(substring);
  }

  /** Adapts a method that outputs to a stream to directly return the output as a String. */
  public static String streamToString(Consumer<? super PrintStream> streamOutputer) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    streamOutputer.accept(new PrintStream(outputStream));
    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
  }

  public static void writeToFile(Path outputPath, String content, Problems problems) {
    try {
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      createDirectories(outputPath.getParent());
      Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      maybeResetAllTimeStamps(outputPath);
    } catch (IOException e) {
      problems.error("Could not write to file: %s", e.toString());
      problems.abortIfRequested();
    }
  }

  public static void copyFile(Path from, Path to) {
    try {
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      maybeResetAllTimeStamps(to);
    } catch (IOException e) {
      // TODO(tdeegan): This blows up during the JRE compile. Did this ever work? The sources are
      // available for compilation so no errors should be seen here unless there is an exceptional
      // condition.
      // errors.error(Errors.Error.ERR_ERROR, "Could not copy java file: "
      // + absoluteOutputPath + ":" + e.getMessage());
    }
  }

  private static void createDirectories(Path outputPath) throws IOException {
    // We are creating directories one by one so that we can reset the timestamp for each one.
    if (outputPath == null || Files.exists(outputPath)) {
      return;
    }
    createDirectories(outputPath.getParent());
    Files.createDirectory(outputPath);
    maybeResetAllTimeStamps(outputPath);
  }

  private static final boolean DETERMINISTIC_TIMESTAMPS =
      Boolean.getBoolean("j2cl.deterministicTimestamps");

  private static void maybeResetAllTimeStamps(Path path) throws IOException {
    if (!DETERMINISTIC_TIMESTAMPS) {
      return;
    }
    Files.getFileAttributeView(path, BasicFileAttributeView.class)
        .setTimes(FileTime.fromMillis(0), FileTime.fromMillis(0), FileTime.fromMillis(0));
  }
}
