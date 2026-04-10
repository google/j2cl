/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.common;

import static com.google.common.truth.Truth.assertThat;

import com.google.j2cl.common.OutputUtils.Output;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class OutputUtilsTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testInitOutput_zip() throws Exception {
    Path tempRoot = tempFolder.getRoot().toPath();
    Path outputPath = tempFolder.newFile("my_output.zip").toPath();
    Problems problems = new Problems();

    try (Output output = OutputUtils.initOutput(outputPath, tempRoot, problems)) {

      Path tempDir = output.createTempDirectory("_source_jars");
      assertThat(tempDir).isEqualTo(tempRoot.resolve("_source_jars"));
    }
    assertThat(problems.hasErrors()).isFalse();
  }

  @Test
  public void testInitOutput_directory() throws Exception {
    Path tempRoot = tempFolder.getRoot().toPath();
    Path outputPath = tempFolder.newFolder("my_output_dir").toPath();
    Problems problems = new Problems();

    try (Output output = OutputUtils.initOutput(outputPath, tempRoot, problems)) {
      assertThat(output.createTempDirectory("_source_jars"))
          .isEqualTo(tempRoot.resolve("_source_jars"));
    }
    assertThat(problems.hasErrors()).isFalse();
  }

  @Test
  public void testInitOutputForBazel_zip() throws Exception {
    Path tempRoot = tempFolder.getRoot().toPath();
    Path outputPath = tempFolder.newFile("my_output.zip").toPath();
    Problems problems = new Problems();

    try (Output output = OutputUtils.initOutputForBazel(outputPath, problems)) {
      assertThat(problems.hasErrors()).isFalse();
      assertThat(output.createTempDirectory("_source_jars"))
          .isEqualTo(tempRoot.resolve("_j2cl/my_output/_source_jars"));
    }
  }

  @Test
  public void testInitOutputForBazel_directory() throws Exception {
    Path tempRoot = tempFolder.getRoot().toPath();
    Path outputPath = tempFolder.newFolder("my_output_dir").toPath();
    Problems problems = new Problems();

    try (Output output = OutputUtils.initOutputForBazel(outputPath, problems)) {
      assertThat(problems.hasErrors()).isFalse();
      assertThat(output.createTempDirectory("_source_jars"))
          .isEqualTo(tempRoot.resolve("_j2cl/my_output_dir/_source_jars"));
    }
  }

  @Test
  public void testCreateTempDirectory_cleansExistingDirectory() throws Exception {
    Path outputPath = tempFolder.newFile("my_output.zip").toPath();
    Problems problems = new Problems();

    // Pollute the directory simulating a previous run with the same output .
    Path preExistingFile =
        tempFolder.newFolder("_j2cl/my_output/_source_jars").toPath().resolve("stale_file.txt");
    Files.writeString(preExistingFile, "stale content");
    assertThat(Files.exists(preExistingFile)).isTrue();

    try (Output output = OutputUtils.initOutputForBazel(outputPath, problems)) {
      Path tempDir = output.createTempDirectory("_source_jars");
      // Verify the directory exists but the file is gone
      assertThat(Files.exists(tempDir)).isTrue();
      assertThat(Files.exists(preExistingFile)).isFalse();
    }
  }
}
