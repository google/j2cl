/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.frontend.common;


import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.FormatMethod;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Base class for implementing that AST conversion from different front ends. */
public abstract class AbstractCompilationUnitBuilder {

  private final PackageInfoCache packageInfoCache = PackageInfoCache.get();

  /** Type stack to keep track of the lexically enclosing types as they are being created. */
  private final List<Type> typeStack = new ArrayList<>();

  private String currentSourceFile;
  private CompilationUnit currentCompilationUnit;

  /**
   * Sets the JS namespace and whether it defines a null marked scope for a package that is being
   * compiled from source.
   */
  protected void setPackagePropertiesFromSource(
      String packageName, String jsNamespace, String objectiveCName, boolean isNullMarked) {
    packageInfoCache.setPackageProperties(
        PackageInfoCache.SOURCE_CLASS_PATH_ENTRY,
        packageName,
        jsNamespace,
        objectiveCName,
        isNullMarked);
  }

  protected String getCurrentSourceFile() {
    return currentSourceFile;
  }

  protected void setCurrentSourceFile(String currentSourceFile) {
    this.currentSourceFile = currentSourceFile;
  }

  protected CompilationUnit getCurrentCompilationUnit() {
    return currentCompilationUnit;
  }

  protected void setCurrentCompilationUnit(CompilationUnit currentCompilationUnit) {
    this.currentCompilationUnit = currentCompilationUnit;
  }

  /** Invoke {@code supplier} with {@code type} in the type stack. */
  protected <T> T processEnclosedBy(Type type, Supplier<T> supplier) {
    typeStack.add(type);
    T converted = supplier.get();
    typeStack.remove(typeStack.size() - 1);
    return converted;
  }

  /** Returns the current type. */
  protected Type getCurrentType() {
    return Iterables.getLast(typeStack, null);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // General helpers.
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates temporary variables for a resource that is declared outside of the try-catch statement.
   */
  protected static VariableDeclarationExpression toResource(Expression expression) {
    if (expression instanceof VariableDeclarationExpression) {
      return (VariableDeclarationExpression) expression;
    }

    // Create temporary variables for resources declared outside of the try statement.
    return VariableDeclarationExpression.newBuilder()
        .addVariableDeclaration(
            Variable.newBuilder()
                .setName("$resource")
                .setTypeDescriptor(expression.getTypeDescriptor())
                .setFinal(true)
                .build(),
            expression)
        .build();
  }

  @FormatMethod
  protected Error internalCompilerError(Throwable e, String format, Object... params) {
    return new InternalCompilerError(e, internalCompilerErrorMessage(format, params));
  }

  @FormatMethod
  protected Error internalCompilerError(String format, Object... params) {
    return new InternalCompilerError(internalCompilerErrorMessage(format, params));
  }

  @FormatMethod
  protected String internalCompilerErrorMessage(String format, Object... params) {
    return String.format(format, params) + ", in file: " + currentSourceFile;
  }
}
