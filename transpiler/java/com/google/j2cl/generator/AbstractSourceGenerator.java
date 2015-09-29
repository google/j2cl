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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A base class for source generators. We may have two subclasses, which are
 * JavaScriptGenerator and SourceMapGenerator.
 */
public abstract class AbstractSourceGenerator {
  protected final Errors errors;

  public AbstractSourceGenerator(Errors errors) {
    this.errors = errors;
  }

  public void writeToFile(Path outputPath, Charset charset) {
    try {
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      Files.createDirectories(outputPath.getParent());
      Files.write(outputPath, toSource().getBytes(charset));
    } catch (IOException e) {
      errors.error(Errors.Error.ERR_ERROR, e.getMessage());
      errors.maybeReportAndExit();
    }
  }

  public abstract String toSource();

  public abstract String getSuffix();

  public abstract String getTemplateFilePath();
}
