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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.Problems.FatalError;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

/** Utilities for tools to process output. */
public class OutputUtils {

  /** Abstract output of a command. */
  public static class Output implements AutoCloseable {
    private final ExecutorService fileService = Executors.newSingleThreadExecutor();
    private final Problems problems;
    private final Path root;

    private Output(Problems problems, Path root) {
      this.problems = problems;
      this.root = root;
    }

    public void write(String path, byte[] content) {
      Path outputPath = root.resolve(path);
      fileService.execute(() -> writeToFile(outputPath, content));
    }

    public void write(String path, String content) {
      write(path, ImmutableList.of(content));
    }

    public void write(String path, ImmutableList<String> contentChunks) {
      Path outputPath = root.resolve(path);
      fileService.execute(() -> writeToFile(outputPath, contentChunks));
    }

    private void writeToFile(Path outputPath, byte[] content) {
      try {
        createDirectories(outputPath.getParent());
        Files.write(outputPath, content);
        onFileCreation(outputPath);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      }
    }

    private void writeToFile(Path outputPath, ImmutableList<String> chunks) {
      try {
        createDirectories(outputPath.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, UTF_8)) {
          for (String chunk : chunks) {
            writer.append(chunk);
          }
        }
        onFileCreation(outputPath);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      }
    }

    public void copyFile(String fromAbsolute, String to) {
      Path fromPath = Paths.get(fromAbsolute);
      Path toPath = root.resolve(to);
      fileService.execute(() -> copyToFile(fromPath, toPath));
    }

    private void copyToFile(Path from, Path to) {
      try {
        createDirectories(to.getParent());
        Files.copy(
            from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        onFileCreation(to);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_COPY_FILE, e.toString());
      }
    }

    protected void createDirectories(Path outputPath) throws IOException {
      Files.createDirectories(outputPath);
    }

    protected void onFileCreation(Path outputPath) throws IOException {}

    public void cancel() {
      fileService.shutdownNow();
    }

    @Override
    public void close() {
      try {
        fileService.shutdown();
        fileService.awaitTermination(Long.MAX_VALUE, SECONDS);
      } catch (InterruptedException ie) {
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
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

    return new Output(problems, output);
  }

  private static Output getZipOutput(Path output, Problems problems) {
    FileSystem newFileSystem = initZipOutput(output, problems);

    return new Output(problems, newFileSystem.getPath("/")) {

      @Override
      protected final void createDirectories(Path outputPath) throws IOException {
        // We are creating directories one by one so that we can reset the timestamp for each one.
        if (outputPath == null || Files.exists(outputPath)) {
          return;
        }
        createDirectories(outputPath.getParent());
        Files.createDirectory(outputPath);
        onFileCreation(outputPath);
      }

      private final FileTime defaultFileTime = FileTime.fromMillis(0);

      @Override
      protected void onFileCreation(Path path) throws IOException {
        // Wipe entries modification time so that input->output mapping is stable
        // regardless of the time of day.
        Files.getFileAttributeView(path, BasicFileAttributeView.class)
            .setTimes(defaultFileTime, defaultFileTime, defaultFileTime);
      }

      @Override
      public void close() {
        super.close();
        try {
          newFileSystem.close();
        } catch (IOException e) {
          problems.fatal(FatalError.CANNOT_CLOSE_ZIP, e.getMessage());
        }
      }
    };
  }

  @Nullable
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

  public static void writeToFile(Path outputPath, byte[] content, Problems problems) {
    try {
      Files.createDirectories(outputPath.getParent());
      Files.write(outputPath, content);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  /** Returns the package relative path for a file. */
  public static String getPackageRelativePath(String packageName, String filename) {
    return Paths.get(packageName.replace('.', '/'), filename).toString();
  }

  private OutputUtils() {}
}
