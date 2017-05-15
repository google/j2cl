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
package com.google.j2cl.frontend.common;

import com.google.j2cl.problems.Problems;
import com.google.j2cl.problems.Problems.Message;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains reusable utilities for tools needing to read from zip files and write from
 * zip files. (J2CL proper and the GwtIncompatible stripper)
 */
public class FrontendUtils {
  private static final String JAVA_EXTENSION = ".java";
  private static final String SRCJAR_EXTENSION = ".srcjar";

  /** Returns a map of expanded paths by original paths. */
  public static List<String> expandSourcePathSourceJarEntries(
      List<String> sourceFilePaths, Problems problems) {

    Path srcjarContentDir;
    try {
      // Make sure to extract all of the Jars into a single temp dir so that when later sorting
      // sourceFilePaths there is no instability introduced by differences in randomly generated
      // temp dir prefixes.
      srcjarContentDir = Files.createTempDirectory(".srcjar");

    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_CREATE_TEMP_DIR, e.getMessage());
      return null;
    }

    List<String> expandedSourceFilePaths = new ArrayList<>();

    for (String sourceFilePath : sourceFilePaths) {
      if (sourceFilePath.endsWith(SRCJAR_EXTENSION)) {
        try {
          ZipFiles.unzipFile(new File(sourceFilePath), srcjarContentDir.toFile());
        } catch (IOException e) {
          problems.error(Message.ERR_CANNOT_EXTRACT_ZIP, sourceFilePath);
          return null;
        }
      } else {
        expandedSourceFilePaths.add(sourceFilePath);
      }
    }

    // Accumulate the contained .java files back into sourceFilePaths.
    try {
      Files.walkFileTree(
          srcjarContentDir,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
              if (path.toString().endsWith(JAVA_EXTENSION)) {
                expandedSourceFilePaths.add(path.toAbsolutePath().toString());
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_EXTRACT_ZIP, srcjarContentDir.toString());
      return null;
    }

    // Sort source file paths so that our input is always in a stable order. If this is not done
    // and you can't trust the input to have been provided already in a stable order then the result
    // is that you will create an output Foo.js.zip with randomly ordered entries, and this will
    // cause unstable optimization in JSCompiler.
    Collections.sort(expandedSourceFilePaths);
    return expandedSourceFilePaths;
  }

  public static boolean checkSourceFiles(List<String> sourceFiles, Problems problems) {
    for (String sourceFile : sourceFiles) {
      if (sourceFile.endsWith(JAVA_EXTENSION) || sourceFile.endsWith(SRCJAR_EXTENSION)) {
        File file = new File(sourceFile);
        if (!file.exists()) {
          problems.error(Message.ERR_FILE_NOT_FOUND, sourceFile);
          return false;
        }
        if (!file.isFile()) {
          problems.error(Message.ERR_FILE_NOT_FOUND, sourceFile);
          return false;
        }
      } else {
        // does not support non-java files.
        problems.error(Message.ERR_FILE_NOT_FOUND, sourceFile);
        return false;
      }
    }
    return true;
  }

  public static FileSystem initZipOutput(Path outputPath, Problems problems) {
    // Ensures that we will not fail if the zip already exists.
    File zipFile = outputPath.toFile();
    if (zipFile.exists()) {
      zipFile.delete();
    }

    String uriPath = outputPath.toUri().getPath();
    try {
      Map<String, String> env = new HashMap<>();
      env.put("create", "true");
      return FileSystems.newFileSystem(URI.create("jar:file:" + uriPath), env, null);
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_OPEN_ZIP, outputPath.toString(), e.getMessage());
      return null;
    }
  }
}
