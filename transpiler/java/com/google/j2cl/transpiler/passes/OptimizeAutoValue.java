/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Optimize AutoValue generated classes to reduce their code size. */
public class OptimizeAutoValue extends NormalizationPass {

  private final boolean enabled;

  public OptimizeAutoValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void applyTo(Type type) {
    if (!enabled) {
      return;
    }

    // Change the parent type of AutoValue class to ValueType.
    if (canExtendValueType(type.getDeclaration())) {
      type.setSuperTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType);
      return;
    }

    DeclaredTypeDescriptor superType = type.getSuperTypeDescriptor();
    if (superType == null || !superType.getTypeDeclaration().isAnnotatedWithAutoValue()) {
      return;
    }

    optimizeAutoValueImplementationType(type, superType);
  }

  private static void optimizeAutoValueImplementationType(
      Type type, DeclaredTypeDescriptor autoValueType) {
    if (TypeDescriptors.isJavaLangObject(autoValueType.getSuperTypeDescriptor())) {
      // This is already optimized to extend Value Type, no mixin needed.
    } else {
      // Add mixin that will provide the implementation for java.lang.Object.
      int mask = calculateJavaLangObjectMethodMask(type);
      addValueTypeMixin(type, mask);
    }

    // Now safely remove existing implementation methods.
    type.getMembers().removeIf(m -> m.getDescriptor().isOrOverridesJavaLangObjectMethod());
  }

  private static boolean canExtendValueType(TypeDeclaration declaration) {
    return declaration.isAnnotatedWithAutoValue()
        && TypeDescriptors.isJavaLangObject(declaration.getSuperTypeDescriptor());
  }

  private static int calculateJavaLangObjectMethodMask(Type type) {
    int mask = 0;
    for (Method method : type.getMethods()) {
      if (!method.getDescriptor().isOrOverridesJavaLangObjectMethod()) {
        continue;
      }
      switch (method.getDescriptor().getName()) {
        case "equals":
          mask |= 1;
          break;
        case "hashCode":
          mask |= 2;
          break;
        case "toString":
          mask |= 4;
          break;
        default:
          throw new AssertionError(method.getDescriptor().getName());
      }
    }
    return mask;
  }

  private static void addValueTypeMixin(Type type, int mask) {
    MethodDescriptor mixinMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType)
            .setName("mixin")
            .setParameterTypeDescriptors(TypeDescriptors.get().nativeFunction, PrimitiveTypes.INT)
            .build();
    MethodCall mixinCall =
        MethodCall.Builder.from(mixinMethodDescriptor)
            .setArguments(
                new JavaScriptConstructorReference(type.getDeclaration()),
                NumberLiteral.fromInt(mask))
            .build();
    type.addLoadTimeStatement(mixinCall.makeStatement(type.getSourcePosition()));
  }
}
