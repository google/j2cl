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

import com.google.common.io.Files;
import com.google.j2cl.errors.Errors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A base class for source generators. We may have two subclasses, which are
 * JavaScriptGenerator and SourceMapGenerator.
 */
public abstract class AbstractSourceGenerator {
  protected final Errors errors;
  protected final String outputPath;
  protected final File outputDirectory;
  protected final Charset charset;

  public AbstractSourceGenerator(
      Errors errors, String outputPath, File outputDirectory, Charset charset) {
    this.errors = errors;
    this.outputPath = outputPath;
    this.outputDirectory = outputDirectory;
    this.charset = charset;
  }

  public void writeToFile() {
    try {
      File outputFile = new File(outputDirectory, getOutputPath());
      File parentFile = outputFile.getParentFile();
      if (!parentFile.exists() && !parentFile.mkdirs()) {
        errors.error(Errors.ERR_CANNOT_CREATE_DIRECTORY, outputDirectory.getAbsolutePath());
        errors.maybeReportAndExit();
      }
      String source = toSource();
      System.out.println("writing to file: " + outputFile.getAbsolutePath());
      Files.write(source, outputFile, charset);
    } catch (IOException e) {
      errors.error(e.getMessage());
    }
  }

  public abstract String toSource();

  public abstract String getSuffix();

  public String getOutputPath() {
    return outputPath + getSuffix();
  }
}
