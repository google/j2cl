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

import com.google.j2cl.common.Problems.FatalError;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.function.Consumer;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2CL with J2CL.
 */
public class J2clUtils {

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

  /** Adapts a method that outputs to a stream to directly return the output as a String. */
  public static String streamToString(Consumer<? super PrintStream> streamOutputer) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    streamOutputer.accept(new PrintStream(outputStream));
    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
  }

  public static void writeToFile(Path outputPath, String content, Problems problems) {
    try {
      createDirectories(outputPath.getParent());
      Files.write(outputPath, Collections.singleton(content), StandardCharsets.UTF_8);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      maybeResetAllTimeStamps(outputPath);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  public static void writeToFile(Path outputPath, byte[] content, Problems problems) {
    try {
      createDirectories(outputPath.getParent());
      Files.write(outputPath, content);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      maybeResetAllTimeStamps(outputPath);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  public static void copyFile(Path from, Path to, Problems problems) {
    try {
      createDirectories(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      maybeResetAllTimeStamps(to);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_COPY_FILE, e.toString());
    }
  }

  private static final boolean DETERMINISTIC_TIMESTAMPS =
      Boolean.getBoolean("j2cl.deterministicTimestamps");

  private static void createDirectories(Path outputPath) throws IOException {
    if (!DETERMINISTIC_TIMESTAMPS) {
      Files.createDirectories(outputPath);
      return;
    }
    // We are creating directories one by one so that we can reset the timestamp for each one.
    if (outputPath == null || Files.exists(outputPath)) {
      return;
    }
    createDirectories(outputPath.getParent());
    Files.createDirectory(outputPath);
    maybeResetAllTimeStamps(outputPath);
  }

  private static void maybeResetAllTimeStamps(Path path) throws IOException {
    if (!DETERMINISTIC_TIMESTAMPS) {
      return;
    }
    Files.getFileAttributeView(path, BasicFileAttributeView.class)
        .setTimes(FileTime.fromMillis(0), FileTime.fromMillis(0), FileTime.fromMillis(0));
  }
}
