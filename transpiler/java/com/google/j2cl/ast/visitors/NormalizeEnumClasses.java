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
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/** Make the implicit parameters and super calls in enum constructors explicit. */
public class NormalizeEnumClasses extends NormalizationPass {
  private static final String ORDINAL_PARAMETER_NAME = "$ordinal";
  private static final String VALUE_NAME_PARAMETER_NAME = "$name";

  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    for (Type type : compilationUnit.getTypes()) {
      if (!type.isEnumOrSubclass()) {
        continue;
      }

      rewriteEnumConstructors(type);
      createEnumOrdinalConstants(type);
      rewriteEnumValueFieldsInitialization(type);
    }
  }

  /** Rewrites enum constructors to include parameters for the ordinal and name. */
  private static void rewriteEnumConstructors(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            // Only add parameters to constructor methods in Enum classes.
            if (!method.isConstructor()) {
              return method;
            }

            Variable nameParameter =
                Variable.newBuilder()
                    .setName(VALUE_NAME_PARAMETER_NAME)
                    .setTypeDescriptor(TypeDescriptors.get().javaLangString)
                    .setParameter(true)
                    .build();

            Variable ordinalParameter =
                Variable.newBuilder()
                    .setName(ORDINAL_PARAMETER_NAME)
                    .setTypeDescriptor(PrimitiveTypes.INT)
                    .setParameter(true)
                    .build();

            // rewrite the super() call in the method body to thread the orinal and name
            // parameters.
            method
                .getBody()
                .accept(
                    new AbstractRewriter() {
                      @Override
                      public Node rewriteMethodCall(MethodCall methodCall) {
                        if (!methodCall.getTarget().isConstructor()) {
                          return methodCall;
                        }

                        return MethodCall.Builder.from(methodCall)
                            .addArgumentsAndUpdateDescriptor(
                                0, nameParameter.getReference(), ordinalParameter.getReference())
                            .build();
                      }
                    });

            return Method.Builder.from(method)
                .addParameters(0, nameParameter, ordinalParameter)
                .build();
          }
        });
  }

  /** Creates constant static fields to hold the enum ordinal constants. */
  private static void createEnumOrdinalConstants(Type type) {
    int nextOrdinal = 0;
    List<Field> ordinalConstantFields = new ArrayList<>();
    for (Field enumField : type.getEnumFields()) {
      FieldDescriptor ordinalConstantFieldDescriptor =
          AstUtils.getEnumOrdinalConstantFieldDescriptor(enumField.getDescriptor());

      final int currentOrdinal = nextOrdinal++;
      // Create a constant field to hold the ordinal for the current enum value.
      ordinalConstantFields.add(
          Field.Builder.from(ordinalConstantFieldDescriptor)
              .setSourcePosition(enumField.getSourcePosition())
              .setInitializer(NumberLiteral.of(currentOrdinal))
              .build());
    }
    type.addFields(ordinalConstantFields);
  }

  /** Rewrites the initialization of the enum value fields with the right ordinal and name. */
  public static void rewriteEnumValueFieldsInitialization(Type type) {
    type.getEnumFields().forEach(NormalizeEnumClasses::rewriteEnumValueFieldInitialization);
  }

  private static void rewriteEnumValueFieldInitialization(Field enumField) {
    enumField.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            // There might be other new instances that appear in the enum initialization,
            // check that this is the right one. The instantiation can be of the enum
            // class or an "anonymous" subclass.
            if (!newInstance
                .getTypeDescriptor()
                .isAssignableTo(enumField.getDescriptor().getTypeDescriptor())) {
              return newInstance;
            }

            FieldDescriptor ordinalConstantFieldDescriptor =
                AstUtils.getEnumOrdinalConstantFieldDescriptor(enumField.getDescriptor());

            // Add the name and ordinal as first and second parameters when instantiating
            // the enum value.
            return NewInstance.Builder.from(newInstance)
                .addArgumentsAndUpdateDescriptor(
                    0,
                    enumReplaceStringMethodCall(
                        StringLiteral.fromPlainText(enumField.getDescriptor().getName())),
                    FieldAccess.Builder.from(ordinalConstantFieldDescriptor).build())
                .build();
          }
        });
  }

  private static MethodCall enumReplaceStringMethodCall(Expression nameVariable) {
    return RuntimeMethods.createUtilMethodCall("$makeEnumName", nameVariable);
  }
}
