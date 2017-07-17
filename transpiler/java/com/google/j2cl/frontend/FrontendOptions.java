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
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.Message;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Frontend options, which is initialized by a Flag instance that is already parsed. */
@AutoValue
public abstract class FrontendOptions {

  public static FrontendOptions create(FrontendFlags flags, Problems problems) {
    checkSourceFiles(flags.files, problems);

    return new AutoValue_FrontendOptions(
        getPathEntries(flags.classPath),
        getPathEntries(flags.nativeSourcePath),
        getAllSources(flags.files, problems),
        flags.output.endsWith(".zip")
            ? getZipOutput(flags.output, problems)
            : getDirOutput(flags.output, problems),
        flags.readableSourceMaps,
        flags.declareLegacyNamespaces,
        flags.generateTimeReport);
  }

  public abstract List<String> getClasspathEntries();

  public abstract List<String> getNativeSourceZipEntries();

  public abstract List<String> getSourceFiles();

  public abstract Path getOutputPath();

  public abstract boolean getShouldPrintReadableSourceMap();

  public abstract boolean getDeclareLegacyNamespace();

  public abstract boolean getGenerateTimeReport();

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

  private static ImmutableList<String> getAllSources(List<String> sources, Problems problems) {
    // Make sure to extract all of the Jars into a single temp dir so that when later sorting
    // sourceFilePaths there is no instability introduced by differences in randomly generated
    // temp dir prefixes.
    Path srcjarContentDir;
    try {
      srcjarContentDir = Files.createTempDirectory("source_jar");
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
            f -> f.endsWith("jar") ? extractSourceJar(f, srcjarContentDir, problems) : Stream.of(f))
        .sorted()
        .collect(ImmutableList.toImmutableList());
  }

  private static Stream<String> extractSourceJar(
      String sourceJarPath, Path srcjarContentDir, Problems problems) {
    try {
      ZipFiles.unzipFile(new File(sourceJarPath), srcjarContentDir.toFile());
      return Files.walk(srcjarContentDir).map(Path::toString).filter(f -> f.endsWith(".java"));
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_EXTRACT_ZIP, sourceJarPath);
    }
    return Stream.of();
  }

  private static Path getDirOutput(String output, Problems problems) {
    Path outputPath = Paths.get(output);
    if (Files.isRegularFile(outputPath)) {
      problems.error(Message.ERR_OUTPUT_LOCATION, outputPath);
    }
    return outputPath;
  }

  private static Path getZipOutput(String output, Problems problems) {
    Path outputPath = Paths.get(output);
    if (Files.isDirectory(outputPath)) {
      problems.error(Message.ERR_OUTPUT_LOCATION, outputPath);
    }

    // Ensures that we will not fail if the zip already exists.
    outputPath.toFile().delete();

    try {
      FileSystem newFileSystem =
          FileSystems.newFileSystem(
              URI.create("jar:file:" + outputPath.toAbsolutePath()),
              ImmutableMap.of("create", "true"));
      return newFileSystem.getPath("/");
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_OPEN_ZIP, outputPath.toString(), e.getMessage());
      return null;
    }
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
