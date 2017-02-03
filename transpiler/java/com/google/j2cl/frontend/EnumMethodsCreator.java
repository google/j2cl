/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.frontend;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldAccess.Builder;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.common.JsUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class generates the AST structure for the synthesized static methods values and valueOf on
 * Enum types. Additionally, we add a private static field "namesToValuesMap" which is created the
 * first time valueOf() is called and allows for quick lookup of Enum values by (String) name.
 */
public class EnumMethodsCreator {
  private static final String VALUE_OF_METHOD_NAME = "valueOf";
  private static final String VALUES_METHOD_NAME = "values";
  private static final String NAMES_TO_VALUES_MAP_FIELD_NAME = "namesToValuesMap";
  private static final String CREATE_MAP_METHOD_NAME = "createMapFromValues";
  private static final String GET_VALUE_METHOD_NAME = "getValueFromNameAndMap";

  private Type enumType;
  private FieldDescriptor namesToValuesMapFieldDescriptor;
  private MethodDescriptor valuesMethodDescriptor;
  private MethodDescriptor valueOfMethodDescriptor;

  public static void applyTo(Type enumType) {
    checkArgument(enumType.isEnum());
    EnumMethodsCreator instance = new EnumMethodsCreator(enumType);
    instance.run();
  }

  private EnumMethodsCreator(Type enumType) {
    boolean jsType = enumType.getDescriptor().isJsType();
    TypeDescriptor enumTypeDescriptor = enumType.getDescriptor().getUnsafeTypeDescriptor();

    this.enumType = enumType;
    this.namesToValuesMapFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingClassTypeDescriptor(enumTypeDescriptor)
            .setName(NAMES_TO_VALUES_MAP_FIELD_NAME)
            .setTypeDescriptor(
                TypeDescriptors.createNative(
                    // Browser global
                    JsUtils.JS_PACKAGE_GLOBAL,
                    // Native type name
                    "Object",
                    // Type parameters.
                    Lists.newArrayList(
                        TypeDescriptors.get().javaLangString,
                        TypeDescriptors.toNonNullable(enumTypeDescriptor))))
            .setStatic(true)
            .setVisibility(Visibility.PRIVATE)
            .build();
    this.valuesMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setStatic(true)
            .setEnclosingClassTypeDescriptor(enumTypeDescriptor)
            .setName(VALUES_METHOD_NAME)
            .setReturnTypeDescriptor(TypeDescriptors.getForArray(enumTypeDescriptor, 1))
            .setParameterTypeDescriptors()
            .setJsInfo(jsType ? JsInfo.RAW : JsInfo.NONE)
            .build();
    this.valueOfMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setStatic(true)
            .setEnclosingClassTypeDescriptor(enumTypeDescriptor)
            .setName(VALUE_OF_METHOD_NAME)
            .setReturnTypeDescriptor(enumTypeDescriptor)
            .setParameterTypeDescriptors(TypeDescriptors.get().javaLangString)
            .setJsInfo(jsType ? JsInfo.RAW : JsInfo.NONE)
            .build();
  }

  private void run() {
    enumType.addField(
        Field.Builder.from(this.namesToValuesMapFieldDescriptor)
            .setInitializer(NullLiteral.NULL)
            .build());
    Method valueOfMethod = createValueOfMethod();
    valueOfMethod.setSourcePosition(enumType.getSourcePosition());
    enumType.addMethod(valueOfMethod);
    Method valuesMethod = createValuesMethod();
    valuesMethod.setSourcePosition(enumType.getSourcePosition());
    enumType.addMethod(valuesMethod);
  }

  /**
   * Creates the ast needed for valueOf(String name) which is of the form: <code>
   * private Object namesToValuesMap = null;
   * public static EnumType valueOf(String name) {
   *   if(namesToValuesMap == null){
   *     namesToValuesMap = Enums.createMapFromValues(this.values());
   *   }
   *   return Enums.getValueFromNameAndMap(name, namesToValuesMap);
   * }
   * </code>
   */
  private Method createValueOfMethod() {
    Variable nameParameter =
        Variable.newBuilder()
            .setName("name")
            .setTypeDescriptor(TypeDescriptors.get().javaLangString)
            .setIsParameter(true)
            .build();

    MethodDescriptor createMapMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ENUMS.getDescriptor())
            .setName(CREATE_MAP_METHOD_NAME)
            .setReturnTypeDescriptor(namesToValuesMapFieldDescriptor.getTypeDescriptor())
            .setParameterTypeDescriptors(enumType.getDescriptor().getUnsafeTypeDescriptor())
            .build();
    MethodDescriptor getMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ENUMS.getDescriptor())
            .setName(GET_VALUE_METHOD_NAME)
            .setReturnTypeDescriptor(enumType.getDescriptor().getUnsafeTypeDescriptor())
            .setParameterTypeDescriptors(
                nameParameter.getTypeDescriptor(),
                namesToValuesMapFieldDescriptor.getTypeDescriptor())
            .build();

    // If statement
    Expression namesToValuesMapIsNullComparison =
        BinaryExpression.newBuilder()
            .setTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
            .setLeftOperand(FieldAccess.Builder.from(namesToValuesMapFieldDescriptor).build())
            .setOperator(BinaryOperator.EQUALS)
            .setRightOperand(NullLiteral.NULL)
            .build();
    Expression valuesCall = MethodCall.Builder.from(valuesMethodDescriptor).build();

    Expression createMapCall =
        MethodCall.Builder.from(createMapMethodDescriptor).setArguments(valuesCall).build();
    Statement assignMapCallToFieldStatement =
        BinaryExpression.Builder.asAssignmentTo(
                FieldAccess.Builder.from(namesToValuesMapFieldDescriptor).build())
            .setRightOperand(createMapCall)
            .build()
            .makeStatement();
    Statement ifStatement =
        new IfStatement(
            namesToValuesMapIsNullComparison, new Block(assignMapCallToFieldStatement), null);

    // Return statement
    Expression getMethodCall =
        MethodCall.Builder.from(getMethodDescriptor)
            .setArguments(
                nameParameter.getReference(),
                FieldAccess.Builder.from(namesToValuesMapFieldDescriptor).build())
            .build();
    Statement returnStatement =
        ReturnStatement.newBuilder()
            .setExpression(getMethodCall)
            .setTypeDescriptor(
                TypeDescriptors.getForArray(enumType.getDescriptor().getUnsafeTypeDescriptor(), 1))
            .build();

    return Method.newBuilder()
        .setMethodDescriptor(valueOfMethodDescriptor)
        .setParameters(nameParameter)
        .addStatements(ifStatement, returnStatement)
        .build();
  }

  /**
   * Creates the ast needed for values() which is of the form: <code>
   * static EnumType[] values() {
   *   return [
   *     EnumType.VALUE1,
   *     EnumType.VALUE2 ...
   *   ];
   * }
   * </code>
   */
  private Method createValuesMethod() {
    // Create method body.
    List<Expression> values =
        enumType
            .getEnumFields()
            .stream()
            .map(enumField -> Builder.from(enumField.getDescriptor()).build())
            .collect(Collectors.toList());

    TypeDescriptor arrayTypeDescriptor =
        TypeDescriptors.getForArray(enumType.getDescriptor().getUnsafeTypeDescriptor(), 1);

    return Method.newBuilder()
        .setMethodDescriptor(valuesMethodDescriptor)
        .addStatements(
            ReturnStatement.newBuilder()
                .setExpression(new ArrayLiteral(arrayTypeDescriptor, values))
                .setTypeDescriptor(arrayTypeDescriptor)
                .build())
        .build();
  }
}
