/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;

/**
 * Creates Java's implicit equals and hashCode methods in top-most interfaces.
 *
 * <p>This gives JS users the same experience as Java users in that they can treat variables that
 * are typed as some interface as being able to dispatch equals() and hashCode(), without the user
 * having to know about the existence of Objects.js' trampolining.
 */
public class NormalizeDefaultInterfaceMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Visitor());
  }

  private static class Visitor extends AbstractVisitor {
    MethodDescriptor equalsMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setMethodName(MethodDescriptor.EQUALS_METHOD_NAME)
            .setIsAbstract(true)
            .addParameter(TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
            .build();

    MethodDescriptor hashCodeMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setMethodName(MethodDescriptor.HASH_CODE_METHOD_NAME)
            .setIsAbstract(true)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveInt)
            .build();

    @Override
    public boolean enterType(Type type) {
      TypeDescriptor typeDescriptor = type.getDescriptor();
      // Only add the methods to interfaces.
      if (!typeDescriptor.isInterface()) {
        return false;
      }
      // Only add the methods if this interface is top most.
      if (!type.getSuperInterfaceTypeDescriptors().isEmpty()
          || type.getSuperTypeDescriptor() != null) {
        return false;
      }
      // Don't add the methods to native or JsFunction interfaces since they are not conceptually
      // "Java" types.
      if (typeDescriptor.isNative() || typeDescriptor.isJsFunctionInterface()) {
        return false;
      }

      // Only add the equals method if it isn't already present.
      if (!type.containsMethod(ManglingNameUtils.getMangledName(equalsMethodDescriptor))) {
        Variable objParameter =
            new Variable("obj", TypeDescriptors.get().javaLangObject, false, true);
        type.addMethod(
            Method.Builder.fromDefault()
                .addParameters(objParameter)
                .setIsAbstract(true)
                .setMethodDescriptor(equalsMethodDescriptor)
                .setEnclosingClass(type.getDescriptor())
                .build());
      }

      // Only add the equals method if it isn't already present.
      if (!type.containsMethod(ManglingNameUtils.getMangledName(hashCodeMethodDescriptor))) {
        type.addMethod(
            Method.Builder.fromDefault()
                .setIsAbstract(true)
                .setMethodDescriptor(hashCodeMethodDescriptor)
                .setEnclosingClass(type.getDescriptor())
                .build());
      }

      return false;
    }
  }
}
