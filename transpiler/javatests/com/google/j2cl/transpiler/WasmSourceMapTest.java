/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.transpiler;

import static com.google.common.truth.Truth.assertThat;

import com.google.debugging.sourcemap.SourceMapConsumerV3;
import com.google.debugging.sourcemap.SourceMapParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** End to end test for command line invocations. */
@RunWith(JUnit4.class)
public class WasmSourceMapTest {

  @Test
  public void sourceFilesExistInRunfiles() throws IOException, SourceMapParseException {
    String basePath = "transpiler/javatests/com/google/j2cl/transpiler/";
    Path sourceMapFilePath = resolvePathToRunfiles(basePath + "sampleapp_dev.wasm.map");
    SourceMapConsumerV3 consumer = new SourceMapConsumerV3();
    consumer.parse(Files.readAllLines(sourceMapFilePath).stream().collect(Collectors.joining()));
    assertThat(
            consumer.getOriginalSources().stream()
                .anyMatch(s -> s.endsWith("java/lang/Object.java")))
        .isTrue();

    for (String sourceFilePath : consumer.getOriginalSources()) {
      assertThat(Files.exists(resolvePathToRunfiles(basePath + sourceFilePath))).isTrue();
    }
  }

  private static Path resolvePathToRunfiles(String path) {
    try {
      return Paths.get(
          com.google.devtools.build.runfiles.Runfiles.create().rlocation(
              "com_google_j2cl/" + path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
