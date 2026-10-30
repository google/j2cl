/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.junit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Creates and extracts the generated JavaScript consumed by J2CL tests. */
public final class J2clTestSuiteTool {
  private static final LocalDateTime ZIP_TIMESTAMP = LocalDateTime.of(2010, 1, 1, 0, 0);

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      throw new IllegalArgumentException("Expected operation, input, and output arguments");
    }

    Path input = Path.of(args[1]);
    Path output = Path.of(args[2]);
    switch (args[0]) {
      case "archive" -> createArchive(input, output);
      case "extract" -> extractSingleJavaScript(input, output);
      default -> throw new IllegalArgumentException("Unknown operation: " + args[0]);
    }
  }

  static void createArchive(Path input, Path output) throws IOException {
    try (ZipFile inputZip = new ZipFile(input.toFile())) {
      List<? extends ZipEntry> entries =
          inputZip.stream()
              .filter(entry -> !entry.isDirectory())
              .filter(
                  entry ->
                      entry.getName().endsWith(".testsuite")
                          || entry.getName().endsWith(".json"))
              .sorted(Comparator.comparing(entry -> outputName(entry.getName())))
              .toList();
      if (entries.isEmpty()) {
        throw new IOException("J2CL test generation produced no test suite artifacts");
      }

      try (ZipOutputStream outputZip = new ZipOutputStream(Files.newOutputStream(output))) {
        for (ZipEntry entry : entries) {
          byte[] content;
          try (InputStream inputStream = inputZip.getInputStream(entry)) {
            content = inputStream.readAllBytes();
          }
          writeEntry(outputZip, outputName(entry.getName()), content);
        }
      }
    }
  }

  static void extractSingleJavaScript(Path input, Path output) throws IOException {
    try (ZipFile inputZip = new ZipFile(input.toFile())) {
      List<? extends ZipEntry> entries =
          inputZip.stream()
              .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".js"))
              .toList();
      if (entries.size() != 1) {
        throw new IOException(
            "Expected exactly one generated JavaScript test suite, found " + entries.size());
      }
      try (InputStream inputStream = inputZip.getInputStream(entries.get(0))) {
        Files.copy(inputStream, output);
      }
    }
  }

  private static String outputName(String name) {
    return name.endsWith(".testsuite")
        ? name.substring(0, name.length() - ".testsuite".length()) + ".js"
        : name;
  }

  private static void writeEntry(ZipOutputStream zip, String name, byte[] content)
      throws IOException {
    CRC32 crc = new CRC32();
    crc.update(content);
    ZipEntry entry = new ZipEntry(name);
    entry.setMethod(ZipEntry.STORED);
    entry.setSize(content.length);
    entry.setCompressedSize(content.length);
    entry.setCrc(crc.getValue());
    entry.setTimeLocal(ZIP_TIMESTAMP);
    zip.putNextEntry(entry);
    zip.write(content);
    zip.closeEntry();
  }

  private J2clTestSuiteTool() {}
}
