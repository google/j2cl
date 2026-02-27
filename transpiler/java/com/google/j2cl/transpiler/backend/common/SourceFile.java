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
package com.google.j2cl.transpiler.backend.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/** Represents a source file. */
public interface SourceFile {
  String getRelativeFilePath();

  List<String> getLines() throws IOException;

  static SourceFile fromPath(String relativePath) {
    return new SourceFile() {
      private List<String> lines;

      @Override
      public String getRelativeFilePath() {
        return relativePath;
      }

      @Override
      public List<String> getLines() throws IOException {
        if (lines == null) {
          lines = Files.readAllLines(Paths.get(relativePath));
        }
        return lines;
      }
    };
  }
}
