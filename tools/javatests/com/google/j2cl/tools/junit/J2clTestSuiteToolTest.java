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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class J2clTestSuiteToolTest {
  private static final LocalDateTime ZIP_TIMESTAMP = LocalDateTime.of(2010, 1, 1, 0, 0);

  @Test
  public void createArchiveSelectsAndRenamesTestArtifacts() throws Exception {
    Path directory = Files.createTempDirectory("j2cl-test-suite-tool");
    Path input = directory.resolve("input.jar");
    Path output = directory.resolve("output.zip");
    writeZip(
        input,
        List.of(
            Map.entry("z/Example.testsuite", "suite"),
            Map.entry("ignored.class", "class"),
            Map.entry("test_summary.json", "summary")));

    J2clTestSuiteTool.createArchive(input, output);

    try (ZipFile zip = new ZipFile(output.toFile())) {
      List<? extends ZipEntry> entries = zip.stream().toList();
      assertEquals(List.of("test_summary.json", "z/Example.js"), entryNames(entries));
      assertEquals(ZIP_TIMESTAMP, entries.get(0).getTimeLocal());
      assertEquals(ZIP_TIMESTAMP, entries.get(1).getTimeLocal());
      assertEquals("summary", read(zip, entries.get(0)));
      assertEquals("suite", read(zip, entries.get(1)));
    }
  }

  @Test
  public void createArchiveIsIndependentOfInputOrderAndTimestamps() throws Exception {
    Path directory = Files.createTempDirectory("j2cl-test-suite-tool");
    Path firstInput = directory.resolve("first.jar");
    Path secondInput = directory.resolve("second.jar");
    Path firstOutput = directory.resolve("first.zip");
    Path secondOutput = directory.resolve("second.zip");
    List<Map.Entry<String, String>> entries =
        List.of(Map.entry("b.testsuite", "b"), Map.entry("a.testsuite", "a"));
    writeZip(firstInput, entries, 1_500_000_000_000L);
    writeZip(secondInput, List.of(entries.get(1), entries.get(0)), 1_700_000_000_000L);

    J2clTestSuiteTool.createArchive(firstInput, firstOutput);
    J2clTestSuiteTool.createArchive(secondInput, secondOutput);

    assertArrayEquals(Files.readAllBytes(firstOutput), Files.readAllBytes(secondOutput));
  }

  @Test
  public void createArchiveRejectsMissingArtifacts() throws Exception {
    Path directory = Files.createTempDirectory("j2cl-test-suite-tool");
    Path input = directory.resolve("input.jar");
    writeZip(input, List.of(Map.entry("ignored.class", "class")));

    IOException error =
        assertThrows(
            IOException.class,
            () -> J2clTestSuiteTool.createArchive(input, directory.resolve("output.zip")));

    assertEquals("J2CL test generation produced no test suite artifacts", error.getMessage());
  }

  @Test
  public void extractSingleJavaScriptCopiesTheOnlySuite() throws Exception {
    Path directory = Files.createTempDirectory("j2cl-test-suite-tool");
    Path input = directory.resolve("input.zip");
    Path output = directory.resolve("output.js");
    writeZip(
        input,
        List.of(
            Map.entry("javatests/Example.js", "suite"),
            Map.entry("test_summary.json", "summary")));

    J2clTestSuiteTool.extractSingleJavaScript(input, output);

    assertEquals("suite", Files.readString(output));
  }

  @Test
  public void extractSingleJavaScriptRejectsZeroOrMultipleSuites() throws Exception {
    Path directory = Files.createTempDirectory("j2cl-test-suite-tool");
    Path emptyInput = directory.resolve("empty.zip");
    Path multipleInput = directory.resolve("multiple.zip");
    writeZip(emptyInput, List.of(Map.entry("test_summary.json", "summary")));
    writeZip(
        multipleInput,
        List.of(Map.entry("a.js", "a"), Map.entry("b.js", "b")));

    IOException emptyError =
        assertThrows(
            IOException.class,
            () ->
                J2clTestSuiteTool.extractSingleJavaScript(
                    emptyInput, directory.resolve("empty.js")));
    IOException multipleError =
        assertThrows(
            IOException.class,
            () ->
                J2clTestSuiteTool.extractSingleJavaScript(
                    multipleInput, directory.resolve("multiple.js")));

    assertEquals(
        "Expected exactly one generated JavaScript test suite, found 0", emptyError.getMessage());
    assertEquals(
        "Expected exactly one generated JavaScript test suite, found 2", multipleError.getMessage());
  }

  private static void writeZip(Path path, List<Map.Entry<String, String>> entries)
      throws IOException {
    writeZip(path, entries, 1_600_000_000_000L);
  }

  private static void writeZip(
      Path path, List<Map.Entry<String, String>> entries, long timestamp) throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
      for (Map.Entry<String, String> source : entries) {
        ZipEntry entry = new ZipEntry(source.getKey());
        entry.setTime(timestamp++);
        zip.putNextEntry(entry);
        zip.write(source.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
  }

  private static List<String> entryNames(List<? extends ZipEntry> entries) {
    List<String> names = new ArrayList<>();
    for (ZipEntry entry : entries) {
      names.add(entry.getName());
    }
    return names;
  }

  private static String read(ZipFile zip, ZipEntry entry) throws IOException {
    try (var input = zip.getInputStream(entry)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
