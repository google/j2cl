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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.common.SourceFile;
import java.io.IOException;
import java.util.List;

/**
 * NativeJavaScriptFile contains information about native javascript files that is used to output
 * native code during the javascript generation stage.
 */
public class NativeJavaScriptFile implements SourceFile {
  private final FileInfo fileInfo;
  private final String content;

  public static final String NATIVE_EXTENSION = ".native.js";
  public static final String NON_JS_NATIVE_EXTENSION = ".native_js";

  public NativeJavaScriptFile(FileInfo fileInfo, String content) {
    checkArgument(fileInfo.targetPath().endsWith(NATIVE_EXTENSION));
    // Replace .native.js with .native_js so that the file is not seen as a JavaScript source
    // by jscompiler.
    this.fileInfo = fileInfo.withTargetPath(toNonJsExtension(fileInfo.targetPath()));
    this.content = content;
  }

  @Override
  public FileInfo getFileInfo() {
    return fileInfo;
  }

  public String getContent() {
    return content;
  }

  /** Returns the simple or qualified name of the type the native.js file is associated with. */
  public String getSimpleOrQualifiedName() {
    return Files.getNameWithoutExtension(getFileInfo().targetPath());
  }

  @Override
  public List<String> getLines() throws IOException {
    return Splitter.on('\n').splitToList(getContent());
  }

  private static String toNonJsExtension(String path) {
    return path.substring(0, path.lastIndexOf(NATIVE_EXTENSION)) + NON_JS_NATIVE_EXTENSION;
  }
}
