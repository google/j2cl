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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.frontend.common.FrontendOptions;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  public JavacParser(Problems problems) {
    this.problems = problems;
  }

  /** Returns a map from file paths to compilation units after Javac parsing. */
  @Nullable
  public Library parseFiles(FrontendOptions options) {
    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable
    final Map<String, String> targetPathBySourcePath =
        options.getSources().stream()
            .collect(toImmutableMap(FileInfo::sourcePath, FileInfo::targetPath));

    problems.abortIfCancelled();
    try {
      // TODO(b/470163090): Customize the JavaCompiler instance to have more granular control for
      // cancelation.
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      JavacFileManager fileManager =
          (JavacFileManager)
              compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
      var searchpath = options.getClasspaths().stream().collect(toImmutableList());
      fileManager.setLocationFromPaths(StandardLocation.PLATFORM_CLASS_PATH, searchpath);
      fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, searchpath);
      if (options.getSystem() != null) {
        fileManager.setLocationFromPaths(
            StandardLocation.SYSTEM_MODULES, ImmutableList.of(options.getSystem()));
      }
      JavacTaskImpl task =
          (JavacTaskImpl)
              compiler.getTask(
                  null,
                  fileManager,
                  diagnostics,
                  getJavacOptions(options),
                  null,
                  fileManager.getJavaFileObjectsFromFiles(
                      targetPathBySourcePath.keySet().stream()
                          .map(File::new)
                          .collect(toImmutableList())));
      task.addTaskListener(
          new TaskListener() {
            @Override
            public void started(TaskEvent taskEvent) {
              problems.abortIfCancelled();
            }

            @Override
            public void finished(TaskEvent taskEvent) {
              problems.abortIfCancelled();
            }
          });

      List<CompilationUnitTree> javacCompilationUnits = Lists.newArrayList(task.parse());
      task.analyze();
      reportErrors(diagnostics, javacCompilationUnits, options.getForbiddenAnnotations());
      problems.abortIfHasErrors();

      JavaEnvironment javaEnvironment =
          new JavaEnvironment(task.getContext(), TypeDescriptors.getWellKnownTypeNames(), problems);
      CompilationUnitBuilder compilationUnitBuilder =
          new CompilationUnitBuilder(javaEnvironment, problems);

      ImmutableList.Builder<CompilationUnit> compilationUnits = ImmutableList.builder();
      for (var cu : javacCompilationUnits) {
        String sourcePath = cu.getSourceFile().getName();
        if (options.getGenerateKytheIndexingMetadata()) {
          // If Kythe metadata is being requested, use the target path.
          sourcePath = targetPathBySourcePath.get(sourcePath);
        }
        compilationUnits.add(compilationUnitBuilder.buildCompilationUnit(sourcePath, cu));
        problems.abortIfCancelled();
      }
      return Library.newBuilder().setCompilationUnits(compilationUnits.build()).build();
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
      return null;
    }
  }

  private static ImmutableList<String> getJavacOptions(FrontendOptions options) {
    return ImmutableList.<String>builder()
        // Allow JRE classes to depend on internal annotations (in the unnamed module). This is
        // needed for both JRE and non-JRE compilation; some JRE methods are annotated with
        // internal annotations which are then read by some backends.
        .add("--add-reads")
        .add("java.base=ALL-UNNAMED")
        .addAll(options.getJavacOptions())
        .build();
  }

  private void reportErrors(
      DiagnosticCollector<JavaFileObject> diagnosticCollector,
      List<CompilationUnitTree> javacCompilationUnits,
      ImmutableList<String> forbiddenAnnotations) {
    // Here we check for instances of forbidden annotations in the ast. If that is the case, we
    // throw an error since these should have been stripped by the build system already.
    for (var compilationUnit : javacCompilationUnits) {
      AnnotatedNodeCollector annotatedNodeCollector =
          new AnnotatedNodeCollector(forbiddenAnnotations, /* stopTraversalOnMatch= */ false);
      annotatedNodeCollector.visitCompilationUnit(compilationUnit, null);
      for (var forbiddenAnnotation : forbiddenAnnotations) {
        ImmutableSet<Tree> nodesWithForbiddenAnnotations =
            annotatedNodeCollector.getNodesWithAnnotation(forbiddenAnnotation);
        if (!nodesWithForbiddenAnnotations.isEmpty()) {
          JCTree sampleNode = ((JCTree) Iterables.getFirst(nodesWithForbiddenAnnotations, null));
          problems.fatal(
              (int) (compilationUnit.getLineMap().getLineNumber(sampleNode.getStartPosition()) - 1),
              compilationUnit.getSourceFile().getName(),
              FatalError.INCOMPATIBLE_ANNOTATION_FOUND_IN_COMPILE,
              forbiddenAnnotation);
        }
      }
    }

    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
      if (diagnostic.getKind() == Kind.ERROR) {
        String errorMessage = diagnostic.getMessage(Locale.US);
        if (diagnostic.getSource() != null) {
          problems.error(
              (int) diagnostic.getLineNumber(),
              diagnostic.getSource().getName(),
              "%s",
              errorMessage);
        } else {
          problems.error("%s", errorMessage);
        }
      }
    }
  }
}
