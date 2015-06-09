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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.errors.Errors;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Frontend options, which can be initialized by a Flag instance that is already parsed.
 */
public class FrontendOptions {
  private final Errors errors;

  private List<String> classpathEntries;
  private List<String> sourcepathEntries;
  private List<String> bootclassPathEntries;
  private File outputDirectory;
  private String encoding;
  private String sourceVersion;
  private List<String> sourceFilePaths;

  private static final Set<String> VALID_JAVA_VERSIONS =
      ImmutableSet.of("1.8", "1.7", "1.6", "1.5");

  public FrontendOptions(Errors errors, FrontendFlags flags) {
    this.errors = errors;
    initOptions(flags);
  }

  /**
   * Initialize compiler options by parsed flags.
   */
  public void initOptions(FrontendFlags flags) {
    setClasspathEntries(flags.classpath);
    setSourcepathEntries(flags.sourcepath);
    setBootclassPathEntries(flags.bootclasspath);
    setOutputDirectory(flags.outputDir);
    setSourceFiles(flags.files);
    setSourceVersion(flags.source);
    setEncoding(flags.encoding);
  }

  public List<String> getClasspathEntries() {
    return this.classpathEntries;
  }

  public void setClasspathEntries(List<String> classpathEntries) {
    this.classpathEntries = classpathEntries;
  }

  public void setClasspathEntries(String classpath) {
    this.classpathEntries = getPathEntries(classpath);
  }

  public List<String> getSourcepathEntries() {
    return this.sourcepathEntries;
  }

  public void setSourcepathEntries(List<String> sourcepathEntries) {
    this.sourcepathEntries = sourcepathEntries;
  }

  public void setSourcepathEntries(String sourcepath) {
    this.sourcepathEntries = getPathEntries(sourcepath);
  }

  public List<String> getBootclassPathEntries() {
    return this.bootclassPathEntries;
  }

  public void setBootclassPathEntries(List<String> bootclassPathEntries) {
    this.bootclassPathEntries = bootclassPathEntries;
  }

  public void setBootclassPathEntries(String bootclassPath) {
    this.bootclassPathEntries = getPathEntries(bootclassPath);
  }

  public File getOutputDirectory() {
    return this.outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public String getEncoding() {
    return this.encoding;
  }

  public void setEncoding(String encoding) {
    if (checkEncoding(encoding)) {
      this.encoding = encoding;
    }
  }

  public boolean checkEncoding(String encoding) {
    try {
      // Verify encoding has a supported charset.
      Charset.forName(encoding);
    } catch (UnsupportedCharsetException e) {
      errors.error(Errors.ERR_UNSUPPORTED_ENCODING, encoding);
      return false;
    }
    return true;
  }

  public String getSourceVersion() {
    return this.sourceVersion;
  }

  public void setSourceVersion(String sourceVersion) {
    if (checkSourceVersion(sourceVersion)) {
      this.sourceVersion = sourceVersion;
    }
  }

  public boolean checkSourceVersion(String sourceVersion) {
    if (!VALID_JAVA_VERSIONS.contains(sourceVersion)) {
      errors.error(Errors.ERR_INVALID_SOURCE_VERSION, sourceVersion);
      return false;
    }
    return true;
  }

  public List<String> getSourceFiles() {
    return this.sourceFilePaths;
  }

  public void setSourceFiles(List<String> sourceFiles) {
    if (checkSourceFiles(sourceFiles)) {
      this.sourceFilePaths = sourceFiles;
    }
  }

  private boolean checkSourceFiles(List<String> sourceFiles) {
    for (String sourceFile : sourceFiles) {
      if (sourceFile.endsWith(".java")) {
        File file = new File(sourceFile);
        if (!file.exists()) {
          errors.error(Errors.ERR_FILE_NOT_FOUND, sourceFile);
          return false;
        }
        if (!file.isFile()) {
          errors.error(Errors.ERR_INVALID_SOURCE_FILE, sourceFile);
          return false;
        }
      } else {
        // does not support non-java files.
        errors.error(Errors.ERR_INVALID_SOURCE_FILE, sourceFile);
        return false;
      }
    }
    return true;
  }

  private static List<String> getPathEntries(String path) {
    List<String> entries = new ArrayList<>();
    for (String entry : Splitter.on(File.pathSeparatorChar).split(path)) {
      if (new File(entry).exists()) {
        entries.add(entry);
      }
    }
    return entries;
  }
}
