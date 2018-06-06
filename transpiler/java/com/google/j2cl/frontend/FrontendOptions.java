/*
 * Copyright 2015 Google Inc.
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
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.Message;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Frontend options, which is initialized by a Flag instance that is already parsed. */
@AutoValue
public abstract class FrontendOptions {

  public static FrontendOptions create(FrontendFlags flags, Problems problems) {
    checkSourceFiles(flags.files, problems);

    if (flags.readableSourceMaps && flags.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      flags.readableSourceMaps = false;
    }

    return new AutoValue_FrontendOptions(
        getPathEntries(flags.classPath),
        getPathEntries(flags.nativeSourcePath),
        FrontendUtils.getAllSources(flags.files, problems)
            .stream()
            .map(p -> p.sourcePath())
            .collect(ImmutableList.toImmutableList()),
        flags.output.endsWith(".zip")
            ? getZipOutput(flags.output, problems)
            : getDirOutput(flags.output, problems),
        flags.readableSourceMaps,
        flags.declareLegacyNamespaces,
        flags.generateTimeReport,
        flags.generateKytheIndexingMetadata);
  }

  public abstract List<String> getClasspathEntries();

  public abstract List<String> getNativeSourceZipEntries();

  public abstract List<String> getSourceFiles();

  public abstract Path getOutputPath();

  public abstract boolean getShouldPrintReadableSourceMap();

  public abstract boolean getDeclareLegacyNamespace();

  public abstract boolean getGenerateTimeReport();

  public abstract boolean getGenerateKytheIndexingMetadata();

  private static boolean checkSourceFiles(List<String> sourceFiles, Problems problems) {
    for (String sourceFile : sourceFiles) {
      if (isValidExtension(sourceFile)) {
        File file = new File(sourceFile);
        if (!file.isFile()) {
          problems.error(Message.ERR_FILE_NOT_FOUND, sourceFile);
          return false;
        }
      } else {
        // does not support non-java files.
        problems.error(Message.ERR_UNKNOWN_INPUT_TYPE, sourceFile);
        return false;
      }
    }
    return true;
  }

  private static boolean isValidExtension(String sourceFile) {
    return sourceFile.endsWith(".java")
        || sourceFile.endsWith(".srcjar")
        || sourceFile.endsWith("-src.jar");
  }

  private static Path getDirOutput(String output, Problems problems) {
    Path outputPath = Paths.get(output);
    if (Files.isRegularFile(outputPath)) {
      problems.error(Message.ERR_OUTPUT_LOCATION, outputPath);
    }
    return outputPath;
  }

  private static Path getZipOutput(String output, Problems problems) {
    FileSystem newFileSystem = FrontendUtils.initZipOutput(output, problems);
    return newFileSystem == null ? null : newFileSystem.getPath("/");
  }

  private static List<String> getPathEntries(String path) {
    List<String> entries = new ArrayList<>();
    for (String entry : Splitter.on(File.pathSeparatorChar).omitEmptyStrings().split(path)) {
      if (new File(entry).exists()) {
        entries.add(entry);
      }
    }
    return entries;
  }
}
