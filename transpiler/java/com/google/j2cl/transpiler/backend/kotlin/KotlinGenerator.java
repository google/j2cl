/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;

/** Kotlin source generator. */
public class KotlinGenerator {
  private static final String FILE_SUFFIX = ".kt";

  private final Type type;
  private final SourceBuilder sourceBuilder = new SourceBuilder();
  private final StatementTranspiler statementTranspiler;

  public KotlinGenerator(Problems problems, Type type) {
    this.type = type;
    this.statementTranspiler = new StatementTranspiler(sourceBuilder);
  }

  public String getSuffix() {
    return FILE_SUFFIX;
  }

  public String renderOutput() {
    try {
      renderImports();
      renderClass();
      return sourceBuilder.build();
    } catch (RuntimeException e) {
      // Catch all unchecked exceptions and rethrow them with more context to make debugging easier.
      // Yes this is really being done on purpose.
      throw new InternalCompilerError(
          e,
          "Error generating source for type %s.",
          type.getDeclaration().getQualifiedBinaryName());
    }
  }

  private void renderImports() {}

  private void renderClass() {
    sourceBuilder.append("package ");
    sourceBuilder.append(type.getDeclaration().getPackageName());
    sourceBuilder.newLine();
    sourceBuilder.newLine();
    renderClassBody();
  }

  private void renderClassBody() {
    if (!type.getDeclaration().isFinal()) {
      sourceBuilder.append("open ");
    }
    sourceBuilder.append("class ");
    sourceBuilder.append(type.getDeclaration().getSimpleSourceName());
    // TODO(dpo): add support for class hierarchies
    sourceBuilder.append(getExtendsClause(type));
    // TODO(dpo): add support for field declarations
    if (!type.getMethods().isEmpty()) {
      sourceBuilder.append(" ");
      sourceBuilder.openBrace();
      renderTypeMethods();
      sourceBuilder.closeBrace();
    }
  }

  private static String getExtendsClause(Type type) {
    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || TypeDescriptors.isJavaLangObject(superTypeDescriptor)) {
      return "";
    }
    String superTypeName = superTypeDescriptor.getQualifiedSourceName();
    return String.format(" extends %s", superTypeName);
  }

  private void renderTypeMethods() {
    // TODO(dpo): Remove short term hack to pull static methods into companion object.
    boolean hasStaticMethods = false;
    for (Method method : type.getMethods()) {
      if (method.isStatic()) {
        hasStaticMethods = true;
        continue;
      }
      sourceBuilder.newLine();
      emitMethodHeader(method);
      statementTranspiler.renderStatement(method.getBody());
    }

    if (!hasStaticMethods) {
      return;
    }

    sourceBuilder.newLine();
    sourceBuilder.append("companion object ");
    sourceBuilder.openBrace();
    for (Method method : type.getMethods()) {
      if (!method.isStatic()) {
        continue;
      }
      sourceBuilder.newLine();
      emitMethodHeader(method);
      statementTranspiler.renderStatement(method.getBody());
    }
    sourceBuilder.closeBrace();
  }

  private void emitMethodHeader(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    sourceBuilder.append("fun ");
    sourceBuilder.append(methodDescriptor.getName());
    sourceBuilder.append("(");
    String separator = "";
    for (Variable var : method.getParameters()) {
      sourceBuilder.append(separator);
      sourceBuilder.append(var.getName());
      sourceBuilder.append(": ");
      emitType(var.getTypeDescriptor());
      separator = ", ";
    }
    sourceBuilder.append(") ");
    if (!TypeDescriptors.isPrimitiveVoid(method.getDescriptor().getReturnTypeDescriptor())) {
      emitType(method.getDescriptor().getReturnTypeDescriptor());
      sourceBuilder.append(" ");
    }
  }

  /**
   * Emits the proper fully rendered type for the give type descriptor at the current location.
   * TODO(dpo): Move this to a better long term place (this logic is likely to get pretty complex).
   */
  private void emitType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayType = (ArrayTypeDescriptor) typeDescriptor;
      sourceBuilder.append("Array<");
      emitType(arrayType.getComponentTypeDescriptor());
      sourceBuilder.append(">");
    } else {
      // TODO(dpo): Other type descriptor logic.
      sourceBuilder.append(typeDescriptor.getReadableDescription());
    }
  }
}
