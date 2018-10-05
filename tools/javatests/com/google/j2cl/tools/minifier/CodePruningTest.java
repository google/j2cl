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
package com.google.j2cl.tools.minifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.tools.rta.CodeRemovalInfo;
import com.google.j2cl.tools.rta.LineRange;
import com.google.j2cl.tools.rta.UnusedLines;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for code pruning done in {@link J2clMinifier}. */
@RunWith(JUnit4.class)
public class CodePruningTest {
  private static final String EMPTY_LINE = "";

  @Test
  public void testFilePruning() {
    CodeRemovalInfo codeRemovalInfo =
        CodeRemovalInfo.newBuilder().addUnusedFiles("Foo.java.js").build();

    J2clMinifier j2clMinifier = new J2clMinifier();
    j2clMinifier.setupRtaCodeRemoval(codeRemovalInfo);

    assertThat(j2clMinifier.minify("Foo.java.js", "Foo.java.js file content")).isEmpty();

    // work with file in zip file
    assertThat(j2clMinifier.minify("my_zip_file.js.zip!/Foo.java.js", "Foo.java.js file content"))
        .isEmpty();

    // any other file should not be modify
    assertThat(j2clMinifier.minify("other/path/Foo.java.js", "other/path/Foo.java.js file content"))
        .isEqualTo("other/path/Foo.java.js file content");
  }

  @Test
  public void testLinePruning() {
    CodeRemovalInfo codeRemovalInfo = createFileLinesRemoval("Foo.java.js", from(2, 3), from(5, 7));

    J2clMinifier j2clMinifier = new J2clMinifier();
    j2clMinifier.setupRtaCodeRemoval(codeRemovalInfo);

    String fileContent =
        createFileContent(
            "line 0: not pruned",
            "line 1: not pruned",
            "line 2: pruned",
            "line 3: not pruned",
            "line 4: not pruned",
            "line 5: pruned",
            "line 6: pruned",
            "line 7: not pruned",
            "line 8: not pruned",
            "line 9: not pruned");

    String expectedFileContent =
        createFileContent(
            "line 0: not pruned",
            "line 1: not pruned",
            EMPTY_LINE,
            "line 3: not pruned",
            "line 4: not pruned",
            EMPTY_LINE,
            EMPTY_LINE,
            "line 7: not pruned",
            "line 8: not pruned",
            "line 9: not pruned");

    assertThat(j2clMinifier.minify("Foo.java.js", fileContent)).isEqualTo(expectedFileContent);
  }

  @Test
  public void testFirstLinePruning() {
    CodeRemovalInfo codeRemovalInfo = createFileLinesRemoval("Foo.java.js", from(0, 1));

    J2clMinifier j2clMinifier = new J2clMinifier();
    j2clMinifier.setupRtaCodeRemoval(codeRemovalInfo);

    String fileContent = createFileContent("line 0: pruned", "line 1: not pruned");

    String expectedFileContent = createFileContent(EMPTY_LINE, "line 1: not pruned");

    assertThat(j2clMinifier.minify("Foo.java.js", fileContent)).isEqualTo(expectedFileContent);
  }

  @Test
  public void testLastLinePruning() {
    CodeRemovalInfo codeRemovalInfo = createFileLinesRemoval("Foo.java.js", from(1, 2));

    J2clMinifier j2clMinifier = new J2clMinifier();
    j2clMinifier.setupRtaCodeRemoval(codeRemovalInfo);

    String fileContent = createFileContent("line 0: not pruned", "line 1: pruned");
    String expectedFileContent = createFileContent("line 0: not pruned", EMPTY_LINE);

    assertThat(j2clMinifier.minify("Foo.java.js", fileContent)).isEqualTo(expectedFileContent);
  }

  @Test
  public void testNonExistingLinePruningThrowsIllegalStateException() {
    CodeRemovalInfo codeRemovalInfo = createFileLinesRemoval("Foo.java.js", from(2, 3));

    J2clMinifier j2clMinifier = new J2clMinifier();
    j2clMinifier.setupRtaCodeRemoval(codeRemovalInfo);

    String fileContent = createFileContent("line 0: not pruned", "line 1: not pruned");

    assertThrows(
        IllegalStateException.class, () -> j2clMinifier.minify("Foo.java.js", fileContent));
  }

  @Test
  public void testUnusedLinesWithoutLineRangeThrowsIllegalStateException() {
    CodeRemovalInfo codeRemovalInfoWithoutLineRange =
        CodeRemovalInfo.newBuilder()
            .addUnusedLines(
                UnusedLines.newBuilder()
                    // We don't specify any lines to remove in this file.
                    .setFileKey("Foo.java.js")
                    .build())
            .build();

    J2clMinifier j2clMinifier = new J2clMinifier();

    assertThrows(
        IllegalStateException.class,
        () -> j2clMinifier.setupRtaCodeRemoval(codeRemovalInfoWithoutLineRange));
  }

  private static CodeRemovalInfo createFileLinesRemoval(String fileKey, LineRange... lineRanges) {
    return CodeRemovalInfo.newBuilder()
        .addUnusedLines(
            UnusedLines.newBuilder()
                .setFileKey(checkNotNull(fileKey))
                .addAllUnusedRanges(ImmutableList.copyOf(lineRanges))
                .build())
        .build();
  }

  private static String createFileContent(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private static LineRange from(int start, int end) {
    return LineRange.newBuilder().setLineStart(start).setLineEnd(end).build();
  }
}
