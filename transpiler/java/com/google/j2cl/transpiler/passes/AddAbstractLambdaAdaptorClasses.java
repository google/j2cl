/*
 * Copyright 2024 Google Inc.
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
 * Creates abstract class that implement functional interfaces that can be shared superclasses of
 * all lambda implementors.
 */
public class AddAbstractLambdaAdaptorClasses extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    List<Type> abstractLambdaAdaptors = new ArrayList<>();
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            if (typeDeclaration.isFunctionalInterface()
                // Native interfaces cannot be implemented in Wasm.
                && !typeDeclaration.isNative()) {
              DeclaredTypeDescriptor abstractLambdaAdaptor =
                  LambdaAdaptorTypeDescriptors.createAbstractLambdaAdaptorTypeDescriptor(
                      type.getTypeDescriptor());
              abstractLambdaAdaptors.add(
                  new Type(type.getSourcePosition(), abstractLambdaAdaptor.getTypeDeclaration()));
            }
          }
        });
    compilationUnit.addTypes(abstractLambdaAdaptors);
  }
}
