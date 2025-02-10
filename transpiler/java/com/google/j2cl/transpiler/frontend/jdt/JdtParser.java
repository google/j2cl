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
package com.google.j2cl.transpiler.frontend.jdt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.frontend.common.FrontendOptions;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * A delegator of JDT's ASTParser that provides a more convenient interface for parsing source files
 * into compilation unit.
 */
public class JdtParser {
  private static final String JAVA_VERSION = JavaCore.VERSION_17;
  private static final int AST_JLS_VERSION = AST.JLS17;

  private final Map<String, String> compilerOptions = new HashMap<>();
  private final Problems problems;

  /** Create and initialize a JdtParser based on passed parameters. */
  public JdtParser(Problems problems) {
    compilerOptions.put(JavaCore.COMPILER_SOURCE, JAVA_VERSION);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JAVA_VERSION);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, JAVA_VERSION);
    compilerOptions.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");

    this.problems = problems;
  }

  public Library parseFiles(FrontendOptions options) {
    CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
        parseFiles(
            options.getSources(),
            options.getClasspaths(),
            options.getGenerateKytheIndexingMetadata(),
            options.getForbiddenAnnotations(),
            TypeDescriptors.getWellKnownTypeNames());
    problems.abortIfHasErrors();

    var packageAnnotationsResolver =
        PackageAnnotationsResolver.create(
            compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath().entrySet().stream()
                .filter(e -> e.getKey().endsWith("package-info.java"))
                .map(Entry::getValue),
            new PackageInfoCache(options.getClasspaths(), problems));
    problems.abortIfCancelled();
    JdtEnvironment environment = new JdtEnvironment(packageAnnotationsResolver);

    Map<String, CompilationUnit> jdtUnitsByFilePath =
        compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath();
    List<ITypeBinding> wellKnownTypeBindings = compilationUnitsAndTypeBindings.getTypeBindings();
    CompilationUnitBuilder compilationUnitBuilder =
        new CompilationUnitBuilder(wellKnownTypeBindings, environment);

    ImmutableList.Builder<com.google.j2cl.transpiler.ast.CompilationUnit> compilationUnits =
        ImmutableList.builder();
    for (var e : jdtUnitsByFilePath.entrySet()) {
      compilationUnits.add(compilationUnitBuilder.buildCompilationUnit(e.getKey(), e.getValue()));
      problems.abortIfCancelled();
    }
    return Library.newBuilder().setCompilationUnits(compilationUnits.build()).build();
  }

  /** Returns a map from file paths to compilation units after JDT parsing. */
  private CompilationUnitsAndTypeBindings parseFiles(
      List<FileInfo> filePaths,
      List<String> classpathEntries,
      boolean useTargetPath,
      List<String> forbiddenAnnotations,
      Collection<String> binaryNamesToResolve) {

    // Parse and create a compilation unit for every file.
    ASTParser parser = newASTParser(classpathEntries);

    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable
    final Map<String, CompilationUnit> compilationUnitsByFilePath = new LinkedHashMap<>();
    final List<ITypeBinding> wellKnownTypeBindings = new ArrayList<>();
    final Map<String, String> targetPathBySourcePath =
        filePaths.stream().collect(Collectors.toMap(FileInfo::sourcePath, FileInfo::targetPath));

    FileASTRequestor astRequestor =
        new FileASTRequestor() {
          @Override
          public void acceptAST(String filePath, CompilationUnit compilationUnit) {
            if (compilationHasErrors(filePath, compilationUnit, forbiddenAnnotations)) {
              return;
            }
            String filePathKey = filePath;
            if (useTargetPath) {
              filePathKey = targetPathBySourcePath.get(filePath);
            }
            compilationUnitsByFilePath.put(filePathKey, compilationUnit);
          }

          @Override
          public void acceptBinding(String bindingKey, IBinding binding) {
            if (binding == null) {
              // Type was requested but wasn't found, ignore.
              return;
            }
            wellKnownTypeBindings.add((ITypeBinding) binding);
          }
        };
    parser.createASTs(
        filePaths.stream()
            .map(FileInfo::sourcePath)
            // Skip module-info in JDT to avoid NPEs. They are not used regardless...
            .filter(f -> !f.endsWith("module-info.java"))
            .toArray(String[]::new),
        getEncodings(filePaths.size()),
        binaryNamesToResolve.stream().map(BindingKey::createTypeBindingKey).toArray(String[]::new),
        astRequestor,
        null);
    return new CompilationUnitsAndTypeBindings(compilationUnitsByFilePath, wellKnownTypeBindings);
  }

  /** Resolves binary names to type bindings. */
  public List<ITypeBinding> resolveBindings(
      List<String> classpathEntries, Collection<String> binaryNames) {
    return parseFiles(
            /* filePaths= */ new ArrayList<>(),
            /* classpathEntries= */ classpathEntries,
            /* useTargetPath= */ false,
            /* forbiddenAnnotations= */ new ArrayList<>(),
            binaryNames)
        .getTypeBindings();
  }

  private ASTParser newASTParser(List<String> classpathEntries) {
    ASTParser parser = ASTParser.newParser(AST_JLS_VERSION);

    parser.setCompilerOptions(compilerOptions);
    parser.setResolveBindings(true);
    // setBindingsRecovery(true) is needed to be able to read annotation parameters even if the
    // annotation is not fully resolved due to missing dependencies.
    parser.setBindingsRecovery(true);
    parser.setEnvironment(
        Iterables.toArray(classpathEntries, String.class), new String[0], new String[0], false);
    return parser;
  }

  private String[] getEncodings(int length) {
    String[] encodings = new String[length];
    Arrays.fill(encodings, StandardCharsets.UTF_8.name());
    return encodings;
  }

  private boolean compilationHasErrors(
      String filename, CompilationUnit unit, List<String> forbiddenAnnotations) {
    boolean hasErrors = false;
    // Here we check for instances of @GwtIncompatible in the ast. If that is the case, we throw an
    // error since these should have been stripped by the build system already.
    for (String forbiddenAnnotation : forbiddenAnnotations) {
      AnnotatedNodeCollector collector = new AnnotatedNodeCollector(forbiddenAnnotation);
      unit.accept(collector);
      if (!collector.getNodes().isEmpty()) {
        problems.fatal(
            unit.getLineNumber(collector.getNodes().get(0).getStartPosition()),
            filename,
            FatalError.INCOMPATIBLE_ANNOTATION_FOUND_IN_COMPILE,
            forbiddenAnnotation);
      }
    }
    for (IProblem problem : unit.getProblems()) {
      if (problem.isError()) {
        problems.error(problem.getSourceLineNumber(), filename, "%s", problem.getMessage());
        hasErrors = true;
      }
    }
    return hasErrors;
  }
}
