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
package com.google.j2cl.generator;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.j2cl.problems.Problems;
import com.google.j2cl.problems.Problems.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * NativeJavaScriptFile contains information about native javascript files that is used to output
 * native code during the javascript generation stage.
 */
public class NativeJavaScriptFile {
  private String relativePath;
  private String content;
  private boolean used = false;
  private String zipPath;

  public static final String NATIVE_EXTENSION = ".native.js";

  public NativeJavaScriptFile(String relativePath, String content, String zipPath) {
    this.relativePath = relativePath;
    this.content = content;
    this.zipPath = zipPath;
  }

  public String getPathWithoutExtension() {
    return relativePath.substring(0, relativePath.lastIndexOf(NATIVE_EXTENSION));
  }

  public String getContent() {
    return content;
  }

  public String getZipPath() {
    return zipPath;
  }

  @Override
  public String toString() {
    return zipPath + "!/" + relativePath;
  }
  /**
   * Can only set to used.
   */
  public void setUsed() {
    used = true;
  }

  public boolean wasUsed() {
    return used;
  }

  /**
   * Given a list of zip file paths, this method will extract files with the
   * extension @NATIVE_EXTENSION and return the a map of file paths to NativeJavaScriptFile objects
   * of the form:
   *
   * <p>/com/google/example/nativejsfile1 => NativeJavaScriptFile
   *
   * <p>/com/google/example/nativejsfile2 => NativeJavaScriptFile
   */
  public static Map<String, NativeJavaScriptFile> getFilesByPathFromZip(
      List<String> zipPaths, String charSet, Problems problems) {
    Map<String, NativeJavaScriptFile> loadedFilesByPath = new LinkedHashMap<>();
    for (String zipPath : zipPaths) {
      try (ZipFile zipFile = new ZipFile(zipPath)) {
        List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
        for (ZipEntry entry : entries) {
          if (!entry.getName().endsWith(NATIVE_EXTENSION)) {
            continue; // If the path isn't of type NATIVE_EXTENSION, don't add it.
          }
          InputStream stream = zipFile.getInputStream(entry);
          String content = CharStreams.toString(new InputStreamReader(stream, charSet));
          Closeables.closeQuietly(stream);
          NativeJavaScriptFile file = new NativeJavaScriptFile(entry.getName(), content, zipPath);
          loadedFilesByPath.put(file.getPathWithoutExtension(), file);
        }
      } catch (IOException e) {
        problems.error(Message.ERR_CANNOT_OPEN_ZIP, zipPath);
      }
    }
    return loadedFilesByPath;
  }
}
