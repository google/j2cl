/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.tools.rta;

import static com.google.common.base.Predicates.not;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Filter every lines concerning the jre from the rta algorithm output and compare them against
 * golden files
 */
@RunWith(JUnit4.class)
public class GoldenFileTester {
  private static List<String> unusedTypesFromRta;
  private static List<String> unusedTypesFromGoldenFile;

  @BeforeClass
  public static void setUp() throws Exception {
    unusedTypesFromRta = readOutputFile(getUnusedTypesOutputFile());
    unusedTypesFromGoldenFile = readGoldenFile(getUnusedTypesGoldenFile());
  }

  @Test
  public void unusedTypesFromRtaEqualsUnusedTypesFromGoldenFile() {
    checkDifferences(unusedTypesFromRta, unusedTypesFromGoldenFile, "unused types");
  }

  private static void checkDifferences(
      List<String> fromRta, List<String> fromGoldenFile, String fileType) {
    List<String> patternsInGoldenFile =
        fromGoldenFile.stream().filter(x -> x.endsWith(".*")).collect(Collectors.toList());
    fromGoldenFile.removeAll(patternsInGoldenFile);
    for (String pattern : patternsInGoldenFile) {
      assertWithMessage("Missing pattern %s in %s", pattern, fileType)
          .that(fromRta.removeIf(x -> x.matches(pattern)))
          .isEqualTo(true);
    }
    assertWithMessage("Comparing rta result with golden file with for " + fileType)
        .that(fromRta)
        .containsExactlyElementsIn(fromGoldenFile);
  }

  private static List<String> readGoldenFile(String filePath) throws IOException {
    return readFileLines(filePath).stream()
        .filter(GoldenFileTester::skipCommentLine)
        // Allow blank lines in golden file in order to group some lines
        .filter(not(Strings::isNullOrEmpty))
        .collect(Collectors.toList());
  }

  private static List<String> readOutputFile(String filePath) throws IOException {
    return readFileLines(filePath).stream()
        // Skip JRE to avoid making the test fragile.
        .filter(GoldenFileTester::skipLineFromJre)
        .collect(Collectors.toList());
  }

  private static List<String> readFileLines(String filePath) throws IOException {
    return Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).readLines();
  }

  private static boolean skipLineFromJre(String line) {
    return !line.startsWith("java.")
        && !line.startsWith("javax.")
        && !line.startsWith("javaemul.")
        && !line.startsWith("kotlin.")
        && !line.startsWith("vmbootstrap.");
  }

  private static boolean skipCommentLine(String line) {
    return !line.startsWith("#");
  }

  private static String getUnusedTypesGoldenFile() {
    return System.getProperty("unused_types_golden_file");
  }

  private static String getUnusedTypesOutputFile() {
    return System.getProperty("unused_types_rta");
  }
}
