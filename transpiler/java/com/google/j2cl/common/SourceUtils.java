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
package com.google.j2cl.common;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.Problems.FatalError;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import javax.annotation.Nullable;

/** Utilities for tools to process source files. */
public class SourceUtils {

  /** Stores path of files to be processed. */
  @AutoValue
  public abstract static class FileInfo implements Comparable<FileInfo> {

    static FileInfo create(String sourcePath, String originalPath) {
      return create(sourcePath, originalPath, originalPath);
    }

    private static FileInfo create(String sourcePath, String originalPath, String targetPath) {
      return new AutoValue_SourceUtils_FileInfo(sourcePath, originalPath, targetPath);
    }

    /**
     * The location of the file on disk, for the purpose of reading its contents.
     *
     * <p>This might be the original file path or a path in a temp directory where the file was
     * extracted from a zip file for example.
     */
    public abstract String sourcePath();

    public abstract String originalPath();

    /**
     * The root relative path.
     *
     * <p>This is the path from the java root or the absolute path on disk if there is no Java root.
     */
    public abstract String targetPath();

    @Override
    public int compareTo(FileInfo o) {
      return targetPath().compareTo(o.targetPath());
    }
  }

  private static final String TEMP_ROOT = "j2cl_sources";

  /** Returns all individual sources where source jars extracted and flattened. */
  @Nullable
  public static Stream<FileInfo> getAllSourcesFromPaths(Stream<Path> sources, Problems problems) {
    return getAllSources(sources.map(Path::toString), problems);
  }

  /** Returns all individual sources where source jars extracted and flattened. */
  @Nullable
  public static Stream<FileInfo> getAllSources(Stream<String> sources, Problems problems) {
    // Make sure to extract all of the Jars into a single temp dir so that when later sorting
    // sourceFilePaths there is no instability introduced by differences in randomly generated
    // temp dir prefixes.
    Path sourcesDir;
    try {
      Path tempDir = Files.createTempDirectory(null);
      // Make sure we create a root so getJavaPath is still reasonable in case of no Java root.
      sourcesDir = Files.createDirectory(tempDir.resolve(TEMP_ROOT));
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_CREATE_TEMP_DIR, e.getMessage());
      return null;
    }

    // Sort source file paths so that our input is always in a stable order. If this is not done
    // and you can't trust the input to have been provided already in a stable order then the result
    // is that you will create an output Foo.js.zip with randomly ordered entries, and this will
    // cause unstable optimization in JSCompiler.
    return sources
        .flatMap(
            f ->
                f.endsWith("jar") || f.endsWith("zip")
                    ? extractZip(f, sourcesDir, problems).stream()
                    : Stream.of(FileInfo.create(f, f.toString(), getJavaPath(f.toString()))))
        .sorted()
        .distinct();
  }

  @Nullable
  private static ImmutableList<FileInfo> extractZip(
      String zipPath, Path sourcesDir, Problems problems) {
    try {
      return ZipFiles.unzipFile(new File(zipPath), sourcesDir.toFile(), problems);
    } catch (ZipException e) {
      problems.fatal(FatalError.CANNOT_EXTRACT_ZIP, zipPath, e.getMessage());
      return null;
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
      return null;
    }
  }

  /**
   * Returns the relative path from java source root.
   *
   * <p>Java source root decision is similar to the algorithm in {@code
   * com.google.devtools.build.lib.rules.java.JavaUtil#getJavaPath} except the full path is returned
   * if there is no known java source root in the path. Also common "super" source roots are
   * considered Java source root.
   */
  public static String getJavaPath(String path) {
    String javaRelativePath = getRelativePath(path, "java", "javatests");
    if (javaRelativePath.equals(path)) {
      // No regular root found. If the file was part of an archive and unzipped in a
      // temp directory, consider the temp directory as source root.
      javaRelativePath = getRelativePath(path, TEMP_ROOT);
    }

    // For super sources consider as the root for super sources.
    return getRelativePath(javaRelativePath, "super", "super-j2cl");
  }

  private static String getRelativePath(String path, String... roots) {
    int index = Integer.MAX_VALUE;
    for (String root : roots) {
      // Choose the one that matches earlier.
      index = Math.min(index, indexAfterRoot(path, root));
    }
    return index < path.length() ? path.substring(index) : path;
  }

  /** Returns the index after root or the end index if not found. */
  private static int indexAfterRoot(String path, String root) {
    String rootPath = root + "/";
    if (path.startsWith(rootPath)) {
      return rootPath.length();
    }
    rootPath = "/" + rootPath;
    int index = path.indexOf(rootPath);
    return index == -1 ? path.length() : index + rootPath.length();
  }

  public static void checkSourceFiles(
      Problems problems, List<String> sourceFiles, String... validExtensions) {
    for (String sourceFile : sourceFiles) {
      if (Arrays.stream(validExtensions).noneMatch(sourceFile::endsWith)) {
        problems.fatal(FatalError.UNKNOWN_INPUT_TYPE, sourceFile);
      }
      if (!Files.isRegularFile(Paths.get(sourceFile))) {
        problems.fatal(FatalError.FILE_NOT_FOUND, sourceFile);
      }
    }
  }

  private SourceUtils() {}
}
