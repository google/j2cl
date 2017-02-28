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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates unimplemented methods in abstract class.
 *
 * <p>An abstract class in Java may have unimplemented methods that are declared by its super
 * interfaces. However, in JavaScript, it requires a class tagged with 'implements' to implement all
 * of the methods defined by the interface. This class synthesizes the unimplemented methods in an
 * abstract class.
 */
public class UnimplementedMethodsCreator extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            // Only stub methods on abstract classes.
            if (!type.isAbstract()) {
              return;
            }

            Set<String> shadowedSignatures = new HashSet<>();

            for (MethodDescriptor methodDescriptor : type.getDeclaration().getMethodDescriptors()) {
              if (methodDescriptor.isConstructor() || methodDescriptor.isStatic()) {
                continue;
              }
              if (!methodDescriptor.getEnclosingClassTypeDescriptor().isInterface()) {
                continue;
              }

              addStubMethod(type, methodDescriptor);

              boolean isParameterized =
                  methodDescriptor != methodDescriptor.getDeclarationMethodDescriptor();
              boolean hasASpecializedSignature =
                  !methodDescriptor.isJsOverride(methodDescriptor.getDeclarationMethodDescriptor());
              if (isParameterized && hasASpecializedSignature) {
                String specializedSignature = methodDescriptor.getOverrideSignature();
                shadowedSignatures.add(specializedSignature);
              }
            }

            /**
             * Shadowing specialized methods can occur for example in the following scenario.
             *
             * <pre>
             * interface List<T> { String getFoo(T t); }
             * abstract class AbsList<T> implements List<T> {}
             * interface StringList extends List<String> { String getFoo(String string); }
             * abstract class AbsStringList extends AbsList<String> implements StringList {}
             * </pre>
             *
             * <p>In this case the declared methods on AbsStringList includes a "get(String)" that
             * was specialized from "List.get(T)". So a stub method is created for "get(Object)" but
             * the "get(String)" coming from StringList is not seen and so no stub is created.
             *
             * <p>The only reason this wasn't causing type check errors is because
             * BridgeMethodsCreator was filling that slot with an improper bridge.
             */
            ImmutableList<TypeDescriptor> interfaceTypeDescriptors =
                type.getDeclaration().getInterfaceTypeDescriptors();
            for (TypeDescriptor interfaceTypeDescriptor : interfaceTypeDescriptors) {
              for (MethodDescriptor methodDescriptor :
                  interfaceTypeDescriptor.getMethodDescriptors()) {
                if (methodDescriptor != methodDescriptor.getDeclarationMethodDescriptor()) {
                  continue;
                }
                if (!shadowedSignatures.contains(methodDescriptor.getOverrideSignature())) {
                  continue;
                }

                addStubMethod(type, methodDescriptor);
              }
            }
          }
        });
  }

  private static void addStubMethod(Type type, MethodDescriptor methodDescriptor) {
    MethodDescriptor stubMethodDescriptor =
        MethodDescriptor.Builder.from(methodDescriptor)
            .setEnclosingClassTypeDescriptor(type.getDeclaration().getUnsafeTypeDescriptor())
            .setAbstract(true)
            .build();
    type.addMethod(
        Method.newBuilder()
            .setMethodDescriptor(stubMethodDescriptor)
            .setParameters(
                AstUtils.createParameterVariables(
                    stubMethodDescriptor.getParameterTypeDescriptors()))
            .setIsOverride(true)
            .build());
  }
}
