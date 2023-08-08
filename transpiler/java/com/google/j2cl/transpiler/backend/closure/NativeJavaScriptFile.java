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

import java.nio.file.Path;

/**
 * NativeJavaScriptFile contains information about native javascript files that is used to output
 * native code during the javascript generation stage.
 */
public class NativeJavaScriptFile {
  private final String relativePath;
  private final String content;

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

  /** Returns the FQN if the filename appears to be of the form "<package>.<class>.native.js". */
  public String getFullyQualifiedName() {
    return Path.of(getRelativePathWithoutExtension()).getFileName().toString();
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return relativePath;
  }
}
