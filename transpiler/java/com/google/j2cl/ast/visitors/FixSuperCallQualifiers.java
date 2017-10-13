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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;

/**
 * Set the qualifier of super calls that should have a qualifier.
 *
 * <p>For a super call that invokes a constructor of an inner class, it should have an qualifier.
 * For example,
 *
 * <pre>{@code
 * class A {
 *   class B {}
 *   class C extends B {
 *     public C() {
 *       super();
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>The super call in C's constructor invokes B's constructor, and the explicit qualifier should
 * be resolved.
 */
public class FixSuperCallQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // super class of {@code type} is an instance nested class.
            return type.getSuperTypeDescriptor() != null
                && type.getSuperTypeDescriptor().isCapturingEnclosingInstance();
          }

          @Override
          public boolean shouldProcessMethod(Method method) {
            return method.isConstructor();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor targetMethod = methodCall.getTarget();
            if (!targetMethod.isConstructor()
                || AstUtils.isDelegatedConstructorCall(
                    methodCall, getCurrentType().getTypeDescriptor())) {
              return methodCall;
            }
            // super() call.
            if (!AstUtils.hasThisReferenceAsQualifier(methodCall)) {
              // has an explicit qualifier.
              return methodCall;
            }
            return MethodCall.Builder.from(methodCall)
                .setQualifier(findSuperCallQualifier(getCurrentType().getTypeDescriptor()))
                .build();
          }

          private Expression findSuperCallQualifier(TypeDescriptor typeDescriptor) {
            TypeDescriptor superTypeDescriptor =
                checkNotNull(typeDescriptor.getSuperTypeDescriptor());
            TypeDescriptor outerTypeDescriptor =
                checkNotNull(superTypeDescriptor.getEnclosingTypeDescriptor());

            Expression qualifier = new ThisReference(typeDescriptor);
            TypeDescriptor currentTypeDescriptor = typeDescriptor;
            while (currentTypeDescriptor.getEnclosingTypeDescriptor() != null
                && !AstUtils.isSubType(currentTypeDescriptor, outerTypeDescriptor)) {
              qualifier =
                  FieldAccess.Builder.from(
                          AstUtils.getFieldDescriptorForEnclosingInstance(
                              currentTypeDescriptor,
                              currentTypeDescriptor.getEnclosingTypeDescriptor()))
                      .setQualifier(qualifier)
                      .build();
              currentTypeDescriptor = currentTypeDescriptor.getEnclosingTypeDescriptor();
            }
            return qualifier;
          }
        });
  }
}
