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

import com.google.j2cl.errors.Errors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Finds the CompilationUnit name for a given ITypeBinding.
 * <p>
 * Handles both Source and Binary type bindings.
 * <p>
 * On the surface the problem being solved here seems simple, but because of non-main top level
 * types in binary type bindings, it can actually be tricky.
 */
public class CompilationUnitNameLocator {
  private class SourceCompilationUnitIndexBuilder extends ASTVisitor {
    @Override
    public boolean visit(TypeDeclaration node) {
      ITypeBinding typeBinding = node.resolveBinding();
      sourceUnitNamesByTypeBinding.put(typeBinding, currentCompilationUnitName);
      return super.visit(node);
    }
  }

  private static String removeExtension(String fileName) {
    return fileName.substring(0, fileName.length() - ".java".length());
  }

  private static char[][] toCompoundName(String qualifiedName) {
    String[] parts = qualifiedName.split("\\.");
    char[][] compoundName = new char[parts.length][];
    for (int i = 0; i < parts.length; i++) {
      compoundName[i] = parts[i].toCharArray();
    }
    return compoundName;
  }

  private final INameEnvironment binaryNameEnvironment;
  private String currentCompilationUnitName;
  private final Map<ITypeBinding, String> sourceUnitNamesByTypeBinding = new HashMap<>();
  private final Errors errors;

  public CompilationUnitNameLocator(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath,
      FrontendOptions options,
      Errors errors) {
    this.errors = errors;
    binaryNameEnvironment = JdtUtils.createNameEnvironment(options);

    SourceCompilationUnitIndexBuilder sourceCompilationUnitIndexBuilder =
        new SourceCompilationUnitIndexBuilder();
    for (Entry<String, org.eclipse.jdt.core.dom.CompilationUnit> entry :
        jdtUnitsByFilePath.entrySet()) {
      String sourceFilePath = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();

      currentCompilationUnitName = removeExtension(new File(sourceFilePath).getName());
      compilationUnit.accept(sourceCompilationUnitIndexBuilder);
    }
  }

  /**
   * Finds and returns the name of the compilation unit that provided the given type, otherwise
   * returns null.
   */
  public String find(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    if (typeBinding.isPrimitive()) {
      // Primitives don't come from a compilation unit.
      return null;
    }

    if (sourceUnitNamesByTypeBinding.containsKey(typeBinding)) {
      return sourceUnitNamesByTypeBinding.get(typeBinding);
    }

    String binaryCompilationUnitName = getBinaryCompilationUnitName(typeBinding);
    if (binaryCompilationUnitName != null) {
      return binaryCompilationUnitName;
    }

    // Most likely resulting from a missing dependency.
    errors.error(Errors.ERR_CANNOT_FIND_UNIT, typeBinding.getQualifiedName());
    return null;
  }

  private String getBinaryCompilationUnitName(ITypeBinding typeBinding) {
    if (typeBinding.isMember()) {
      return getBinaryCompilationUnitName(typeBinding.getDeclaringClass());
    }

    NameEnvironmentAnswer answer =
        binaryNameEnvironment.findType(toCompoundName(typeBinding.getQualifiedName()));
    if (answer != null && answer.isBinaryType()) {
      return removeExtension(new String(answer.getBinaryType().sourceFileName()));
    }

    return null;
  }
}
