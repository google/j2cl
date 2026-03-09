/*
 * Copyright 2026 Google Inc.
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
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Synthesizes method overrides required by Kotlin for standard library methods that are concrete or
 * default in Java but abstract in Kotlin.
 *
 * <p>Specifically:
 *
 * <ul>
 *   <li>Inherited byteValue() and shortValue() for classes extending java.lang.Number.
 *   <li>remove() for classes implementing java.util.Iterator.
 * </ul>
 */
public class OverrideKotlinAdditionalAbstractMethods extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Type rewriteType(Type type) {
            if (type.isNative()) {
              return type;
            }

            if (extendsNumber(type)) {
              synthesizeNumberOverrides(type);
            }

            if (implementsIterator(type)) {
              synthesizeIteratorRemoveOverride(type);
            }

            return type;
          }
        });
  }

  private static boolean extendsNumber(Type type) {
    TypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    return !type.isInterface()
        && superTypeDescriptor != null
        && TypeDescriptors.isJavaLangNumber(superTypeDescriptor);
  }

  private static boolean implementsIterator(Type type) {
    return type.getTypeDescriptor().isSubtypeOf(TypeDescriptors.get().javaUtilIterator)
        || type.getTypeDescriptor().isSubtypeOf(TypeDescriptors.get().javaUtilListIterator);
  }

  private void synthesizeNumberOverrides(Type type) {
    synthesizeNumberOverride(type, "byteValue", "toByte", PrimitiveTypes.BYTE);
    synthesizeNumberOverride(type, "shortValue", "toShort", PrimitiveTypes.SHORT);
  }

  private void synthesizeNumberOverride(
      Type type, String methodName, String ktName, PrimitiveTypeDescriptor returnType) {
    if (type.containsMethod(
        m -> m.getName().equals(methodName) && m.getParameterTypeDescriptors().isEmpty())) {
      return;
    }

    MethodDescriptor intValueMethodDescriptor =
        type.getTypeDescriptor().getMethodDescriptor("intValue");

    MethodDescriptor methodDescriptor =
        MethodDescriptor.Builder.from(intValueMethodDescriptor)
            .setName(methodName)
            .setOriginalKtInfo(KtInfo.newBuilder().setName(ktName).build())
            .setReturnTypeDescriptor(returnType)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setAbstract(false)
            .setNative(false)
            .build();

    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(methodDescriptor)
            .setForcedJavaOverride(true)
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(
                        CastExpression.newBuilder()
                            .setExpression(
                                MethodCall.Builder.from(intValueMethodDescriptor).build())
                            .setCastTypeDescriptor(returnType)
                            .build())
                    .setSourcePosition(type.getSourcePosition())
                    .build())
            .setSourcePosition(type.getSourcePosition())
            .build());
  }

  private void synthesizeIteratorRemoveOverride(Type type) {
    MethodDescriptor iteratorRemoveDescriptor =
        TypeDescriptors.get().javaUtilIterator.getMethodDescriptor("remove");

    MethodDescriptor existingRemove = type.getTypeDescriptor().getMethodDescriptor("remove");
    if (existingRemove != null && !existingRemove.isSameMethod(iteratorRemoveDescriptor)) {
      // Either we have an override already, or a suitable method in a supertype (but exclude the
      // default method from the Iterator supertype).
      return;
    }

    MethodDescriptor removeMethodDescriptor =
        MethodDescriptor.Builder.from(iteratorRemoveDescriptor)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setDefaultMethod(type.isInterface())
            .setAbstract(false)
            .setNative(false)
            .build();

    MethodDescriptor exceptionConstructor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangUnsupportedOperationException)
            .setConstructor(true)
            .setReturnTypeDescriptor(PrimitiveTypes.VOID)
            .build();

    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(removeMethodDescriptor)
            .setForcedJavaOverride(true)
            .addStatements(
                ThrowStatement.newBuilder()
                    .setExpression(NewInstance.Builder.from(exceptionConstructor).build())
                    .setSourcePosition(type.getSourcePosition())
                    .build())
            .setSourcePosition(type.getSourcePosition())
            .build());
  }
}
