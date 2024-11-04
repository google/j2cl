/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/**
 * Inserts a cast for the return type of methods where Java might have inferred the return type
 * based on the usage site. This avoids a potential type mistmatch error in JSCompiler due to the
 * combination of its type inference algoritm and its invariant generics semantics, which are
 * different from Java.
 */
public class InsertTypeAnnotationOnGenericReturnTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDeclaration = methodCall.getTarget().getDeclarationDescriptor();
            // Type variable should be declared in method to trigger inference.
            boolean methodDeclaresTypeVariables =
                !methodDeclaration.getTypeParameterTypeDescriptors().isEmpty();
            TypeDescriptor returnTypeDescriptor = methodDeclaration.getReturnTypeDescriptor();
            // Type variable should be used in method return type and return type should a generic
            // type for inference to matter (as mismatches becomes an issue due to invariant generic
            // type parameters in OTI).
            boolean methodReturnHasTypeVariables =
                !returnTypeDescriptor.getAllTypeVariables().isEmpty();
            boolean methodReturnIsGenericType =
                returnTypeDescriptor instanceof DeclaredTypeDescriptor
                    && ((DeclaredTypeDescriptor) returnTypeDescriptor).hasTypeArguments();

            // If the return is not inferred and specialized in Java there is nothing to fixup.
            boolean isReturnSpecialized =
                !methodDeclaration.getReturnTypeDescriptor().equals(methodCall.getTypeDescriptor());
            // In reality, for an inference mismatch to occur, type variable used in return should
            // be declared by the method. However there is no easy way to check that right now so we
            // are approximating here since extra casts does only hurt uncompiled size.
            if (methodDeclaresTypeVariables
                && methodReturnHasTypeVariables
                && methodReturnIsGenericType
                && isReturnSpecialized) {
              return JsDocCastExpression.newBuilder()
                  .setExpression(methodCall)
                  .setCastTypeDescriptor(methodCall.getTypeDescriptor())
                  .build();
            }
            return methodCall;
          }
        });
  }
}
