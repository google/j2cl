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
package com.google.j2cl.transpiler.passes;

import static java.util.stream.Collectors.toMap;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.Map;

/** Make the implicit parameters and super calls in enum constructors explicit. */
public class NormalizeEnumClasses extends NormalizationPass {
  private static final String ORDINAL_PARAMETER_NAME = "$ordinal";
  private static final String VALUE_NAME_PARAMETER_NAME = "$name";

  private final boolean useMakeEnumNameIndirection;

  public NormalizeEnumClasses() {
    this(true);
  }

  public NormalizeEnumClasses(boolean useMakeEnumNameIndirection) {
    this.useMakeEnumNameIndirection = useMakeEnumNameIndirection;
  }

  @Override
  public void applyTo(Type type) {
    if (!type.isEnumOrSubclass() || type.isNative()) {
      return;
    }
    rewriteEnumConstructors(type);
    createEnumOrdinalConstants(type);
    addImplicitParametersToInstantiations(type);
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

            // Rewrite the super() call in the constructor body to thread the ordinal and name
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
                                0,
                                nameParameter.createReference(),
                                ordinalParameter.createReference())
                            .build();
                      }
                    });

            if (type.isEnum()) {
              // Initialize name and ordinal fields.
              // TODO(b/74986525): revert to initialization of these fields in superclass
              // once removal of unused values is guaranteed or more stable.
              initJavaLangEnumField(method, "ordinal", ordinalParameter.createReference());
              initJavaLangEnumField(method, "name", nameParameter.createReference());
            }

            return Method.Builder.from(method)
                .addParameters(0, nameParameter, ordinalParameter)
                .build();
          }
        });
  }

  private static void initJavaLangEnumField(
      Method method, String fieldName, VariableReference variableReference) {

    FieldDescriptor fieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangEnum)
            .setName(fieldName)
            .setVisibility(Visibility.PRIVATE)
            .setTypeDescriptor(variableReference.getTypeDescriptor())
            .build();

    method
        .getBody()
        .getStatements()
        .add(
            0,
            BinaryExpression.Builder.asAssignmentTo(fieldDescriptor)
                .setRightOperand(variableReference)
                .build()
                .makeStatement(method.getSourcePosition()));
  }

  /** Creates constant static fields to hold the enum ordinal constants. */
  private static void createEnumOrdinalConstants(Type type) {
    // Traverse in reverse order so that the ordinal constant fields are inserted in ascending
    // ordinal order.
    for (Field enumField : type.getEnumFields().reverse()) {

      FieldDescriptor enumFieldDescriptor = enumField.getDescriptor();
      // Create a constant field to hold the ordinal for the current enum value.
      type.addMember(
          // These fields need to be defined at the beginning because they can be referenced by enum
          // constant initializers that are already part of the load time statements.
          0,
          Field.Builder.from(AstUtils.getEnumOrdinalConstantFieldDescriptor(enumFieldDescriptor))
              .setSourcePosition(enumField.getSourcePosition())
              .setInitializer(enumFieldDescriptor.getEnumOrdinalValue())
              .build());
    }
  }

  /** Rewrites the call to the enum constructor with the right ordinal and name. */
  public void addImplicitParametersToInstantiations(Type type) {
    // Collect member descriptors that enclose the instantiation of enum constants. These might be
    // directly in the initializer of the field or in a synthetic method if the initialization was
    // decomposed by the Kotlin frontend.
    Map<MemberDescriptor, FieldDescriptor> enclosingInitializingMemberDescriptorByEnumField =
        type.getEnumFields().stream()
            .collect(
                toMap(
                    (Field e) -> {
                      if (e.getInitializer() instanceof MethodCall initializer) {
                        // In Kotlin, we may have decomposed some statements used as expression in
                        // the enum constructor call. The call to the constructor has been wrapped
                        // in an init function executing the statement before the call to the
                        // enum constructor.
                        return initializer.getTarget();
                      }
                      // Constructor call is done in the field initialization.
                      return e.getDescriptor();
                    },
                    Field::getDescriptor));

    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            FieldDescriptor enumFieldDescriptor =
                enclosingInitializingMemberDescriptorByEnumField.get(
                    getCurrentMember().getDescriptor());
            if (enumFieldDescriptor == null) {
              // We are not in a member initializing an enum field.
              return newInstance;
            }

            // There might be other new instances that appear in the enclosing member,
            // check that this is the right one. The instantiation can be of the enum
            // class or an "anonymous" subclass.
            if (!newInstance
                .getTypeDescriptor()
                .isAssignableTo(enumFieldDescriptor.getTypeDescriptor())) {
              return newInstance;
            }

            FieldDescriptor ordinalConstantFieldDescriptor =
                AstUtils.getEnumOrdinalConstantFieldDescriptor(enumFieldDescriptor);

            // Add the name and ordinal as first and second parameters when instantiating
            // the enum value.
            return NewInstance.Builder.from(newInstance)
                .addArgumentsAndUpdateDescriptor(
                    0,
                    enumReplaceStringMethodCall(new StringLiteral(enumFieldDescriptor.getName())),
                    FieldAccess.Builder.from(ordinalConstantFieldDescriptor).build())
                .build();
          }
        });
  }

  private Expression enumReplaceStringMethodCall(Expression nameVariable) {
    return useMakeEnumNameIndirection
        ? RuntimeMethods.createUtilMethodCall("$makeEnumName", nameVariable)
        : nameVariable;
  }
}
