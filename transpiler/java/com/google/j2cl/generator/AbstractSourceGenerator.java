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
package com.google.j2cl.generator;

import com.google.j2cl.errors.Errors;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A base class for source generators. We may have two subclasses, which are
 * JavaScriptGenerator and SourceMapGenerator.
 */
public abstract class AbstractSourceGenerator {
  protected final Errors errors;
  protected final FileSystem outputFileSystem;
  protected final String relativeFilePath;
  protected final String outputLocationPath;
  protected final Charset charset;

  public AbstractSourceGenerator(
      Errors errors,
      FileSystem outputFileSystem,
      String outputLocationPath,
      String relativeFilePath,
      Charset charset) {
    this.errors = errors;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.relativeFilePath = relativeFilePath;
    this.charset = charset;
  }

  public void writeToFile() {
    try {
      Path absoluteOutputPath =
          outputLocationPath != null
              ? outputFileSystem.getPath(outputLocationPath, relativeFilePath + getSuffix())
              : outputFileSystem.getPath(relativeFilePath + getSuffix());
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      Files.createDirectories(absoluteOutputPath.getParent());
      Files.write(absoluteOutputPath, toSource().getBytes(charset));
    } catch (IOException e) {
      errors.error(e.getMessage());
      errors.maybeReportAndExit();
    }
  }

  public abstract String toSource();

  public abstract String getSuffix();
}
