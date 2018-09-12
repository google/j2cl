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
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
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
  private static Set<String> unusedTypesFromRta;
  private static Set<String> unusedMembersFromRta;
  private static Set<String> unusedTypesFromGoldenFile;
  private static Set<String> unusedMembersFromGoldenFile;

  @BeforeClass
  public static void setUp() throws Exception {
    unusedTypesFromRta = readOutputFile(getUnusedTypesOutputFile());
    unusedMembersFromRta = readOutputFile(getUnusedMembersOutputFile());
    unusedTypesFromGoldenFile = readGoldenFile(getUnusedTypesGoldenFile());
    unusedMembersFromGoldenFile = readGoldenFile(getUnusedMembersGoldenFile());
  }

  @Test
  public void unusedTypesFromRtaEqualsUnusedTypesFromGoldenFile() {
    checkDifferences(unusedTypesFromRta, unusedTypesFromGoldenFile, "types");
  }

  @Test
  public void unusedMembersFromRtaEqualsUnusedMembersFromGoldenFile() {
    checkDifferences(unusedMembersFromRta, unusedMembersFromGoldenFile, "members");
  }

  private void checkDifferences(Set<String> fromRta, Set<String> fromGoldenFile, String fileType) {
    StringBuilder errorMessageBuilder = new StringBuilder();
    Set<String> differenceFromRta = Sets.difference(fromRta, fromGoldenFile);

    if (!differenceFromRta.isEmpty()) {
      errorMessageBuilder
          .append("\nThese ")
          .append(fileType)
          .append(" are NOT present in the golden file but are returned by RTA:\n")
          .append(Joiner.on("\n").skipNulls().join(differenceFromRta))
          .append("\n");
    }

    Set<String> differenceFromGoldenFiles = Sets.difference(fromGoldenFile, fromRta);
    if (!differenceFromGoldenFiles.isEmpty()) {
      errorMessageBuilder
          .append("\nThese ")
          .append(fileType)
          .append(" are present in the golden file but are NOT returned by RTA:\n")
          .append(Joiner.on("\n").skipNulls().join(differenceFromGoldenFiles))
          .append("\n");
    }

    String errorMsg = errorMessageBuilder.toString();
    if (!Strings.isNullOrEmpty(errorMsg)) {
      fail(errorMsg);
    }
  }

  private static Set<String> readGoldenFile(String filePath) throws IOException {
    return readFileLines(filePath).stream()
        .filter(GoldenFileTester::skipCommentLine)
        // Allow blank lines in golden file in order to group some lines
        .filter(not(Strings::isNullOrEmpty))
        .collect(Collectors.toSet());
  }

  private static Set<String> readOutputFile(String filePath) throws IOException {
    return readFileLines(filePath).stream()
        // Skip JRE to avoid making the test fragile.
        .filter(GoldenFileTester::skipLineFromJre)
        .collect(Collectors.toSet());
  }

  private static List<String> readFileLines(String filePath) throws IOException {
    return Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).readLines();
  }

  private static boolean skipLineFromJre(String line) {
    return !line.startsWith("java.")
        && !line.startsWith("javaemul.")
        && !line.startsWith("vmbootstrap.");
  }

  private static boolean skipCommentLine(String line) {
    return !line.startsWith("#");
  }

  private static String getUnusedMembersGoldenFile() {
    return System.getProperty("unused_members_golden_file");
  }

  private static String getUnusedTypesGoldenFile() {
    return System.getProperty("unused_types_golden_file");
  }

  private static String getUnusedMembersOutputFile() {
    return System.getProperty("unused_members_rta");
  }

  private static String getUnusedTypesOutputFile() {
    return System.getProperty("unused_types_rta");
  }
}
