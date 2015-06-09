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
  private final List<String> classpathEntries = new ArrayList<>();;
  private final List<String> sourcepathEntries = new ArrayList<>();;
  private final String encoding;

  public JdtParser(FrontendOptions options, Errors errors) {
    this.errors = errors;

    String version = options.getSourceVersion();
    compilerOptions.put(JavaCore.COMPILER_SOURCE, version);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, version);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, version);

    classpathEntries.addAll(options.getClasspathEntries());
    classpathEntries.addAll(options.getBootclassPathEntries());
    sourcepathEntries.addAll(options.getSourcepathEntries());
    encoding = options.getEncoding();
  }

  public interface Handler {
    public void handleCompilationUnit(String path, CompilationUnit compilationUnit);
  }

  public void parseFiles(List<String> paths, final Handler handler) {
    ASTParser parser = newASTParser(true, false);
    FileASTRequestor astRequestor =
        new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {
            if (checkCompilationErrors(sourceFilePath, compilationUnit)) {
              handler.handleCompilationUnit(sourceFilePath, compilationUnit);
            }
          }
        };
    parser.createASTs(
        Iterables.toArray(paths, String.class),
        getEncodings(paths.size()),
        new String[] {},
        astRequestor,
        null);
  }

  private ASTParser newASTParser(boolean resolveBinding, boolean includeRunningVMBootclasspath) {
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
            String.format(
                "%s:%s: %s", filename, problem.getSourceLineNumber(), problem.getMessage()));
        hasErrors = true;
      }
    }
    return !hasErrors;
  }
}
