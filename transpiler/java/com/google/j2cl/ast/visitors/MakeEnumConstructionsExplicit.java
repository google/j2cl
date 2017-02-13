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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.List;

/** Make the implicit parameters and super calls in enum constructors explicit. */
public class MakeEnumConstructionsExplicit extends NormalizationPass {
  private static final String ORDINAL_PARAMETER_NAME = "$ordinal";
  private static final String VALUE_NAME_PARAMETER_NAME = "$name";

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    final Multiset<TypeDescriptor> ordinalsByEnumTypeDescriptor = HashMultiset.create();

    // Rewrite method headers
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            return type.isEnumOrSubclass();
          }

          @Override
          public Node rewriteMethod(Method method) {
            /*
             * Only add parameters to constructor methods in Enum classes.
             */
            if (!method.isConstructor()) {
              return method;
            }
            return Method.Builder.from(method)
                .addParameters(
                    0,
                    Variable.newBuilder()
                        .setName(VALUE_NAME_PARAMETER_NAME)
                        .setTypeDescriptor(TypeDescriptors.get().javaLangString)
                        .setIsParameter(true)
                        .build(),
                    Variable.newBuilder()
                        .setName(ORDINAL_PARAMETER_NAME)
                        .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
                        .setIsParameter(true)
                        .build())
                .build();
          }
        });

    // Rewrite new instance calls
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            return type.isEnumOrSubclass();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            // Only add arguments to super() calls inside of constructor methods in Enum classes.
            if (!getCurrentMember().isConstructor() || !methodCall.getTarget().isConstructor()) {
              return methodCall;
            }

            List<Variable> methodParameters = ((Method) getCurrentMember()).getParameters();
            // Retrieve ordinal and name parameters from the method definition.
            Variable nameVariable = methodParameters.get(0);
            Variable ordinalVariable = methodParameters.get(1);
            return MethodCall.Builder.from(methodCall)
                .addArgumentsAndUpdateDescriptor(
                    0, nameVariable.getReference(), ordinalVariable.getReference())
                .build();
          }

          private MethodCall enumReplaceStringMethodCall(Expression nameVariable) {
            MethodDescriptor makeEnumNameMethodDescriptor =
                MethodDescriptor.newBuilder()
                    .setStatic(true)
                    .setJsInfo(JsInfo.RAW)
                    .setEnclosingClassTypeDescriptor(
                        TypeDescriptors.BootstrapType.NATIVE_UTIL.getDescriptor())
                    .setName(MethodDescriptor.MAKE_ENUM_NAME_METHOD_NAME)
                    .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
                    .setParameterTypeDescriptors(
                        Lists.newArrayList(TypeDescriptors.get().javaLangString))
                    .build();
            return MethodCall.Builder.from(makeEnumNameMethodDescriptor)
                .setArguments(nameVariable)
                .build();
          }

          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            // Rewrite newInstances for the creation of the enum constants to include the assigned
            // ordinal and name.
            if (!getCurrentType().isEnum()
                || !getCurrentMember().isField()
                || !((Field) getCurrentMember())
                    .getDescriptor()
                    .getTypeDescriptor()
                    .getTypeDeclaration()
                    .equals(getCurrentType().getDeclaration())) {

              // Enum constants creations are exactly those that are field initializers for fields
              // whose class is then enum class.
              return newInstance;
            }
            // This is definitely an enum initialization NewInstance.
            Field enumField = (Field) getCurrentMember();
            checkState(
                enumField != null,
                "Enum values can only be instantiated inside their field initialization");

            int currentOrdinal =
                ordinalsByEnumTypeDescriptor.add(
                    enumField.getDescriptor().getEnclosingClassTypeDescriptor(), 1);

            return NewInstance.Builder.from(newInstance)
                .addArgumentsAndUpdateDescriptor(
                    0,
                    enumReplaceStringMethodCall(
                        StringLiteral.fromPlainText(enumField.getDescriptor().getName())),
                    new NumberLiteral(TypeDescriptors.get().primitiveInt, currentOrdinal))
                .build();
          }
        });
  }
}
