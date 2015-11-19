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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.j2cl.errors.Errors;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A delegator of JDT's ASTParser that provides a more convenient interface for
 * parsing source files into compilation unit.
 */
public class JdtParser {
  private final Errors errors;
  private final Map<String, String> compilerOptions = new HashMap<>();
  private final List<String> classpathEntries = new ArrayList<>();
  private final List<String> sourcepathEntries = new ArrayList<>();
  private final String encoding;
  private boolean includeRunningVMBootclasspath;

  /**
   * Create and initialize a JdtParser based on an options object.
   */
  public JdtParser(FrontendOptions options, Errors errors) {
    this(
        options.getSourceVersion(),
        options.getClasspathEntries(),
        options.getBootclassPathEntries(),
        options.getSourcepathEntries(),
        options.getEncoding(),
        errors);
  }

  /**
   * Create and initialize a JdtParser based on passed parameters.
   */
  public JdtParser(
      String sourceVersion,
      List<String> classpathEntries,
      List<String> bootclassPathEntries,
      List<String> sourcepathEntries,
      String encoding,
      Errors errors) {
    compilerOptions.put(JavaCore.COMPILER_SOURCE, sourceVersion);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, sourceVersion);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, sourceVersion);

    this.classpathEntries.addAll(classpathEntries);
    this.classpathEntries.addAll(bootclassPathEntries);
    this.sourcepathEntries.addAll(sourcepathEntries);
    this.encoding = encoding;
    this.errors = errors;
  }

  /**
   * Returns a map from file paths to compilation units after JDT parsing.
   */
  public Map<String, CompilationUnit> parseFiles(List<String> filePaths) {
    // Preprocess every file and writes the preprocessed content to a temporary file.
    JavaPreprocessor preprocessor = new JavaPreprocessor(compilerOptions);
    final Map<String, String> preprocessedFilesByOriginal =
        preprocessor.preprocessFiles(filePaths, encoding, errors);
    if (preprocessedFilesByOriginal == null) {
      Preconditions.checkState(
          errors.errorCount() > 0, "Didn't get processed files map, but no errors generated.");
      return null;
    }

    // Parse and create a compilation unit for every file.
    ASTParser parser = newASTParser(true);
    final Map<String, CompilationUnit> compilationUnitsByFilePath = new HashMap<>();
    FileASTRequestor astRequestor =
        new FileASTRequestor() {
          @Override
          public void acceptAST(String filePath, CompilationUnit compilationUnit) {
            String originalFilePath = preprocessedFilesByOriginal.get(filePath);
            Preconditions.checkState(
                originalFilePath != null, "Can't find original path for file %s", filePath);
            if (checkCompilationErrors(originalFilePath, compilationUnit)) {
              compilationUnitsByFilePath.put(originalFilePath, compilationUnit);
            }
          }
        };
    parser.createASTs(
        Iterables.toArray(preprocessedFilesByOriginal.keySet(), String.class),
        getEncodings(filePaths.size()),
        new String[] {},
        astRequestor,
        null);
    return compilationUnitsByFilePath;
  }

  public void setIncludeRunningVMBootclasspath(boolean includeRunningVMBootclasspath) {
    this.includeRunningVMBootclasspath = includeRunningVMBootclasspath;
  }

  private ASTParser newASTParser(boolean resolveBinding) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);

    parser.setCompilerOptions(compilerOptions);
    parser.setResolveBindings(resolveBinding);
    parser.setEnvironment(
        Iterables.toArray(classpathEntries, String.class),
        Iterables.toArray(sourcepathEntries, String.class),
        getEncodings(sourcepathEntries.size()),
        includeRunningVMBootclasspath);
    return parser;
  }

  private String[] getEncodings(int length) {
    if (encoding == null) {
      return null;
    }
    String[] encodings = new String[length];
    Arrays.fill(encodings, encoding);
    return encodings;
  }

  private boolean checkCompilationErrors(String filename, CompilationUnit unit) {
    boolean hasErrors = false;
    for (IProblem problem : unit.getProblems()) {
      if (problem.isError()) {
        errors.error(
            Errors.Error.ERR_ERROR,
            String.format(
                "%s:%s: %s", filename, problem.getSourceLineNumber(), problem.getMessage()));
        hasErrors = true;
      }
    }
    return !hasErrors;
  }
}
