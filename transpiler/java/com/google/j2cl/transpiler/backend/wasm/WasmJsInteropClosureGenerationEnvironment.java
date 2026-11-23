/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;
import com.google.j2cl.transpiler.backend.closure.JsKeywords;
import javax.annotation.Nullable;

/** A Closure environment for generating externs and resolving type names. */
class WasmJsInteropClosureGenerationEnvironment extends ClosureGenerationEnvironment {
  public WasmJsInteropClosureGenerationEnvironment() {
    super(ImmutableSet.of(), ImmutableMap.of());
  }

  @Override
  public String getUniqueNameForVariable(HasName variable) {
    // TODO(b/574076464): It is possible, but unlikely, that this results in a collision.
    String name = super.getUniqueNameForVariable(variable);
    return JsKeywords.isKeyword(name) ? name + "__$" : name;
  }

  @Override
  public String aliasForType(TypeDeclaration typeDeclaration) {
    return getJsTypeAlias(typeDeclaration);
  }

  /** Returns whether the type can appear in the @extends/@implements clauses of the externs. */
  @Override
  protected boolean isJavaScriptClass(DeclaredTypeDescriptor typeDescriptor) {
    if (typeDescriptor.isNative()) {
      // Delegate to the standard Closure environment to decide whether a native type exists as a
      // JavaScript class/interface (it handles `*`, `?`, JsFunction, etc.).
      return super.isJavaScriptClass(typeDescriptor);
    }
    // For non-native types, this environment decides whether the type is exported (has externs)
    // or opaque.
    return JsExternsGenerator.shouldGenerateExtern(typeDescriptor);
  }

  @Override
  protected boolean needsExtendsJsDoc() {
    return true;
  }

  @Override
  protected boolean isConst(FieldDescriptor fieldDescriptor) {
    return fieldDescriptor.isFinal();
  }

  @Override
  protected String getJsEnumValueType(TypeDeclaration typeDeclaration) {
    return aliasForType(typeDeclaration) + "$value_type";
  }

  /**
   * Returns the type alias for the given type declaration.
   *
   * <p>For example, for a type with qualified name "j2wasm.CharUtils", this may give you
   * "j2wasm_CharUtils".
   */
  static String getJsTypeAlias(TypeDeclaration typeDeclaration) {
    return AstUtils.buildQualifiedName(
        computeJsAlias(typeDeclaration.getEnclosingModule()),
        typeDeclaration.getInnerTypeQualifier());
  }

  private static String computeJsAlias(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }
    return computeJsAlias(typeDeclaration.getQualifiedJsName());
  }

  public static String computeJsAlias(String qualifiedName) {
    return qualifiedName.replace('.', '_');
  }

  /**
   * Returns the Closure module required to reference {@code methodDescriptor} in the JS imports
   * file, or {@code null} if none is required.
   */
  @Nullable
  public static String getJsModuleDependency(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isExtern()) {
      return null;
    }
    if (methodDescriptor.hasJsNamespace()) {
      // Static native methods whose JS target lives in a different module than their declaring
      // Java class (e.g. `@JsMethod(namespace = "foo.Bar")`).
      return methodDescriptor.getJsNamespace();
    }
    // Otherwise the method belongs to its enclosing type; resolve to the Closure module root
    // (which strips inner-type qualifiers for nested native types).
    return methodDescriptor
        .getEnclosingTypeDescriptor()
        .getTypeDeclaration()
        .getEnclosingModule()
        .getQualifiedJsName();
  }
}
