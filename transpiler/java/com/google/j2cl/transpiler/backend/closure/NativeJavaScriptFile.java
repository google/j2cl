/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.backend.closure;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.MoreFiles;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NativeJavaScriptFile contains information about native javascript files that is used to output
 * native code during the javascript generation stage.
 */
public class NativeJavaScriptFile {
  private final String relativePath;
  private final String content;
  private boolean used = false;

  public static final String NATIVE_EXTENSION = ".native.js";

  public NativeJavaScriptFile(String relativePath, String content) {
    this.relativePath = relativePath;
    this.content = content;
  }

  /** Returns the path for the native file relative to the root. */
  public String getRelativeFilePath() {
    // Replace .native.js by .native_js so that the file is not seen as a JavaScript source
    // by jscompiler.
    return getRelativePathWithoutExtension() + ".native_js";
  }

  public String getRelativePathWithoutExtension() {
    return relativePath.substring(0, relativePath.lastIndexOf(NATIVE_EXTENSION));
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return relativePath;
  }

  /** Can only set to used. */
  public void setUsed() {
    used = true;
  }

  public boolean wasUsed() {
    return used;
  }

  /**
   * Given a list of native files, return a map of file paths to NativeJavaScriptFile objects of the
   * form:
   *
   * <p>/com/google/example/nativejsfile1 => NativeJavaScriptFile
   *
   * <p>/com/google/example/nativejsfile2 => NativeJavaScriptFile
   */
  public static Map<String, NativeJavaScriptFile> getMap(List<FileInfo> files, Problems problems) {
    Map<String, NativeJavaScriptFile> loadedFilesByPath = new LinkedHashMap<>();
    for (FileInfo file : files) {
      try {
        String content = MoreFiles.asCharSource(Paths.get(file.sourcePath()), UTF_8).read();
        NativeJavaScriptFile nativeFile = new NativeJavaScriptFile(file.targetPath(), content);
        loadedFilesByPath.put(nativeFile.getRelativePathWithoutExtension(), nativeFile);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
      }
    }
    return loadedFilesByPath;
  }
}
