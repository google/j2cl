/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates adapter classes for functional interfaces.
 *
 * <p>In general, the pass creates abstract classes that implement the functional interface to be
 * shared superclasses of all lambda implementors.
 */
public class AddFunctionalInterfaceAdaptors extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    List<Type> functionalInterfaceAdaptors = new ArrayList<>();
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            if (!typeDeclaration.isFunctionalInterface()
                // JsFunction implementations extend the common JsFunctionAdaptor instead of having
                // individual adapters generated.
                || typeDeclaration.isJsFunctionInterface()
                // Native interfaces cannot be implemented in Wasm.
                || typeDeclaration.isNative()) {
              return;
            }

            DeclaredTypeDescriptor adaptorTypeDescriptor =
                LambdaAdaptorTypeDescriptors.createFunctionalInterfaceAdaptorTypeDescriptor(
                    type.getTypeDescriptor());
            Type adaptorType =
                Type.builder()
                    .setSourcePosition(type.getSourcePosition())
                    .setTypeDeclaration(adaptorTypeDescriptor.getTypeDeclaration())
                    .build();

            functionalInterfaceAdaptors.add(adaptorType);
          }
        });
    compilationUnit.addTypes(functionalInterfaceAdaptors);
  }
}
