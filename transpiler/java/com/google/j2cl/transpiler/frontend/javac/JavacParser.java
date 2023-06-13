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
package com.google.j2cl.transpiler.frontend.javac;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * A delegator of Javac ASTParser that provides a more convenient interface for parsing source files
 * into compilation unit.
 */
public class JavacParser {
  private final Problems problems;
  private final ImmutableList<String> classpathEntries;

  /** Create and initialize a JavacParser based on passed parameters. */
  public JavacParser(List<String> classpathEntries, Problems problems) {

    this.classpathEntries = ImmutableList.copyOf(classpathEntries);
    this.problems = problems;
  }

  /** Returns a map from file paths to compilation units after Javac parsing. */
  @Nullable
  public List<CompilationUnit> parseFiles(
      List<FileInfo> filePaths, boolean useTargetPath, ImmutableList<String> forbiddenAnnotations) {

    if (filePaths.isEmpty()) {
      return ImmutableList.of();
    }

    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable
    final Map<String, String> targetPathBySourcePath =
        filePaths.stream().collect(Collectors.toMap(FileInfo::sourcePath, FileInfo::targetPath));

    try {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      JavacFileManager fileManager =
          (JavacFileManager)
              compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
      List<File> searchpath = classpathEntries.stream().map(File::new).collect(toList());
      fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, searchpath);
      fileManager.setLocation(StandardLocation.CLASS_PATH, searchpath);
      JavacTaskImpl task =
          (JavacTaskImpl)
              compiler.getTask(
                  null,
                  fileManager,
                  diagnostics,
                  // TODO(b/143213486): Figure out how to make the pipeline work with the module
                  // system.
                  ImmutableList.of(
                      "--patch-module",
                      "java.base=.",
                      // Allow JRE classes are allowed to depend on the jsinterop annotations
                      "--add-reads",
                      "java.base=ALL-UNNAMED"),
                  null,
                  fileManager.getJavaFileObjectsFromFiles(
                      targetPathBySourcePath.keySet().stream().map(File::new).collect(toList())));
      List<CompilationUnitTree> javacCompilationUnits = Lists.newArrayList(task.parse());
      task.analyze();
      if (hasErrors(diagnostics, javacCompilationUnits, forbiddenAnnotations)) {
        return ImmutableList.of();
      }

      JavaEnvironment javaEnvironment =
          new JavaEnvironment(task.getContext(), TypeDescriptors.getWellKnownTypeNames());

      return CompilationUnitBuilder.build(javacCompilationUnits, javaEnvironment);
    } catch (IOException e) {
      problems.fatal(FatalError.valueOf(e.getMessage()));
      return null;
    }
  }

  private boolean hasErrors(
      DiagnosticCollector<JavaFileObject> diagnosticCollector,
      List<CompilationUnitTree> javacCompilationUnits,
      ImmutableList<String> forbiddenAnnotations) {
    boolean hasErrors = false;
    // Here we check for instances of @GwtIncompatible in the ast. If that is the case, we throw an
    // error since these should have been stripped by the build system already.
    for (String forbiddenAnnotation : forbiddenAnnotations) {
      Set<String> filesWithGwtIncompatible =
          AnnotatedNodeCollector.filesWithAnnotation(javacCompilationUnits, forbiddenAnnotation);
      if (!filesWithGwtIncompatible.isEmpty()) {
        problems.fatal(
            FatalError.INCOMPATIBLE_ANNOTATION_FOUND_IN_COMPILE,
            forbiddenAnnotation,
            filesWithGwtIncompatible.iterator().next());
      }
    }
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
      if (diagnostic.getKind() == Kind.ERROR) {
        problems.error(
            (int) diagnostic.getLineNumber(),
            diagnostic.getSource().getName(),
            "%s",
            diagnostic.getMessage(Locale.US));
        hasErrors = true;
      }
    }
    return hasErrors;
  }
}
