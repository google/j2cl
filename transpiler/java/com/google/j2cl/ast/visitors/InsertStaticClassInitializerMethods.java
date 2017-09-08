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
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.common.SourcePosition;
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
            if (needsClinitCall(method.getDescriptor())) {
              return Method.Builder.from(method)
                  .addStatement(
                      0,
                      newClinitCallStatement(
                          method.getBody().getSourcePosition(),
                          method.getDescriptor().getEnclosingTypeDescriptor()))
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

            if (hasClinitMethod(type.getSuperTypeDescriptor())) {
              superClinitCallStatements.add(
                  newClinitCallStatement(type.getSourcePosition(), type.getSuperTypeDescriptor()));
            }
            addRequiredSuperInterfacesClinitCalls(
                type.getSourcePosition(),
                type.getDeclaration().getUnsafeTypeDescriptor(),
                superClinitCallStatements);

            if (!superClinitCallStatements.isEmpty()) {
              type.addStaticInitializerBlock(
                  0, new Block(type.getSourcePosition(), superClinitCallStatements));
            }
          }
        });
  }

  private static Statement newClinitCallStatement(
      SourcePosition sourcePosition, TypeDescriptor typeDescriptor) {
    MethodDescriptor clinitMethodDescriptor = AstUtils.getClinitMethodDescriptor(typeDescriptor);
    return MethodCall.Builder.from(clinitMethodDescriptor).build().makeStatement(sourcePosition);
  }

  private void addRequiredSuperInterfacesClinitCalls(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      List<Statement> superClinitCallStatements) {
    for (TypeDescriptor interfaceTypeDescriptor : typeDescriptor.getInterfaceTypeDescriptors()) {
      if (!hasClinitMethod(interfaceTypeDescriptor)) {
        continue;
      }

      if (interfaceTypeDescriptor.getTypeDeclaration().declaresDefaultMethods()) {
        // The interface declares a default method; invoke its clinit which will initialize
        // the interface and all it superinterfaces that declare default methods.
        superClinitCallStatements.add(
            newClinitCallStatement(sourcePosition, interfaceTypeDescriptor));
        continue;
      }

      // The interface does not declare a default method so don't call its clinit; instead recurse
      // into its super interface hierarchy to invoke clinits of super interfaces that declare
      // default methods.
      addRequiredSuperInterfacesClinitCalls(
          sourcePosition, interfaceTypeDescriptor, superClinitCallStatements);
    }
  }

  private static boolean needsClinitCall(MethodDescriptor methodDescriptor) {
    // Skip native methods.
    if (methodDescriptor.isNative()) {
      return false;
    }

    // At this point all constructors (except JsConstructors) are static factory ($create) methods,
    // so clinits need to be inserted only on static methods or JsConstructors. clinits are not
    // needed in factories or JsConstructors for enum classes because those are already only called
    // from the clinit itself.

    if (methodDescriptor.getEnclosingTypeDescriptor().isEnum()
        && methodDescriptor.getMethodOrigin() == MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR) {
      // These are the classes corresponding to enums excluding the anonymous enum subclasses.
      return false;
    }

    return methodDescriptor.isStatic() || methodDescriptor.isJsConstructor();
  }

  private static boolean hasClinitMethod(TypeDescriptor typeDescriptor) {
    return typeDescriptor != null
        && !typeDescriptor.isNative()
        && !typeDescriptor.isJsFunctionInterface();
  }
}
