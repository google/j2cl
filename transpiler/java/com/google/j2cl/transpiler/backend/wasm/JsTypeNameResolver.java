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
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;

/** A minimal Closure environment for resolving type names. */
final class JsTypeNameResolver extends ClosureGenerationEnvironment {
  public JsTypeNameResolver() {
    super(ImmutableSet.of(), ImmutableMap.of());
  }

  @Override
  public String aliasForType(TypeDeclaration typeDeclaration) {
    return getJsTypeAlias(typeDeclaration);
  }

  /**
   * Returns the type alias for the given type declaration.
   *
   * <p>For example, for a type with qualified name "j2wasm.CharUtils", this may give you
   * "j2wasm_CharUtils".
   */
  public static String getJsTypeAlias(TypeDeclaration typeDeclaration) {
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

  public static String computeJsAlias(String name) {
    return name.replace('.', '_');
  }

  /** Returns the set of Closure modules that are required to resolve the given type. */
  public static ImmutableSet<String> getJsModuleDependencies(Type type) {
    // TODO(b/450097012): Add modules for parent types and interfaces.
    ImmutableSet.Builder<String> moduleDeps = ImmutableSet.builder();
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            if (!method.getDescriptor().canBeReferencedExternally()) {
              return;
            }
            collectModuleDependencies(moduleDeps, method.getDescriptor());
          }

          @Override
          public void exitField(Field field) {
            if (!field.getDescriptor().canBeReferencedExternally()) {
              return;
            }
            collectModuleDependencies(moduleDeps, field.getDescriptor());
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
      ImmutableSet.Builder<String> moduleDeps, MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.isExtern()) {
      if (methodDescriptor.hasJsNamespace()) {
        moduleDeps.add(methodDescriptor.getJsNamespace());
      } else {
        collectModuleDependencies(moduleDeps, methodDescriptor.getEnclosingTypeDescriptor());
      }
    }

    methodDescriptor
        .getParameterTypeDescriptors()
        .forEach(p -> collectModuleDependencies(moduleDeps, p));
  }

  private static void collectModuleDependencies(
      ImmutableSet.Builder<String> moduleDeps, FieldDescriptor fieldDescriptor) {
    collectModuleDependencies(moduleDeps, fieldDescriptor.getTypeDescriptor());
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
