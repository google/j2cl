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
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;

/** A Closure environment for generating externs and resolving type names. */
class WasmJsInteropClosureGenerationEnvironment extends ClosureGenerationEnvironment {
  public WasmJsInteropClosureGenerationEnvironment() {
    super(ImmutableSet.of(), ImmutableMap.of());
  }

  @Override
  public String aliasForType(TypeDeclaration typeDeclaration) {
    return getJsTypeAlias(typeDeclaration);
  }

  @Override
  protected boolean isJavaScriptClass(DeclaredTypeDescriptor typeDescriptor) {
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

  /** Returns the set of Closure modules that are required to resolve the given type. */
  public static ImmutableSet<String> getJsModuleDependencies(Type type) {
    // TODO(b/450097012): Add modules for parent types and interfaces.
    ImmutableSet.Builder<String> moduleDeps = ImmutableSet.builder();
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMember(Member method) {
            if (!method.getDescriptor().canBeReferencedExternally()) {
              return;
            }
            collectModuleDependencies(moduleDeps, method.getDescriptor());
          }
        });
    return moduleDeps.build();
  }

  /**
   * Returns the set of Closure modules that are required to resolve the given method's parameter
   * and return types.
   */
  public static ImmutableSet<String> getJsModuleDependencies(MethodDescriptor methodDescriptor) {
    ImmutableSet.Builder<String> moduleDeps = ImmutableSet.builder();
    collectModuleDependencies(moduleDeps, methodDescriptor);
    return moduleDeps.build();
  }

  private static void collectModuleDependencies(
      ImmutableSet.Builder<String> moduleDeps, MemberDescriptor memberDescriptor) {
    switch (memberDescriptor) {
      case MethodDescriptor methodDescriptor -> {
        // Collect module dependencies for types appearing in the parameter list.
        methodDescriptor
            .getParameterTypeDescriptors()
            .forEach(p -> collectModuleDependencies(moduleDeps, p));

        if (methodDescriptor.isExtern()) {
          // If the method is an extern there is nothing else to collect.
          break;
        }

        if (methodDescriptor.hasJsNamespace()) {
          moduleDeps.add(methodDescriptor.getJsNamespace());
        } else {
          collectModuleDependencies(moduleDeps, methodDescriptor.getEnclosingTypeDescriptor());
        }
      }
      case FieldDescriptor fieldDescriptor ->
          collectModuleDependencies(moduleDeps, fieldDescriptor.getTypeDescriptor());

      default -> throw new AssertionError("Unexpected MemberDescriptor: " + memberDescriptor);
    }
  }

  private static void collectModuleDependencies(
      ImmutableSet.Builder<String> moduleDeps, TypeDescriptor typeDescriptor) {
    if (!(typeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor)) {
      return;
    }
    TypeDeclaration typeDeclaration = declaredTypeDescriptor.getTypeDeclaration();
    if (!(typeDeclaration.isNative() || typeDeclaration.isJsType()) || typeDeclaration.isExtern()) {
      return;
    }
    moduleDeps.add(typeDeclaration.getEnclosingModule().getQualifiedJsName());
    for (TypeDescriptor t : declaredTypeDescriptor.getTypeArgumentDescriptors()) {
      collectModuleDependencies(moduleDeps, t);
    }
  }
}
