/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.frontend;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.Message;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class contains reusable utilities for tools needing to read from zip files and write from
 * zip files. (J2CL proper and the GwtIncompatible stripper)
 */
public class FrontendUtils {

  /** Stores path of files to be processed. */
  @AutoValue
  public abstract static class FileInfo implements Comparable<FileInfo> {

    public static FileInfo create(String sourcePath, String targetPath) {
      return new AutoValue_FrontendUtils_FileInfo(sourcePath, targetPath);
    }

    public abstract String sourcePath();

    public abstract String targetPath();

    @Override
    public int compareTo(FileInfo o) {
      return targetPath().compareTo(o.targetPath());
    }
  }

  /** Returns all individual sources where source jars extracted and flattened. */
  public static Stream<FileInfo> getAllSources(List<String> sources, Problems problems) {
    // Make sure to extract all of the Jars into a single temp dir so that when later sorting
    // sourceFilePaths there is no instability introduced by differences in randomly generated
    // temp dir prefixes.
    Path sourcesDir;
    try {
      sourcesDir = Files.createTempDirectory("j2cl_sources");
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_CREATE_TEMP_DIR, e.getMessage());
      return null;
    }

    // Sort source file paths so that our input is always in a stable order. If this is not done
    // and you can't trust the input to have been provided already in a stable order then the result
    // is that you will create an output Foo.js.zip with randomly ordered entries, and this will
    // cause unstable optimization in JSCompiler.
    return sources
        .stream()
        .flatMap(
            f ->
                f.endsWith("jar") || f.endsWith("zip")
                    ? extractZip(f, sourcesDir, problems).stream()
                    : Stream.of(FileInfo.create(f, f)))
        .sorted()
        .distinct();
  }

  private static ImmutableList<FileInfo> extractZip(
      String zipPath, Path sourcesDir, Problems problems) {
    try {
      ZipFiles.unzipFile(new File(zipPath), sourcesDir.toFile());
      try (Stream<Path> stream = Files.walk(sourcesDir)) {
        return stream
            .filter(Files::isRegularFile)
            .map(p -> FileInfo.create(p.toString(), sourcesDir.relativize(p).toString()))
            .collect(ImmutableList.toImmutableList());
      }
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_EXTRACT_ZIP, zipPath);
      return ImmutableList.of();
    }
  }

  public static FileSystem initZipOutput(String output, Problems problems) {
    Path outputPath = Paths.get(output);
    if (Files.isDirectory(outputPath)) {
      problems.error(Message.ERR_OUTPUT_LOCATION, outputPath);
    }

    // Ensures that we will not fail if the zip already exists.
    outputPath.toFile().delete();

    try {
      return FileSystems.newFileSystem(
          URI.create("jar:file:" + outputPath.toAbsolutePath()), ImmutableMap.of("create", "true"));
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_OPEN_ZIP, outputPath.toString(), e.getMessage());
      return null;
    }
  }

  /**
   * Loads a potential flag file and returns the flags. Flag files are only allowed as the last
   * parameter and need to start with an '@'.
   */
  public static String[] expandFlagFile(String[] args) throws IOException {
    if (args.length == 0) {
      return args;
    }

    String potentialFlagFile = args[args.length - 1];

    if (potentialFlagFile == null || !potentialFlagFile.startsWith("@")) {
      return args;
    }

    String flagFile = potentialFlagFile.substring(1);

    List<String> combinedArgs = new ArrayList<>();
    String flagFileContent =
        com.google.common.io.Files.toString(new File(flagFile), StandardCharsets.UTF_8);
    List<String> argsFromFlagFile =
        Splitter.on('\n').omitEmptyStrings().splitToList(flagFileContent);
    Collections.addAll(combinedArgs, args);
    // remove the flag file entry
    combinedArgs.remove(combinedArgs.size() - 1);
    combinedArgs.addAll(argsFromFlagFile);

    return combinedArgs.toArray(new String[0]);
  }
}
