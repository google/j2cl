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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.errors.Errors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

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

  private static final List<String> wellKnownClassNames =
      ImmutableList.of(
          "java.lang.Class",
          "java.lang.CharSequence",
          "java.lang.Comparable",
          "java.lang.Number",
          "java.lang.Object",
          "java.lang.String",
          "java.lang.Throwable");

  /** Returns a map from file paths to compilation units after JDT parsing. */
  public CompilationUnitsAndTypeBindings parseFiles(List<String> filePaths) {
    // Preprocess every file and writes the preprocessed content to a temporary file.
    JavaPreprocessor preprocessor = new JavaPreprocessor(compilerOptions);
    final Map<String, String> preprocessedFilesByOriginal =
        preprocessor.preprocessFiles(filePaths, encoding, errors);
    if (preprocessedFilesByOriginal == null) {
      checkState(
          errors.errorCount() > 0, "Didn't get processed files map, but no errors generated.");
      return null;
    }

    // Parse and create a compilation unit for every file.
    ASTParser parser = newASTParser(true);

    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable
    final Map<String, CompilationUnit> compilationUnitsByFilePath = new LinkedHashMap<>();
    final List<ITypeBinding> wellKnownTypeBindings = new ArrayList<>();

    FileASTRequestor astRequestor =
        new FileASTRequestor() {
          @Override
          public void acceptAST(String filePath, CompilationUnit compilationUnit) {
            String originalFilePath = preprocessedFilesByOriginal.get(filePath);
            checkState(originalFilePath != null, "Can't find original path for file %s", filePath);
            if (checkCompilationErrors(originalFilePath, compilationUnit)) {
              compilationUnitsByFilePath.put(originalFilePath, compilationUnit);
            }
          }

          @Override
          public void acceptBinding(String bindingKey, IBinding binding) {
            wellKnownTypeBindings.add((ITypeBinding) binding);
          }
        };
    parser.createASTs(
        preprocessedFilesByOriginal.keySet().stream().toArray(String[]::new),
        getEncodings(filePaths.size()),
        wellKnownClassNames.stream().map(BindingKey::createTypeBindingKey).toArray(String[]::new),
        astRequestor,
        null);
    return new CompilationUnitsAndTypeBindings(compilationUnitsByFilePath, wellKnownTypeBindings);
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
      // TODO(b/30126552): Replace this solution with a better alternative (which possibly is
      // stripping GwtIncompatible as a preprocessing step outside the transpiler.
      if (problem.isError()) {
        if (isErrorCausedByNotImplentingGwtIncompatibleOverride(problem, unit)) {
          // HACK: Ignore GwtIncompatible related errors, this is fragile because is tied
          // to the jdt error reporting.
          continue;
        }
        errors.error(
            Errors.Error.ERR_ERROR,
            String.format(
                "%s:%s: %s", filename, problem.getSourceLineNumber(), problem.getMessage()));
        hasErrors = true;
      }
    }
    return !hasErrors;
  }

  private boolean isErrorCausedByNotImplentingGwtIncompatibleOverride(
      IProblem problem, CompilationUnit unit) {
    final boolean[] found = new boolean[1];
    // TODO(rluble): check if this error is caused by the presence of a GwtIncompatible
    // abstract method.
    if (problem.getID() == IProblem.AbstractMethodMustBeImplemented) {
      final String superTypeName = problem.getArguments()[2];
      final String methodName = problem.getArguments()[0];
      unit.accept(
          new ASTVisitor() {
            @Override
            public void endVisit(TypeDeclaration typeDeclaration) {
              // Find the offending supertype.
              ITypeBinding superTypeBinding =
                  findTypeBindingByName(typeDeclaration.resolveBinding(), superTypeName);
              if (superTypeBinding == null) {
                return;
              }
              // Find the offending method.
              for (IMethodBinding methodBinding : superTypeBinding.getDeclaredMethods()) {
                if (!methodBinding.getName().equals(methodName)) {
                  continue;
                }
                // Check whether it is annotated with GwtIncompatible.
                for (IAnnotationBinding annotationBinding : methodBinding.getAnnotations()) {
                  if (annotationBinding.getName().equals("GwtIncompatible")) {
                    found[0] = true;
                  }
                }
              }
            }
          });
    }
    return found[0];
  }

  private ITypeBinding findTypeBindingByName(ITypeBinding typeBinding, String soughtTypeName) {
    if (typeBinding == null) {
      return null;
    }
    if (soughtTypeName.equals(typeBinding.getQualifiedName())) {
      return typeBinding;
    }
    ITypeBinding superTypeBinding =
        findTypeBindingByName(typeBinding.getSuperclass(), soughtTypeName);

    if (superTypeBinding != null) {
      return superTypeBinding;
    }

    for (ITypeBinding superInterfaceBinding : typeBinding.getInterfaces()) {
      superTypeBinding = findTypeBindingByName(superInterfaceBinding, soughtTypeName);
      if (superTypeBinding != null) {
        return superTypeBinding;
      }
    }

    return null;
  }
}
