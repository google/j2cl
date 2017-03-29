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
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.ArrayList;
import java.util.List;

/** The first line of a static method should be a call to the Class's clinit. */
public class InsertStaticClassInitializerMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Add clinit calls to static methods and (real js) constructors.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            boolean isStaticMethod = method.getDescriptor().isStatic();
            boolean isJsConstructor = method.getDescriptor().isJsConstructor();
            if (isStaticMethod || isJsConstructor) {
              return Method.Builder.from(method)
                  .addStatement(
                      0,
                      newClinitCallStatement(method.getDescriptor().getEnclosingTypeDescriptor()))
                  .build();
            }
            return method;
          }
        });

    // Synthesize a static initializer block that calls the necessary super type clinits.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            List<Statement> superClinitCallStatements = new ArrayList<>();

            if (needsClinitCall(type.getSuperTypeDescriptor())) {
              superClinitCallStatements.add(newClinitCallStatement(type.getSuperTypeDescriptor()));
            }
            addRequiredSuperInterfacesClinitCalls(
                type.getDeclaration().getUnsafeTypeDescriptor(), superClinitCallStatements);

            if (!superClinitCallStatements.isEmpty()) {
              type.addStaticInitializerBlock(0, new Block(superClinitCallStatements));
            }
          }
        });
  }

  private static Statement newClinitCallStatement(TypeDescriptor typeDescriptor) {
    MethodDescriptor clinitMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setStatic(true)
            .setEnclosingTypeDescriptor(typeDescriptor)
            .setName("$clinit")
            .setJsInfo(JsInfo.RAW)
            .build();
    return MethodCall.Builder.from(clinitMethodDescriptor).build().makeStatement();
  }

  private void addRequiredSuperInterfacesClinitCalls(
      TypeDescriptor typeDescriptor, List<Statement> superClinitCallStatements) {
    for (TypeDescriptor interfaceTypeDescriptor : typeDescriptor.getInterfaceTypeDescriptors()) {
      if (!needsClinitCall(interfaceTypeDescriptor)) {
        continue;
      }

      if (interfaceTypeDescriptor.getTypeDeclaration().declaresDefaultMethods()) {
        // The interface declares a default method; invoke its clinit which will initialize
        // the interface and all it superinterfaces that declare default methods.
        superClinitCallStatements.add(newClinitCallStatement(interfaceTypeDescriptor));
        continue;
      }

      // The interface does not declare a default method so don't call its clinit; instead recurse
      // into its super interface hierarchy to invoke clinits of super interfaces that declare
      // default methods.
      addRequiredSuperInterfacesClinitCalls(interfaceTypeDescriptor, superClinitCallStatements);
    }
  }

  private static boolean needsClinitCall(TypeDescriptor typeDescriptor) {
    return typeDescriptor != null
        && !typeDescriptor.isNative()
        && !typeDescriptor.isJsFunctionInterface();
  }
}
