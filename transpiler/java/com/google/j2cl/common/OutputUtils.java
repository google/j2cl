/*
 * Copyright 2020 Google Inc.
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

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.Problems.FatalError;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utilities for tools to process output. */
public class OutputUtils {

  /** Abstract output of a command. */
  public interface Output extends AutoCloseable {
    Path getRoot();

    @Override
    void close();
  }

  public static Output initOutput(Path output, Problems problems) {
    return output.toString().endsWith(".zip") || output.toString().endsWith(".jar")
        ? getZipOutput(output, problems)
        : getDirOutput(output, problems);
  }

  private static Output getDirOutput(Path output, Problems problems) {
    if (Files.isRegularFile(output)) {
      problems.fatal(FatalError.OUTPUT_LOCATION, output);
    }

    return new Output() {
      @Override
      public Path getRoot() {
        return output;
      }

      @Override
      public void close() {}
    };
  }

  private static Output getZipOutput(Path output, Problems problems) {
    FileSystem newFileSystem = initZipOutput(output, problems);

    return new Output() {
      @Override
      public Path getRoot() {
        return newFileSystem.getPath("/");
      }

      @Override
      public void close() {
        try {
          newFileSystem.close();
        } catch (IOException e) {
          problems.fatal(FatalError.CANNOT_CLOSE_ZIP, e.getMessage());
        }
      }
    };
  }

  private static FileSystem initZipOutput(Path output, Problems problems) {
    if (Files.isDirectory(output)) {
      problems.fatal(FatalError.OUTPUT_LOCATION, output);
    }

    // Ensures that we will not fail if the zip already exists.
    output.toFile().delete();

    try {
      return FileSystems.newFileSystem(
          URI.create("jar:" + output.toAbsolutePath().toUri()), ImmutableMap.of("create", "true"));
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_CREATE_ZIP, output, e.getMessage());
      return null;
    }
  }

  private OutputUtils() {}
}
