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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/** Creates the devirtualized methods for devirtualized boxed types. */
public class DevirtualizeBoxedTypesAndJsFunctionImplementations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // Creates devirtualized static methods for the boxed types (Boolean, Double, String).
            return TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())
                || type.getDeclaration().isJsFunctionImplementation();
          }

          @Override
          public Method rewriteMethod(Method method) {
            if (!shouldDevirtualize(method)) {
              return method;
            }

            // Add the static method to current type.
            // NOTE: The added method will be traversed, and will be skipped.
            getCurrentType().addMethod(AstUtils.createDevirtualizedMethod(method));

            return null;
          }

          private boolean shouldDevirtualize(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!methodDescriptor.isPolymorphic()) {
              return false;
            }

            TypeDescriptor enclosingTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();

            if (methodDescriptor.isJsFunction()) {
              // If the JsFunction method has different method signature from the SAM method, it
              // should be devirtualized.
              return !AstUtils.areParameterErasureEqual(
                  methodDescriptor,
                  enclosingTypeDescriptor.getFunctionalInterface().getJsFunctionMethodDescriptor());
            }
            return true;
          }
        });
  }
}
